"""Record explicitly cancelled mandates linked by a unique reference."""
import re
from collections import defaultdict
from datetime import datetime, timezone

from sqlalchemy import func, select

from app.models.cancellation_event import CancellationEvent
from app.models.commitment import Commitment
from app.models.commitment_transaction import CommitmentTransaction
from app.models.transaction import Transaction

CONFIRMED_CLOSURES = {"user_confirmed", "explicit_notification", "provider_confirmed", "explicit_statement"}
_CLOSURE = re.compile(
    r"\b(?:mandate|auto\s*pay|auto[- ]?debit|standing instruction)\s+(?:(?:was|is)\s+)?"
    r"(?:cancelled|canceled|revoked|terminated|deactivated)\b", re.I,
)


def mark_ended(commitment, timestamp):
    if timestamp.tzinfo is None:
        timestamp = timestamp.replace(tzinfo=timezone.utc)
    if commitment.status == "marked_not_recurring" or timestamp > datetime.now(timezone.utc):
        return
    if commitment.user_decision == "confirmed" and commitment.user_decision_at and commitment.user_decision_at > timestamp:
        return
    commitment.status = "ended_user_recorded"
    commitment.user_decision = "cancelled"
    commitment.user_decision_at = timestamp
    commitment.next_expected_date = None


def sync_statement_closures(db, user_id):
    now = datetime.now(timezone.utc)
    events = list(db.scalars(select(CancellationEvent).where(
        CancellationEvent.user_id == user_id, CancellationEvent.confirmation_type.in_(CONFIRMED_CLOSURES),
        CancellationEvent.event_timestamp <= now,
    )).all())
    notices = db.scalars(select(Transaction).where(
        Transaction.user_id == user_id, Transaction.payment_reference.is_not(None),
        Transaction.transaction_date <= now,
        (Transaction.amount == 0) | (func.lower(Transaction.transaction_status) == "cancelled"),
        Transaction.raw_descriptor.op("~*")(r"mandate|auto[ -]?(pay|debit)|standing instruction"),
    )).all()
    notices = [row for row in notices if _CLOSURE.search(row.raw_descriptor)]
    if notices:
        linked = db.execute(select(CommitmentTransaction.commitment_id, Transaction).join(
            Transaction, Transaction.id == CommitmentTransaction.transaction_id,
        ).join(Commitment, Commitment.id == CommitmentTransaction.commitment_id).where(
            Transaction.user_id == user_id, Commitment.user_id == user_id,
            func.lower(Transaction.payment_reference).in_({r.payment_reference.lower() for r in notices}),
        ))
        by_reference = defaultdict(dict)
        for cid, transaction in linked:
            matches = by_reference[(transaction.account_id, transaction.currency, transaction.payment_reference.lower())]
            if cid not in matches or transaction.transaction_date < matches[cid].transaction_date:
                matches[cid] = transaction
        already = {(event.commitment_id, event.event_timestamp) for event in events if event.confirmation_type == "explicit_statement"}
        for row in notices:
            matches = by_reference[(row.account_id, row.currency, row.payment_reference.lower())]
            if len(matches) != 1:
                continue
            (cid, evidence), = matches.items()
            if row.transaction_date <= evidence.transaction_date or (cid, row.transaction_date) in already:
                continue
            event = CancellationEvent(
                user_id=user_id, commitment_id=cid, merchant_id=evidence.merchant_id,
                event_timestamp=row.transaction_date, event_source="statement",
                confirmation_type="explicit_statement", evidence_description="Mandate closure shown in the uploaded statement.",
                is_synthetic=row.is_synthetic,
            )
            db.add(event)
            events.append(event)
            already.add((cid, row.transaction_date))
    latest = {}
    for event in sorted(events, key=lambda e: e.event_timestamp):
        latest[event.commitment_id] = event.event_timestamp
    if latest:
        for commitment in db.scalars(select(Commitment).where(
            Commitment.user_id == user_id, Commitment.id.in_(latest),
        )):
            mark_ended(commitment, latest[commitment.id])
