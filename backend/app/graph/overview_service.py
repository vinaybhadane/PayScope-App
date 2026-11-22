"""A bounded, connected graph snapshot; Neo4j reads never block dashboard metrics."""

from datetime import datetime
import logging
from threading import Lock
from time import monotonic
import uuid
from neo4j import Query, READ_ACCESS
from neo4j.exceptions import Neo4jError as DriverError
from sqlalchemy import select, func
from sqlalchemy.orm import joinedload, Session
from app.graph.config import neo4j_settings as settings
from app.graph.driver import get_neo4j_driver
from app.graph.exceptions import Neo4jConnectionError, Neo4jDisabledError, Neo4jSyncError
from app.graph.schemas import (
    GraphElements,
    GraphNode,
    GraphNodeData,
    GraphEdge,
    GraphEdgeData,
)
from app.models.account import Account
from app.models.commitment import Commitment
from app.models.commitment_insight import CommitmentInsight
from app.models.commitment_transaction import CommitmentTransaction
from app.models.transaction import Transaction
from app.models.import_record import ImportRecord

_retry_after = 0.0
_circuit_lock = Lock()
logger = logging.getLogger(__name__)


def reset_neo4j_circuit():
    global _retry_after
    with _circuit_lock:
        _retry_after = 0


OVERVIEW_QUERY = """
MATCH (u:User {id:$user_id, user_id:$user_id})
CALL (u) {
    MATCH (u)-[:OWNS_ACCOUNT {user_id:$user_id}]->(a:Account {ownerId:$user_id, user_id:$user_id})
    MATCH (a)-[:PAYS {user_id:$user_id}]->(m:Merchant)
    WITH a, m ORDER BY a.id, m.id LIMIT $limit
    RETURN collect(DISTINCT a) AS accounts, collect(DISTINCT m) AS merchants
}
CALL (accounts, merchants) {
    UNWIND accounts AS a
    MATCH (a)-[:HAS_TRANSACTION {user_id:$user_id}]->(t:Transaction {user_id:$user_id})
           -[:PAID_TO {user_id:$user_id}]->(m:Merchant)
    WHERE t.statement_import_id IS NOT NULL AND m IN merchants
    WITH DISTINCT t ORDER BY t.transaction_date DESC, t.id LIMIT $limit
    RETURN collect(t) AS transactions
}
CALL (transactions) {
    UNWIND transactions AS t
    MATCH (c:Commitment {user_id:$user_id})-[:SUPPORTED_BY {user_id:$user_id}]->(t)
    WITH DISTINCT c ORDER BY c.id LIMIT $limit
    RETURN collect(c) AS commitments
}
CALL (merchants) {
    UNWIND merchants AS m
    MATCH (m)-[:HAS_PAYMENT_DESCRIPTOR {user_id:$user_id}]->(d:MerchantDescriptor {user_id:$user_id})
    WITH DISTINCT d ORDER BY d.id LIMIT $limit
    RETURN collect(d) AS descriptors
}
CALL (merchants) {
    UNWIND merchants AS m
    MATCH (m)-[:HAS_MANDATE {user_id:$user_id}]->(ma:Mandate {user_id:$user_id})
    WITH DISTINCT ma ORDER BY ma.id LIMIT $limit
    RETURN collect(ma) AS mandates
}
CALL (mandates, commitments) {
    MATCH (i:CommitmentInsight {user_id:$user_id})
    WHERE EXISTS { MATCH (ma:Mandate)-[:FLAGGED_AS {user_id:$user_id}]->(i) WHERE ma IN mandates }
       OR EXISTS { MATCH (c:Commitment)-[:HAS_INSIGHT {user_id:$user_id}]->(i) WHERE c IN commitments }
    WITH i ORDER BY i.detected_at DESC, i.id LIMIT $limit
    RETURN collect(i) AS insights
}
WITH u, size(accounts) AS account_count, [u]+accounts+commitments+merchants+insights+transactions+descriptors+mandates AS selected
UNWIND selected AS n
OPTIONAL MATCH (n)-[r {user_id:$user_id}]->(other) WHERE other IN selected
WITH u, account_count, collect(DISTINCT n) AS nodes, collect(DISTINCT r) AS relationships
RETURN CASE WHEN account_count = 0 THEN [] ELSE nodes END AS nodes,
       relationships, u.last_sync_at AS last_sync_at, u.sync_status AS sync_status
"""


