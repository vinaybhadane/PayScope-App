"""Project imported payment evidence and derive similarity from stored descriptors."""

from collections import defaultdict
import re
import unicodedata
import uuid

from neo4j import Query
from sqlalchemy import select

from app.graph.config import neo4j_settings as settings
from app.models.import_record import ImportRecord
from app.models.transaction import Transaction


PAYMENT_EVIDENCE = """
UNWIND $rows AS row
MATCH (a:Account {id: row.account_id, user_id: $user_id})
MATCH (t:Transaction {id: row.transaction_id, user_id: $user_id})
MERGE (m:Merchant {id: row.merchant_id})
ON CREATE SET m.canonical_name = row.descriptor, m.user_id = $user_id
SET m.sync_token = CASE WHEN m.user_id = $user_id THEN $token ELSE m.sync_token END
SET a.ownerId = $user_id, t.statement_import_id = row.import_id
MERGE (a)-[p:PAYS {user_id: $user_id}]->(m) SET p.sync_token = $token
MERGE (t)-[paid:PAID_TO {user_id: $user_id}]->(m) SET paid.sync_token = $token
MERGE (d:MerchantDescriptor {id: row.descriptor_id})
SET d.user_id = $user_id, d.rawDescriptor = row.descriptor, d.sync_token = $token
MERGE (m)-[has:HAS_PAYMENT_DESCRIPTOR {user_id: $user_id}]->(d) SET has.sync_token = $token
FOREACH (_ IN CASE WHEN row.mandate_key IS NULL THEN [] ELSE [1] END |
    MERGE (ma:Mandate {reference: row.mandate_key})
    SET ma.id = row.mandate_key, ma.user_id = $user_id, ma.label = row.mandate_label,
        ma.sync_token = $token
    MERGE (m)-[h:HAS_MANDATE {user_id: $user_id}]->(ma) SET h.sync_token = $token
    MERGE (t)-[observed:USES_MANDATE {user_id: $user_id}]->(ma) SET observed.sync_token = $token
)
"""

_LINK_ALERTS = """
MATCH (r:CommitmentInsight {user_id: $user_id, sync_token: $token})
WHERE r.status = 'open'
MATCH (r)-[:EVIDENCED_BY {user_id: $user_id, sync_token: $token}]->
      (t:Transaction {user_id: $user_id})-[:USES_MANDATE {user_id: $user_id, sync_token: $token}]->
      (ma:Mandate {user_id: $user_id})
SET r:RiskAlert, r.type = r.insight_type
MERGE (ma)-[f:FLAGGED_AS {user_id: $user_id}]->(r) SET f.sync_token = $token
"""

_LINK_SIMILAR = """
UNWIND $rows AS row
MATCH (a:MerchantDescriptor {id: row.left, user_id: $user_id}),
      (b:MerchantDescriptor {id: row.right, user_id: $user_id})
MERGE (a)-[r:SIMILAR_DESCRIPTOR {user_id: $user_id}]->(b)
SET r.score = row.score, r.sync_token = $token
"""

_NOISE_WORDS = frozenset({"com", "www", "inc", "ltd", "entm", "entertainment", "pvt", "limited",
                          "upi", "nach", "ecs", "pos", "card", "debit", "credit", "autopay",
                          "recurring", "monthly", "plan", "subscription", "payment", "ref", "india"})
_MANDATE_HEADERS = frozenset({"mandatereference", "mandateid", "mandateref", "umrn", "upiumn", "umn",
                              "emandatereference", "mandateexternalref"})


def _identity(workspace: str, namespace: str, value: str) -> str:
    return str(uuid.uuid5(uuid.UUID(workspace), f"{namespace}:{value}"))


def payment_rows_statement(user_id):
    return select(Transaction.id.label("transaction_id"), Transaction.account_id, Transaction.import_id,
                  Transaction.merchant_id, Transaction.raw_descriptor.label("descriptor"),
                  Transaction.raw_source_snapshot).join(ImportRecord, ImportRecord.id == Transaction.import_id).where(
        Transaction.user_id == user_id, ImportRecord.user_id == user_id,
        ImportRecord.source_label == "bank_statement", Transaction.account_id.is_not(None),
        Transaction.direction == "debit", Transaction.transaction_status == "successful", Transaction.amount > 0,
    )


def make_payment_row(source, workspace):
    row = dict(source)
    snapshot = row.pop("raw_source_snapshot") or {}
    descriptor = row["descriptor"]
    key = " ".join(unicodedata.normalize("NFKC", descriptor).casefold().split())
    row["descriptor_id"] = _identity(workspace, "descriptor", key)
    row["merchant_id"] = row["merchant_id"] or _identity(workspace, "unmatched-merchant", key)
    mandate = next((str(value).strip() for header, value in snapshot.items()
                    if not header.startswith("_") and re.sub(r"[^a-z0-9]", "", header.casefold()) in _MANDATE_HEADERS
                    and isinstance(value, (str, int)) and not isinstance(value, bool)
                    and str(value).strip().casefold() not in {"", "-", "--", "0", "n/a", "none", "null"}), None)
    row["mandate_key"] = _identity(workspace, "mandate", f"{row['account_id']}:{mandate.casefold()}") if mandate else None
    row["mandate_label"] = f"Mandate · {mandate[-4:]}" if mandate else None
    return row


def _tokens(descriptor):
    return frozenset(token for token in re.findall(r"[^\W_]+", unicodedata.normalize("NFKC", descriptor).casefold())
                     if len(token) >= 3 and not token.isdecimal() and token not in _NOISE_WORDS)


def _similar_pairs(descriptors):
    tokens_by_id = {row["id"]: _tokens(row["raw"]) for row in descriptors}
    matches = defaultdict(list)
    for descriptor_id, tokens in tokens_by_id.items():
        for token in tokens:
            matches[token].append(descriptor_id)
    for left, tokens in tokens_by_id.items():
        # Extremely common terms alone are not candidate evidence and would lead to a quadratic join.
        candidates = {right for token in tokens if len(matches[token]) <= 200 for right in matches[token] if right > left}
        for right in sorted(candidates):
            other = tokens_by_id[right]
            score = len(tokens & other) / len(tokens | other)
            if score >= 0.6:
                yield {"left": left, "right": right, "score": score}


def link_payment_evidence(session, workspace, token):
    """Use the stored descriptor values as the input to the similarity computation."""
    from itertools import islice

    params = {"user_id": workspace, "token": token}
    session.run(Query(_LINK_ALERTS, timeout=settings.SYNC_QUERY_TIMEOUT), **params).consume()
    descriptors = session.run(Query(
        "MATCH (d:MerchantDescriptor {user_id:$user_id, sync_token:$token}) RETURN d.id AS id, d.rawDescriptor AS raw",
        timeout=settings.SYNC_QUERY_TIMEOUT,
    ), **params).data()
    pairs = _similar_pairs(descriptors)
    while batch := list(islice(pairs, max(1, min(settings.SYNC_BATCH_SIZE, 2000)))):
        session.run(Query(_LINK_SIMILAR, timeout=settings.SYNC_QUERY_TIMEOUT), rows=batch, **params).consume()
