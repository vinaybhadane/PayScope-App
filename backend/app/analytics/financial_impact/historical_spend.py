"""Historical expenditure analysis across selectable time horizons and monthly spend timeline."""
import uuid
from datetime import datetime, timezone, timedelta
from decimal import Decimal
from typing import Dict, List, Optional, Set
from sqlalchemy import case, exists, func, select
from sqlalchemy.orm import Session

from app.analytics.financial_impact.config import HISTORICAL_PERIODS
from app.analytics.financial_impact.normalization import round_currency
from app.analytics.financial_impact.schemas import (
    HistoricalPeriodSpend,
    HistoricalSpendResponse,
    MonthlyTrendPoint,
)
from app.models.commitment_transaction import CommitmentTransaction
from app.models.commitment import Commitment
from app.models.transaction import Transaction


def get_monthly_payment_activity(db: Session, user_id: uuid.UUID, *, as_of=None, limit=None):
    month = func.date_trunc("month", func.timezone("UTC", Transaction.transaction_date))
    debit = func.lower(Transaction.direction) == "debit"
    credit = func.lower(Transaction.direction) == "credit"
    recurring = exists(select(1).select_from(CommitmentTransaction).join(Commitment).where(
        CommitmentTransaction.transaction_id == Transaction.id, Commitment.user_id == user_id,
        Commitment.cadence.not_in(["unknown", "irregular"]), Commitment.status != "marked_not_recurring",
    ))
    query = select(
        month.label("month"),
        func.sum(case((debit, Transaction.amount), else_=0)).label("gross"),
        func.sum(case((credit, Transaction.amount), else_=0)).label("credits"),
        func.sum(case((debit & recurring, Transaction.amount), else_=0)).label("recurring"),
    ).where(Transaction.user_id == user_id, Transaction.currency == "INR",
            func.lower(Transaction.transaction_status) == "successful", debit | credit)
    if as_of is not None:
        query = query.where(Transaction.transaction_date <= as_of)
    query = query.group_by(month).order_by(month.desc())
    if limit is not None:
        query = query.limit(limit)
    return [MonthlyTrendPoint(
        month_key=row.month.strftime("%Y-%m"), month_label=row.month.strftime("%b %Y"),
        gross_debit=round_currency(row.gross), credits=round_currency(row.credits),
        net_spend=round_currency(row.gross - row.credits), recurring_debit=round_currency(row.recurring),
    ) for row in reversed(db.execute(query).all())]


def get_user_historical_spend(
    db: Session,
    user_id: uuid.UUID,
    as_of: Optional[datetime] = None,
) -> HistoricalSpendResponse:
    """Calculate historical expenditure across 30d, 90d, 180d, 365d, and all history.

    Safeguards:
    - Filters ONLY successful debits for gross debit spend.
    - Explicitly excludes failed, reversed, and pending transactions.
    - Excludes duplicate transaction records.
    - Tracks credits and net spend separately.
    """
    now = as_of or datetime.now(timezone.utc)

    # Fetch all transactions for this user
    all_txs_stmt = (
        select(Transaction)
        .where(Transaction.user_id == user_id, Transaction.currency == 'INR')
        .order_by(Transaction.transaction_date.desc())
    )
    all_txs = db.scalars(all_txs_stmt).all()

    # Pre-fetch transaction IDs that belong to confirmed recurring commitments
    recurring_tx_ids_stmt = (
        select(CommitmentTransaction.transaction_id)
        .join(Transaction, CommitmentTransaction.transaction_id == Transaction.id)
        .join(Commitment, CommitmentTransaction.commitment_id == Commitment.id)
        .where(Transaction.user_id == user_id, Commitment.user_id == user_id,
               Commitment.cadence.not_in(["unknown", "irregular"]), Commitment.status != "marked_not_recurring")
    )
    recurring_tx_ids: Set[uuid.UUID] = set(db.scalars(recurring_tx_ids_stmt).all())

    # De-duplicate transactions if needed
    seen_external_ids: Set[str] = set()
    deduped_txs: List[Transaction] = []
    excluded_duplicates_count = 0

    for tx in all_txs:
        ext_id = tx.external_synthetic_id
        if ext_id:
            if ext_id in seen_external_ids:
                excluded_duplicates_count += 1
                continue
            seen_external_ids.add(ext_id)
        deduped_txs.append(tx)
    unique_ids = {tx.id for tx in deduped_txs}

    # Helper for period window filtering
    def _evaluate_period(period_key: str, days: Optional[int], label: str) -> HistoricalPeriodSpend:
        start_threshold = now - timedelta(days=days) if days is not None else None
        
        gross_debit = Decimal("0.00")
        credits = Decimal("0.00")
        valid_count = 0
        failed_count = 0
        period_dups = 0

        for tx in all_txs:
            # Check date window
            tx_dt = tx.transaction_date
            if tx_dt.tzinfo is None:
                tx_dt = tx_dt.replace(tzinfo=timezone.utc)
            if tx_dt > now or (start_threshold and tx_dt < start_threshold):
                continue

            # Check status
            st = (tx.transaction_status or "").lower()
            dir_ = (tx.direction or "").lower()

            if st != "successful":
                failed_count += 1
                continue

            # Duplicate check
            ext_id = tx.external_synthetic_id
            # If it was excluded in deduped list
            if tx.id not in unique_ids:
                period_dups += 1
                continue

            amt = tx.amount or Decimal("0.00")
            if dir_ == "debit":
                gross_debit += amt
                valid_count += 1
            elif dir_ == "credit":
                credits += amt

        net_spend = gross_debit - credits

        return HistoricalPeriodSpend(
            period=period_key,
            period_label=label,
            start_date=start_threshold,
            end_date=now,
            gross_debit_amount=round_currency(gross_debit),
            total_credits=round_currency(credits),
            net_spend=round_currency(net_spend),
            transaction_count=valid_count,
            excluded_duplicates_count=period_dups,
            excluded_failed_count=failed_count,
        )

    period_specs = [
        ("30d", 30, "Last 30 Days"),
        ("90d", 90, "Last 90 Days"),
        ("180d", 180, "Last 6 Months"),
        ("365d", 365, "Last 12 Months"),
        ("all", None, "All Time History"),
    ]

    periods_summary = [
        _evaluate_period(key, days, label)
        for key, days, label in period_specs
    ]

    return HistoricalSpendResponse(
        currency="INR",
        periods=periods_summary,
        monthly_timeline=get_monthly_payment_activity(db, user_id, as_of=now),
    )