def _node(node_id, kind, name, **props):
    return GraphNode(
        data=GraphNodeData(
            id=str(node_id), label=name, name=name, node_type=kind, **props
        )
    )


def _edge(source, target, kind, **metadata):
    return GraphEdge(
        data=GraphEdgeData(
            id=f"{source}:{kind}:{target}",
            source=str(source),
            target=str(target),
            relationship_type=kind,
            label=kind.replace("_", " ").lower(),
            metadata=metadata,
        )
    )


def serialize_neo4j(record, limit):
    nodes = []
    for n in record["nodes"]:
        kind = "RiskAlert" if "RiskAlert" in n.labels else next(iter(n.labels))
        name = (
            n.get("display_name")
            or n.get("canonical_name")
            or n.get("label")
            or n.get("title")
            or n.get("raw_descriptor")
            or n.get("rawDescriptor")
            or kind
        )
        nodes.append(
            _node(
                n["id"],
                kind,
                name,
                amount=n.get("representative_amount", n.get("amount")),
                status=n.get("status"),
                category=n.get("category", n.get("insight_type")),
                cadence=n.get("cadence"),
                confidence_band=n.get("confidence_band"),
                metadata={
                    k: n.get(k)
                    for k in (
                        "currency",
                        "payment_channel",
                        "transaction_date",
                        "severity",
                        "masked_reference",
                    )
                    if n.get(k) is not None
                },
            )
        )
    ids = {n.data.id for n in nodes}
    edges = [
        _edge(r.start_node["id"], r.end_node["id"], r.type, **({"score": r["score"]} if "score" in r else {}))
        for r in record["relationships"]
        if r.start_node["id"] in ids and r.end_node["id"] in ids
    ]
    by_id = {n.data.id: n for n in nodes}
    for e in edges:
        if e.data.relationship_type == "ASSOCIATED_WITH":
            by_id[e.data.source].data.category = by_id[e.data.target].data.category
    return GraphElements(
        nodes=nodes,
        edges=edges,
        source="neo4j",
        last_sync_at=record.get("last_sync_at"),
        truncated=any(sum(n.data.node_type == kind for n in nodes) >= limit for kind in (
            "Merchant", "Transaction", "MerchantDescriptor", "Mandate", "Commitment",
        )),
        message="Live Neo4j projection of this workspace's statement payments.",
    )


def get_live_overview(db: Session, user_id: uuid.UUID, limit=50):
    global _retry_after
    limit = max(1, min(limit, 100))
    if not settings.ENABLED:
        raise Neo4jDisabledError("Live Neo4j is disabled for this environment.")
    if monotonic() < _retry_after:
        raise Neo4jConnectionError("Live Neo4j is temporarily unavailable. Please retry shortly.")
    try:
        driver = get_neo4j_driver()
        with driver.session(database=settings.DATABASE, default_access_mode=READ_ACCESS, fetch_size=200) as session:
            record = session.run(Query(OVERVIEW_QUERY, timeout=settings.QUERY_TIMEOUT),
                                 user_id=str(user_id), limit=limit).single()
    except Exception as exc:
        with _circuit_lock:
            _retry_after = monotonic() + 30
        logger.warning("Live graph read failed (%s).", exc.code if isinstance(exc, DriverError) else type(exc).__name__)
        raise Neo4jConnectionError("Live Neo4j is temporarily unavailable. Please retry shortly.") from exc

    latest_import = db.scalar(select(func.max(ImportRecord.finished_at)).where(
        ImportRecord.user_id == user_id,
        ImportRecord.status.in_(["completed", "partially_completed"])))
    if not record or record.get("sync_status") != "ready":
        return relational_overview(db, user_id, limit)
    graph = serialize_neo4j(record, limit)
    if not graph.nodes:
        return relational_overview(db, user_id, limit)
    if latest_import and (not graph.last_sync_at or graph.last_sync_at < latest_import):
        # When graph is behind latest import, still return graph with note or sync status
        pass
    return graph


