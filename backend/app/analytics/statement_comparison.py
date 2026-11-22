"""Compare the actual payments represented by two statement uploads."""
import re
import uuid
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from decimal import Decimal

from sqlalchemy import func, select, union_all
from sqlalchemy.orm import Session

from app.analytics.date_utils import estimate_next_expected_date
from app.matching.normalizer import DescriptorNormalizer
from app.models.cancellation_event import CancellationEvent
from app.models.commitment import Commitment
from app.models.commitment_transaction import CommitmentTransaction
from app.models.import_record import ImportRecord
from app.models.statement_duplicate import StatementDuplicate
from app.models.transaction import Transaction
from app.analytics.cancellations.statement_evidence import CONFIRMED_CLOSURES

ZERO = Decimal("0.00")


def _membership(user_id):
    original = select(Transaction.import_id, Transaction.id.label("transaction_id")).where(
        Transaction.user_id == user_id, Transaction.import_id.is_not(None),
    )
    duplicates = select(StatementDuplicate.import_id, StatementDuplicate.transaction_id).join(
        Transaction, Transaction.id == StatementDuplicate.transaction_id,
    ).where(Transaction.user_id == user_id)
    return union_all(original, duplicates).subquery()


def list_statements(db: Session, user_id: uuid.UUID):
    members = _membership(user_id)
    start = func.min(Transaction.transaction_date)
    end = func.max(Transaction.transaction_date)
    query = (
        select(ImportRecord, start, end, func.count(Transaction.id))
        .join(members, members.c.import_id == ImportRecord.id)
        .join(Transaction, Transaction.id == members.c.transaction_id)
        .where(ImportRecord.user_id == user_id, ImportRecord.source_label == "bank_statement",
               ImportRecord.status.in_(["completed", "partially_completed"]))
        .group_by(ImportRecord.id)
        .order_by(end.desc(), start.desc(), ImportRecord.created_at.desc(), ImportRecord.id)
    )
    return [
        {
            "id": str(record.id), "filename": record.filename_display,
            "start": first.date().isoformat(), "end": last.date().isoformat(),
            "payment_count": count, "unreadable_count": record.rejected_count,
            "unlinked_count": max(0, record.accepted_count + record.duplicate_count - count),
        }
        for record, first, last, count in db.execute(query)
    ]


def _totals(payments):
    totals = defaultdict(lambda: {"inflow": ZERO, "outflow": ZERO, "count": 0})
    for payment in payments:
        key = "inflow" if payment.direction.lower() == "credit" else "outflow"
        totals[payment.currency][key] += payment.amount
        totals[payment.currency]["count"] += 1
    return totals


def _show_totals(total):
    return {**{key: str(total[key]) for key in ("inflow", "outflow")},
            "net": str(total["inflow"] - total["outflow"]), "count": total["count"]}


def _new_flows(previous, current):
    def flow_key(payment):
        name = DescriptorNormalizer.normalize(payment.raw_descriptor)
        name = re.sub(r"\b(?:[a-z]*\d{6,}[a-z0-9]*|ref\d+)\b", " ", name)
        name = " ".join(name.split())
        return payment.currency, payment.direction.lower(), payment.merchant_id or name

    prior = {flow_key(p) for p in previous}
    groups = {}
    for payment in current:
        key = flow_key(payment)
        if key in prior:
            continue
        item = groups.setdefault(key, {
            "label": payment.raw_descriptor, "currency": key[0], "direction": key[1],
            "amount": ZERO, "count": 0, "transaction_id": str(payment.id),
        })
        item["amount"] += payment.amount
        item["count"] += 1
    return [dict(item, amount=str(item["amount"])) for item in sorted(groups.values(), key=lambda i: -i["amount"])]


