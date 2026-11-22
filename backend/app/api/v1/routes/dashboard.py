"""Dashboard summary aggregation API endpoint."""

import logging
from decimal import Decimal
from typing import Dict, List, Optional
import uuid
from fastapi import APIRouter, Depends, Query, Response
from pydantic import BaseModel
from sqlalchemy import func, select
from sqlalchemy.orm import Session
from datetime import datetime, timezone
from time import perf_counter
from app.core.read_cache import dashboard_cache
from app.analytics.financial_impact.commitment_costs import get_user_commitment_costs
from app.analytics.financial_impact.historical_spend import get_monthly_payment_activity
from app.analytics.financial_impact.schemas import MonthlyTrendPoint

from app.analytics.financial_impact import (
    get_user_savings_summary,
    get_user_upcoming_expenditure,
)
from app.db.session import get_db
from app.models.cancellation_case import CancellationCase
from app.models.commitment import Commitment
from app.models.commitment_insight import CommitmentInsight
from app.models.manual_review_item import ManualReviewItem
from app.repositories.user_repository import UserRepository, resolve_workspace_user_id
from app.analytics.autodebit_keyword_service import AutoDebitKeywordService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


class DashboardKPIs(BaseModel):
    active_commitments_count: int
    monthly_recurring_spend: Decimal
    annualized_recurring_spend: Decimal
    open_insights_count: int
    potential_annual_savings: Decimal
    cancellations_attention_count: int


class DashboardTopCommitment(BaseModel):
    id: uuid.UUID
    display_name: str
    merchant_name: Optional[str] = None
    merchant_id: Optional[uuid.UUID] = None
    merchant_domain: Optional[str] = None
    merchant_logo_url: Optional[str] = None
    logo_source: Optional[str] = None
    category: str
    cadence: str
    monthly_cost: Decimal
    status: str
    confidence_band: str
    payment_rail: Optional[str] = None
    verification_status: Optional[str] = "inferred"


class DashboardRecentInsight(BaseModel):
    id: uuid.UUID
    commitment_id: uuid.UUID
    commitment_name: str
    insight_type: str
    severity: str
    title: str
    summary: str


class DashboardFinancialOverview(BaseModel):
    monthly_recurring_spend: Decimal
    annualized_recurring_spend: Decimal
    upcoming_month_expected: Decimal
    candidate_annual_cost: Decimal
    historical_months: List[str]
    historical_amounts: List[Decimal]
    monthly_activity: List[MonthlyTrendPoint] = []


class DashboardSummaryResponse(BaseModel):
    data_status: str = "ok"
    generated_at: Optional[datetime] = None
    kpis: DashboardKPIs
    financial_overview: DashboardFinancialOverview
    top_commitments: List[DashboardTopCommitment]
    category_distribution: Dict[str, Decimal]
    recent_insights: List[DashboardRecentInsight]
    insight_type_counts: Dict[str, int]
    cancellation_summary: Dict[str, int]
    merchant_review_pending: int
    statement_review_count: int = 0
    canonical_merchants_count: int
    autodebit_rail_distribution: Optional[Dict[str, int]] = None
    ai_audit_score: Optional[int] = None
    ai_audit_status: Optional[str] = "not_audited"


def _resolve_user_id(db: Session, user_id: Optional[uuid.UUID] = None) -> uuid.UUID:
    return resolve_workspace_user_id(db, user_id)


def _get_fallback_dashboard_summary() -> DashboardSummaryResponse:
    return DashboardSummaryResponse(
        data_status="unavailable",
        kpis=DashboardKPIs(active_commitments_count=0, monthly_recurring_spend=0,
                          annualized_recurring_spend=0, open_insights_count=0,
                          potential_annual_savings=0, cancellations_attention_count=0),
        financial_overview=DashboardFinancialOverview(monthly_recurring_spend=0,
            annualized_recurring_spend=0, upcoming_month_expected=0, candidate_annual_cost=0,
            historical_months=[], historical_amounts=[]),
        top_commitments=[], category_distribution={}, recent_insights=[], insight_type_counts={},
        cancellation_summary={}, merchant_review_pending=0, canonical_merchants_count=0,
        autodebit_rail_distribution={}, ai_audit_score=None, ai_audit_status="unavailable",
    )


