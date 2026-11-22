"""Owner-scoped payment ledger, with decimal totals kept separate by currency."""
import uuid
from decimal import Decimal
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select, func, case
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.models.transaction import Transaction
from app.models.import_record import ImportRecord
from app.repositories.user_repository import resolve_workspace_user_id

router = APIRouter(prefix="/transactions", tags=["Payments"])


def ledger_totals(db, conditions):
    successful = Transaction.transaction_status == "successful"
    rows = db.execute(select(Transaction.currency, func.count(Transaction.id).label("count"),
        func.sum(case((successful & (Transaction.direction == "debit"), Transaction.amount), else_=0)).label("debits"),
        func.sum(case((successful & (Transaction.direction == "credit"), Transaction.amount), else_=0)).label("credits"),
        func.sum(case((~successful, 1), else_=0)).label("excluded_count"))
        .where(*conditions).group_by(Transaction.currency).order_by(Transaction.currency)).all()
    return [{"currency": row.currency, "count": row.count, "debits": str(row.debits or Decimal("0.00")),
             "credits": str(row.credits or Decimal("0.00")), "net_outflow": str((row.debits or 0) - (row.credits or 0)),
             "excluded_count": row.excluded_count} for row in rows]


@router.get("")
def payments(search: str = Query("", max_length=200), direction: Optional[str] = Query(None, pattern="^(debit|credit)$"),
             currency: Optional[str] = Query(None, pattern="^[A-Z]{3}$"),
             limit: int = Query(50, ge=1, le=200), offset: int = Query(0, ge=0), db: Session = Depends(get_db)):
    uid = resolve_workspace_user_id(db)
    conditions = [Transaction.user_id == uid]
    if search:
        # Escape SQL LIKE wildcards so a search means a literal substring.
        escaped = search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        conditions.append(Transaction.raw_descriptor.ilike(f"%{escaped}%", escape="\\"))
    if direction:
        conditions.append(Transaction.direction == direction)
    if currency:
        conditions.append(Transaction.currency == currency)
    total = db.scalar(select(func.count(Transaction.id)).where(*conditions)) or 0
    rows = db.execute(select(Transaction, ImportRecord.filename_display)
        .outerjoin(ImportRecord, (ImportRecord.id == Transaction.import_id) & (ImportRecord.user_id == uid))
        .where(*conditions).order_by(Transaction.transaction_date.desc(), Transaction.id)
        .offset(offset).limit(limit)).all()
    return {"total": total, "totals": ledger_totals(db, conditions), "items": [
        {"id": str(tx.id), "date": tx.transaction_date, "description": tx.raw_descriptor,
         "amount": str(tx.amount), "currency": tx.currency, "direction": tx.direction,
         "status": tx.transaction_status, "channel": tx.payment_channel,
         "reference": tx.payment_reference, "filename": filename, "source_row": tx.source_row_number,
         "source_page": (tx.raw_source_snapshot or {}).get("_page"),
         "source_sheet": (tx.raw_source_snapshot or {}).get("_sheet")} for tx, filename in rows]}


@router.get("/{transaction_id}")
def payment_evidence(transaction_id: uuid.UUID, db: Session = Depends(get_db)):
    tx = db.scalar(select(Transaction).where(Transaction.id == transaction_id, Transaction.user_id == resolve_workspace_user_id(db)))
    if tx is None:
        raise HTTPException(404, "Payment not found.")
    return {"id": str(tx.id), "source": tx.raw_source_snapshot, "source_row": tx.source_row_number,
            "amount": str(tx.amount), "direction": tx.direction, "currency": tx.currency}