def get_overview(db: Session, user_id: uuid.UUID, limit=50):
    # The legacy SQL explorer remains explicitly labelled for installations without Neo4j.
    return get_live_overview(db, user_id, limit) if settings.ENABLED else relational_overview(db, user_id, limit)


def relational_overview(db, user_id, limit=50):
    """Same evidence model from SQL, with fixed query count and no lazy collections."""
    nodes = {str(user_id): _node(user_id, "User", "Your workspace")}
    edges = {}

    def edge(source, target, kind):
        e = _edge(source, target, kind)
        edges[e.data.id] = e

    commitments = db.scalars(
        select(Commitment)
        .options(joinedload(Commitment.merchant))
        .where(Commitment.user_id == user_id)
        .order_by(Commitment.id)
        .limit(limit + 1)
    ).all()
    truncated = len(commitments) > limit
    commitments = commitments[:limit]
    ids = [c.id for c in commitments]
    accounts = db.scalars(
        select(Account).where(Account.user_id == user_id).order_by(Account.id).limit(20)
    ).all()
    for a in accounts:
        nodes[str(a.id)] = _node(
            a.id,
            "Account",
            a.label,
            metadata={"currency": a.currency, "masked_reference": a.masked_reference},
        )
        edge(user_id, a.id, "OWNS_ACCOUNT")
    for c in commitments:
        nodes[str(c.id)] = _node(
            c.id,
            "Commitment",
            c.display_name,
            amount=float(c.representative_amount or 0),
            status=c.status,
            cadence=c.cadence,
            confidence_band=c.confidence_band,
            category=c.merchant.category if c.merchant else c.commitment_type,
            metadata={"currency": c.currency},
        )
        edge(user_id, c.id, "HAS_COMMITMENT")
        if c.merchant:
            m = c.merchant
            nodes[str(m.id)] = _node(
                m.id, "Merchant", m.canonical_name, category=m.category
            )
            edge(c.id, m.id, "ASSOCIATED_WITH")
    if ids:
        insights = db.scalars(
            select(CommitmentInsight)
            .where(
                CommitmentInsight.user_id == user_id,
                CommitmentInsight.commitment_id.in_(ids),
            )
            .order_by(CommitmentInsight.detected_at.desc())
            .limit(50)
        ).all()
        for i in insights:
            nodes[str(i.id)] = _node(
                i.id,
                "CommitmentInsight",
                i.title,
                status=i.status,
                category=i.insight_type,
                metadata={"severity": i.severity},
            )
            edge(i.commitment_id, i.id, "HAS_INSIGHT")
        txs = db.execute(
            select(
                Transaction.id,
                Transaction.account_id,
                Transaction.raw_descriptor,
                Transaction.amount,
                Transaction.currency,
                Transaction.transaction_date,
                Transaction.payment_channel,
                Transaction.merchant_id,
                CommitmentTransaction.commitment_id,
            )
            .join(
                CommitmentTransaction,
                Transaction.id == CommitmentTransaction.transaction_id,
            )
            .where(
                Transaction.user_id == user_id,
                CommitmentTransaction.commitment_id.in_(ids),
            )
            .order_by(Transaction.transaction_date.desc(), Transaction.id)
            .limit(limit)
        ).all()
        for t in txs:
            nodes[str(t.id)] = _node(
                t.id,
                "Transaction",
                t.raw_descriptor,
                amount=float(t.amount),
                metadata={
                    "transaction_date": t.transaction_date.isoformat(),
                    "payment_channel": t.payment_channel,
                    "currency": t.currency,
                },
            )
            edge(t.commitment_id, t.id, "SUPPORTED_BY")
            if str(t.account_id) in nodes:
                edge(t.account_id, t.id, "HAS_TRANSACTION")
            if str(t.merchant_id) in nodes:
                edge(t.id, t.merchant_id, "PAID_TO")
    return GraphElements(
        nodes=list(nodes.values()),
        edges=list(edges.values()),
        source="relational",
        truncated=truncated,
        message="Showing source records from PostgreSQL. Sync the projection to enable Neo4j reads.",
    )