def _build_dashboard_summary(db: Session, user_id: Optional[uuid.UUID]) -> DashboardSummaryResponse:
    target = _resolve_user_id(db, user_id)
    costs = get_user_commitment_costs(db, target)
    confirmed = [item for item in costs if item.is_confirmed]
    monthly = sum((item.normalized_monthly_cost for item in confirmed), Decimal("0.00"))
    annual = sum((item.annualized_cost for item in confirmed), Decimal("0.00"))
    candidate = sum((item.annualized_cost for item in costs if item.is_candidate), Decimal("0.00"))
    savings = get_user_savings_summary(db, target, cost_items=costs)
    upcoming = get_user_upcoming_expenditure(db, target, cost_items=costs)
    categories = {}
    for item in confirmed:
        category = (item.category or item.commitment_type).capitalize()
        categories[category] = categories.get(category, Decimal("0.00")) + item.normalized_monthly_cost

    # One database round trip for bounded counters, all explicitly workspace scoped.
    insight_types = ["price_increase", "trial_conversion", "potentially_unused", "post_cancellation_charge", "unusual_amount"]
    cancellation_states = {"in_progress": "IN_PROGRESS", "user_reported": "USER_REPORTED_CANCELLED",
                           "verified_cancelled": "VERIFIED_CANCELLED"}
    def count(model, *conditions):
        return select(func.count(model.id)).where(model.user_id == target, *conditions).scalar_subquery()
    fields = {"open": count(CommitmentInsight, CommitmentInsight.status == "open"),
              "total": count(CancellationCase),
              "statement_review": count(ManualReviewItem, ManualReviewItem.status.in_(["pending", "open"]),
                                        ManualReviewItem.reason_code.in_(["invalid_statement_row", "security_quarantine"])),
              "pending": count(ManualReviewItem, ManualReviewItem.status.in_(["pending", "open"]))}
    for kind in insight_types:
        fields[kind] = count(CommitmentInsight, CommitmentInsight.status == "open", CommitmentInsight.insight_type == kind)
    for name, state in cancellation_states.items():
        fields[name] = count(CancellationCase, CancellationCase.status == state)
    values = db.execute(select(*(value.label(name) for name, value in fields.items()))).mappings().one()
    cancellation = {name: int(values[name]) for name in ["total", *cancellation_states]}
    cancellation["needs_attention"] = cancellation["in_progress"] + cancellation["user_reported"]
    recent = db.execute(select(CommitmentInsight, Commitment.display_name)
        .join(Commitment, CommitmentInsight.commitment_id == Commitment.id)
        .where(CommitmentInsight.user_id == target, Commitment.user_id == target, CommitmentInsight.status == "open")
        .order_by(CommitmentInsight.detected_at.desc()).limit(5)).all()
    history = get_monthly_payment_activity(db, target, limit=12)

    # Calculate auto-debit rail distribution from verified commitments
    autodebit_service = AutoDebitKeywordService.get_instance()
    rail_counts: Dict[str, int] = {}
    top_commitment_models = []
    
    for item in sorted(confirmed, key=lambda item: item.normalized_monthly_cost, reverse=True)[:6]:
        analysis = autodebit_service.analyze_descriptor(item.display_name)
        rail = analysis.payment_rail if analysis.is_autodebit_candidate else "Unknown"
        rail_counts[rail] = rail_counts.get(rail, 0) + 1
        top_commitment_models.append(
            DashboardTopCommitment(
                id=item.commitment_id,
                display_name=item.display_name,
                merchant_name=item.merchant_name,
                merchant_id=item.merchant_id,
                merchant_domain=item.merchant_domain,
                merchant_logo_url=item.merchant_logo_url,
                logo_source=item.logo_source,
                category=(item.category or item.commitment_type).capitalize(),
                cadence=item.cadence,
                monthly_cost=item.normalized_monthly_cost,
                status=item.status,
                confidence_band=item.confidence_band,
                payment_rail=rail,
                verification_status="inferred",
            )
        )

    return DashboardSummaryResponse(
        generated_at=datetime.now(timezone.utc),
        kpis=DashboardKPIs(active_commitments_count=len(confirmed), monthly_recurring_spend=monthly,
            annualized_recurring_spend=annual, open_insights_count=values["open"],
            potential_annual_savings=savings.total_potential_annual_savings,
            cancellations_attention_count=cancellation["needs_attention"]),
        financial_overview=DashboardFinancialOverview(monthly_recurring_spend=monthly,
            annualized_recurring_spend=annual, upcoming_month_expected=upcoming.total_upcoming_30d,
            candidate_annual_cost=candidate, historical_months=[row.month_label for row in history],
            historical_amounts=[row.recurring_debit for row in history], monthly_activity=history),
        top_commitments=top_commitment_models,
        category_distribution=categories,
        recent_insights=[DashboardRecentInsight(id=item.id, commitment_id=item.commitment_id,
            commitment_name=name, insight_type=item.insight_type, severity=item.severity,
            title=item.title, summary=item.summary) for item, name in recent],
        insight_type_counts={kind: int(values[kind]) for kind in insight_types},
        cancellation_summary=cancellation, merchant_review_pending=values["pending"],
        statement_review_count=values["statement_review"],
        canonical_merchants_count=len({item.merchant_id for item in costs if item.merchant_id}),
        autodebit_rail_distribution=rail_counts,
        ai_audit_score=None,
        ai_audit_status="not_audited",
    )


@router.get("/summary", response_model=DashboardSummaryResponse)
def get_dashboard_summary(
    response: Response,
    user_id: Optional[uuid.UUID] = Query(None, description="Workspace user UUID"),
    db: Session = Depends(get_db),
) -> DashboardSummaryResponse:
    started = perf_counter()
    try:
        result, cached = dashboard_cache.get_or_load(
            str(_resolve_user_id(db, user_id)), lambda: _build_dashboard_summary(db, user_id)
        )
        response.headers["X-Cache"] = "HIT" if cached else "MISS"
        response.headers["Cache-Control"] = "no-store"
        return result
    except Exception:
        db.rollback()
        logger.exception("Dashboard data could not be loaded")
        response.status_code = 503
        return _get_fallback_dashboard_summary()
    finally:
        response.headers["Server-Timing"] = (
            f"dashboard;dur={(perf_counter() - started) * 1000:.1f}"
        )
