"""Upcoming expected recurring commitment expenditures."""

import uuid
from datetime import date, timedelta
from dateutil.relativedelta import relativedelta
from zoneinfo import ZoneInfo
from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from sqlalchemy.orm import Session

from app.analytics.financial_impact.commitment_costs import get_user_commitment_costs
from app.analytics.financial_impact.normalization import round_currency
from app.analytics.financial_impact.schemas import (
    UpcomingExpenseItem,
    UpcomingExpenditureResponse,
    CommitmentCostItem,
)


def get_user_upcoming_expenditure(
    db: Session,
    user_id: uuid.UUID,
    as_of: Optional[date] = None,
    cost_items: Optional[List[CommitmentCostItem]] = None,
) -> UpcomingExpenditureResponse:
    """Project expected commitment debits for next 30 and 60 days based on verified cadence.

    Distinguishes scheduled vs estimated dates, and excludes unconfirmed one-off transactions.
    """
    today = as_of or datetime.now(ZoneInfo("Asia/Kolkata")).date()
    cutoff_30d = today + timedelta(days=30)
    cutoff_60d = today + timedelta(days=60)

    if cost_items is None:
        cost_items = get_user_commitment_costs(db, user_id)
    # Consider active/confirmed commitments or commitments with explicit next_expected_date
    candidates = [
        item
        for item in cost_items
        if item.is_confirmed and item.representative_amount > Decimal("0.00")
    ]

    items: List[UpcomingExpenseItem] = []
    total_30d = Decimal("0.00")
    total_60d = Decimal("0.00")

    for item in candidates:
        exp_date = item.next_expected_date
        basis = "estimated_from_observed_cadence"
        cadence = (item.cadence or "").lower()
        steps = {"weekly": timedelta(days=7), "fortnightly": timedelta(days=14),
                 "monthly": relativedelta(months=1), "variable_monthly": relativedelta(months=1),
                 "quarterly": relativedelta(months=3), "annual": relativedelta(years=1)}
        step = steps.get(cadence)
        if not exp_date or step is None:
            continue
        # Anchor every occurrence to the observed billing date, including month
        # ends; include all weekly/fortnightly payments in the selected horizon.
        anchor = exp_date
        occurrence = 0
        while exp_date < today:
            occurrence += 1
            exp_date = anchor + step * occurrence
        while today <= exp_date <= cutoff_60d:
            amt = item.representative_amount
            if exp_date <= cutoff_30d:
                total_30d += amt
            total_60d += amt

            items.append(
                UpcomingExpenseItem(
                    commitment_id=item.commitment_id,
                    display_name=item.display_name,
                    merchant_id=getattr(item, "merchant_id", None),
                    merchant_name=item.merchant_name,
                    merchant_domain=getattr(item, "merchant_domain", None),
                    merchant_logo_url=getattr(item, "merchant_logo_url", None),
                    logo_source=getattr(item, "logo_source", None),
                    expected_date=exp_date,
                    estimated_amount=round_currency(amt),
                    currency=item.currency,
                    cadence=item.cadence,
                    estimation_basis=basis,
                    is_confirmed=item.is_confirmed,
                )
            )
            occurrence += 1
            exp_date = anchor + step * occurrence

    items.sort(key=lambda x: x.expected_date)

    return UpcomingExpenditureResponse(
        currency="INR",
        total_upcoming_30d=round_currency(total_30d),
        total_upcoming_60d=round_currency(total_60d),
        items=items,
    )