def _commitment_changes(db, user_id, previous, current, earlier, later):
    old = {t.id: t for t in previous if t.direction.lower() == "debit"}
    new = {t.id: t for t in current if t.direction.lower() == "debit"}
    if not (old or new):
        return []
    # Scope by owner on both sides, including for user supplied statement IDs.
    links = db.execute(
        select(Commitment, CommitmentTransaction.transaction_id)
        .join(CommitmentTransaction, CommitmentTransaction.commitment_id == Commitment.id)
        .join(Transaction, Transaction.id == CommitmentTransaction.transaction_id)
        .where(Commitment.user_id == user_id, Transaction.user_id == user_id,
               CommitmentTransaction.transaction_id.in_(set(old) | set(new)),
               Commitment.status != "marked_not_recurring")
    )
    groups = {}
    for commitment, tid in links:
        _, old_payments, new_payments = groups.setdefault(commitment.id, (commitment, [], []))
        if tid in old:
            old_payments.append(old[tid])
        if tid in new:
            new_payments.append(new[tid])

    closures = {}
    if groups:
        events = db.scalars(select(CancellationEvent).where(
            CancellationEvent.user_id == user_id, CancellationEvent.commitment_id.in_(groups),
            CancellationEvent.confirmation_type.in_(CONFIRMED_CLOSURES),
            CancellationEvent.event_timestamp <= datetime.now(timezone.utc),
        ).order_by(CancellationEvent.event_timestamp.desc(), CancellationEvent.id)).all()
        for event in events:
            closures.setdefault(event.commitment_id, event)

    changes = []
    for commitment, old_payments, new_payments in groups.values():
        old_charge = max(old_payments, key=_payment_order, default=None)
        new_charge = max(new_payments, key=_payment_order, default=None)
        last_charge = new_charge or old_charge

        def add(kind, *, date=None, evidence=None):
            changes.append({
                "kind": kind, "commitment_id": str(commitment.id), "label": commitment.display_name,
                "currency": commitment.currency, "cadence": commitment.cadence, "evidence": evidence,
                "previous_amount": str(old_charge.amount) if old_charge else None,
                "current_amount": str(new_charge.amount) if new_charge else None,
                "date": date or last_charge.transaction_date.date().isoformat(),
            })

        closure = closures.get(commitment.id)
        if closure and closure.event_timestamp.date().isoformat() >= earlier["start"]:
            kind = "closure_recorded" if closure.confirmation_type == "user_confirmed" else "closed"
            closed_on = closure.event_timestamp.date()
            add(kind, date=closed_on.isoformat(), evidence=closure.confirmation_type)
            if new_charge and new_charge.transaction_date.date() > closed_on:
                add("charge_after_closure", evidence=closure.confirmation_type)
        if old_charge and new_charge:
            if new_charge.transaction_date > old_charge.transaction_date and new_charge.amount != old_charge.amount:
                add("price_increase" if new_charge.amount > old_charge.amount else "price_decrease")
        elif new_charge and commitment.first_seen_at.date().isoformat() >= later["start"]:
            add("new_subscription" if commitment.cadence != "unknown" else "new_candidate")
        elif old_charge and not closure and earlier["end"] < later["start"]:
            expected = estimate_next_expected_date(old_charge.transaction_date.date(), commitment.cadence, 0)
            if (expected and later["start"] <= expected.isoformat()
                    and (expected + timedelta(days=3)).isoformat() <= later["end"]):
                add("payment_not_seen", date=expected.isoformat())
    return sorted(changes, key=lambda change: (change["kind"], change["label"].lower(), change["commitment_id"]))


def _payment_order(tx):
    return tx.transaction_date, tx.source_row_number or 0, tx.id


def compare_statements(db, user_id, previous_id=None, current_id=None):
    statements = list_statements(db, user_id)
    result = {"statements": statements, "previous": None, "current": None, "totals": [], "changes": [],
              "new_flows": [], "shared_payments": 0, "overlapping_dates": False}
    by_id = {item["id"]: item for item in statements}
    current = by_id.get(str(current_id)) if current_id else next(iter(statements), None)
    if current_id and not current:
        raise LookupError("The selected statement is unavailable.")
    if not current:
        return result
    previous = by_id.get(str(previous_id)) if previous_id else next((
        row for row in statements if row["id"] != current["id"] and (row["end"], row["start"]) < (current["end"], current["start"])
    ), None)
    if previous_id and not previous:
        raise LookupError("The selected statement is unavailable.")
    if not previous:
        return dict(result, current=current)
    if previous["id"] == current["id"] or (previous["end"], previous["start"]) > (current["end"], current["start"]):
        raise ValueError("Choose two different statements, with the earlier statement on the left.")

    members = _membership(user_id)
    statement_ids = [uuid.UUID(previous["id"]), uuid.UUID(current["id"])]
    query = select(members.c.import_id, Transaction).join(Transaction, Transaction.id == members.c.transaction_id).where(
        members.c.import_id.in_(statement_ids), Transaction.user_id == user_id,
        func.lower(Transaction.transaction_status) == "successful",
        func.lower(Transaction.direction).in_(["debit", "credit"]),
    ).order_by(Transaction.transaction_date, Transaction.id)
    payments = defaultdict(dict)
    for statement_id, payment in db.execute(query):
        payments[statement_id][payment.id] = payment
    old, new = (list(payments[sid].values()) for sid in statement_ids)
    shared = len(payments[statement_ids[0]].keys() & payments[statement_ids[1]].keys())
    old_totals, new_totals = _totals(old), _totals(new)
    for currency in sorted(old_totals.keys() | new_totals.keys()):
        before, after = old_totals[currency], new_totals[currency]
        delta = {key: after[key] - before[key] for key in before}
        result["totals"].append({"currency": currency, "previous": _show_totals(before),
                                 "current": _show_totals(after), "delta": _show_totals(delta)})
    return dict(result, previous=previous, current=current, shared_payments=shared,
                overlapping_dates=previous["end"] >= current["start"],
                changes=_commitment_changes(db, user_id, old, new, previous, current),
                new_flows=_new_flows(old, new))
