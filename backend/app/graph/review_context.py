"""Tenant-scoped Neo4j facts to attach to transaction evidence for a model review."""

from neo4j import Query, READ_ACCESS

from app.graph.config import neo4j_settings as settings
from app.graph.driver import get_neo4j_driver
from app.graph.exceptions import Neo4jConnectionError, Neo4jDisabledError, Neo4jSyncError


_CONTEXT_QUERY = """
MATCH (:User {id:$user_id, user_id:$user_id, sync_status:'ready'})
UNWIND $transaction_ids AS id
MATCH (t:Transaction {id:id, user_id:$user_id})-[:PAID_TO {user_id:$user_id}]->(m:Merchant)
WHERE t.statement_import_id IS NOT NULL
WITH m, collect(DISTINCT t.id) AS transaction_ids
CALL (m) {
    MATCH (a:Account {ownerId:$user_id, user_id:$user_id})-[:PAYS {user_id:$user_id}]->(m)
    RETURN count(DISTINCT a) AS account_count
}
CALL (m) {
    MATCH (payment:Transaction {user_id:$user_id})-[:PAID_TO {user_id:$user_id}]->(m)
    WHERE payment.statement_import_id IS NOT NULL
    RETURN count(DISTINCT payment) AS transaction_count
}
CALL (m) {
    OPTIONAL MATCH (m)-[:HAS_MANDATE {user_id:$user_id}]->(ma:Mandate {user_id:$user_id})
    OPTIONAL MATCH (ma)-[:FLAGGED_AS {user_id:$user_id}]->(r:RiskAlert {user_id:$user_id, status:'open'})
    RETURN count(DISTINCT ma) AS mandate_count, collect(DISTINCT r.type) AS existing_flags
}
RETURN transaction_ids, account_count, transaction_count, mandate_count, existing_flags
"""

_ALERT_TYPES = {"price_increase", "trial_conversion", "potentially_unused", "post_cancellation_charge", "unusual_amount"}


def payment_graph_context(user_id, transaction_ids):
    if not settings.ENABLED:
        raise Neo4jDisabledError("Live Neo4j must be enabled for a grounded review.")
    try:
        driver = get_neo4j_driver()
        with driver.session(database=settings.DATABASE, default_access_mode=READ_ACCESS) as session:
            rows = session.run(Query(_CONTEXT_QUERY, timeout=settings.QUERY_TIMEOUT),
                               user_id=str(user_id), transaction_ids=transaction_ids).data()
    except Exception as exc:
        raise Neo4jConnectionError("Live Neo4j is unavailable. No review was sent to Gemini.") from exc
    if not rows:
        raise Neo4jSyncError("Sync Neo4j to make the selected payments available for review.")
    return [{
        "transactionIds": sorted(row["transaction_ids"]),
        "accountCount": row["account_count"],
        "transactionCount": row["transaction_count"],
        "mandateCount": row["mandate_count"],
        "existingFlags": sorted({flag for flag in row["existing_flags"] if flag in _ALERT_TYPES}),
    } for row in rows]
