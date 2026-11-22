"""API endpoints for Phase 10 Neo4j Graph Relationship Intelligence."""

import logging
from collections import Counter
from typing import Optional
import uuid
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.graph.config import neo4j_settings
from app.graph.driver import check_neo4j_connection
from app.graph.exceptions import (
    Neo4jConnectionError,
    Neo4jDisabledError,
    Neo4jSyncError,
)
from app.core.read_cache import graph_cache
from app.graph.overview_service import get_live_overview, get_overview
from app.graph.query_service import GraphQueryService
from app.graph.schemas import (
    GraphElements,
    GraphHealthResponse,
    GraphSummaryResponse,
    GraphSyncResponse,
)
from app.graph.sync_service import GraphSyncService
from app.repositories.user_repository import resolve_workspace_user_id

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/graph", tags=["Graph Intelligence"])


def _resolve_user_id(db: Session, user_id: Optional[uuid.UUID] = None) -> uuid.UUID:
    return resolve_workspace_user_id(db, user_id)


@router.get("/health", response_model=GraphHealthResponse)
def get_graph_health(db: Session = Depends(get_db)) -> GraphHealthResponse:
    """Check connectivity and operational readiness of Neo4j graph engine."""
    enabled = neo4j_settings.ENABLED
    connected = False
    message = "Neo4j is disabled in configuration. Graph explorer operates in relational projection mode."
    node_count = 0
    edge_count = 0

    if enabled:
        try:
            connected = check_neo4j_connection()
            if connected:
                graph = get_overview(db, _resolve_user_id(db))
                node_count = len(graph.nodes)
                edge_count = len(graph.edges)
                message = f"Neo4j active and connected to {neo4j_settings.DATABASE} ({node_count} nodes, {edge_count} relationships)."
            else:
                message = "Neo4j is enabled but unreachable."
        except Neo4jSyncError as e:
            message = str(e)
        except Exception as e:
            logger.warning("Graph health check failed: %s", e)
            message = "Neo4j is temporarily unavailable."

    return GraphHealthResponse(
        status="healthy" if (not enabled or connected) else "degraded",
        enabled=enabled,
        connected=connected,
        database=neo4j_settings.DATABASE if enabled else None,
        node_count=node_count,
        edge_count=edge_count,
        message=message,
    )


@router.get("/summary", response_model=GraphSummaryResponse)
def get_graph_summary(db: Session = Depends(get_db)) -> GraphSummaryResponse:
    """Count nodes and edges in the current workspace's graph snapshot."""
    user_id = _resolve_user_id(db)
    try:
        graph, _ = graph_cache.get_or_load(f"overview:{user_id}:50", lambda: get_overview(db, user_id))
    except (Neo4jConnectionError, Neo4jDisabledError, Neo4jSyncError) as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return GraphSummaryResponse(
        total_nodes=len(graph.nodes),
        total_relationships=len(graph.edges),
        counts_by_label=dict(Counter(node.data.node_type for node in graph.nodes)),
        counts_by_relationship=dict(Counter(edge.data.relationship_type for edge in graph.edges)),
        last_sync_timestamp=graph.last_sync_at,
    )


@router.post("/sync", response_model=GraphSyncResponse)
def sync_graph_projection(
    user_id: Optional[uuid.UUID] = Query(None, description="Workspace user UUID"),
    db: Session = Depends(get_db),
) -> GraphSyncResponse:
    """Idempotently synchronizes the PostgreSQL source-of-truth records into Neo4j."""
    target_user_id = _resolve_user_id(db, user_id)

    try:
        sync_svc = GraphSyncService(db)
        return sync_svc.sync_user_graph(target_user_id)
    except (Neo4jConnectionError, Neo4jDisabledError, Neo4jSyncError) as e:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Graph synchronization failed. Please retry.",
        )


@router.get("", response_model=GraphElements)
def get_live_relationships(
    user_id: Optional[uuid.UUID] = Query(None, description="Workspace user UUID"),
    limit: int = Query(50, ge=1, le=100),
    db: Session = Depends(get_db),
) -> GraphElements:
    target = _resolve_user_id(db, user_id)
    try:
        graph, _ = graph_cache.get_or_load(f"live:{target}:{limit}", lambda: get_live_overview(db, target, limit))
        return graph
    except (Neo4jDisabledError, Neo4jConnectionError, Neo4jSyncError) as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@router.get("/explorer", response_model=GraphElements)
def get_graph_explorer_network(
    user_id: Optional[uuid.UUID] = Query(None, description="Workspace user UUID"),
    limit: int = Query(50, ge=1, le=100),
    db: Session = Depends(get_db),
) -> GraphElements:
    """Retrieve the multi-hop relationship graph elements for Cytoscape.js rendering."""
    target = _resolve_user_id(db, user_id)
    try:
        result, _ = graph_cache.get_or_load(f"overview:{target}:{limit}", lambda: get_overview(db, target, limit))
        return result
    except Exception as exc:
        logger.warning("Failed to query graph payment overview: %s", exc)
        raise HTTPException(
            status_code=503, detail="Relationship data is temporarily unavailable."
        )


@router.get("/merchants/{id}", response_model=GraphElements)
def get_merchant_graph(
    id: uuid.UUID,
    db: Session = Depends(get_db),
) -> GraphElements:
    """Retrieve merchant descriptor aliases and transaction links for graph exploration."""
    try:
        graph = get_overview(db, _resolve_user_id(db))
        selected = {str(id)}
        for edge in graph.edges:
            if edge.data.target == str(id) or edge.data.source == str(id):
                selected.update([edge.data.source, edge.data.target])
        return GraphElements(nodes=[n for n in graph.nodes if n.data.id in selected],
                             edges=[e for e in graph.edges if e.data.source in selected and e.data.target in selected],
                             source=graph.source, last_sync_at=graph.last_sync_at)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="The operation could not be completed. Please retry.")
    except Exception as exc:
        logger.warning("Failed to query merchant graph: %s", exc)
        raise HTTPException(status_code=503, detail="Merchant relationships are temporarily unavailable.") from exc


@router.get("/commitments/{id}", response_model=GraphElements)
def get_commitment_graph(
    id: uuid.UUID,
    user_id: Optional[uuid.UUID] = Query(None, description="Workspace user UUID"),
    db: Session = Depends(get_db),
) -> GraphElements:
    """Retrieve commitment evidence trail and anomaly insights in Cytoscape graph format."""
    target_user_id = _resolve_user_id(db, user_id)
    svc = GraphQueryService(db)
    try:
        return svc.get_commitment_evidence(target_user_id, id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="The operation could not be completed. Please retry.")
    except Exception as exc:
        logger.warning("Failed to query commitment graph: %s", exc)
        raise HTTPException(status_code=503, detail="Commitment relationships are temporarily unavailable.") from exc
