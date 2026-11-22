"""Strict bank-statement scalar parsing shared by structured and OCR imports."""
import re
from datetime import date, datetime, timezone
from decimal import Decimal, InvalidOperation
from dateutil import parser

EMPTY = {"", "-", "--", "\u2013", "\u2014", "\u2212", "n/a", "na", "nil", "none", "null"}


def money(value, *, optional=False):
    if value is None or str(value).strip().lower() in EMPTY:
        if optional:
            return None
        raise ValueError("Amount cannot be empty.")
    raw = str(value).strip()
    negative = raw.startswith("(") and raw.endswith(")")
    raw = re.sub(r"(?i)\b(?:INR|USD|EUR|GBP|DR|CR)\b|\bRs\.?|[₹$€£,\s()]", "", raw)
    try:
        amount = Decimal(raw)
        if negative:
            amount = -abs(amount)
        if not amount.is_finite() or abs(amount) >= Decimal("10000000000000000"):
            raise ValueError("Amount is outside the supported range.")
        if amount != amount.quantize(Decimal("0.01")):
            raise ValueError("Amount has more than two decimal places; review the number format.")
        return amount.quantize(Decimal("0.01"))
    except InvalidOperation as exc:
        raise ValueError("Invalid numeric amount.") from exc


def statement_date(value, order="dayfirst"):
    if isinstance(value, datetime):
        result = value
    elif isinstance(value, date):
        result = datetime.combine(value, datetime.min.time())
    else:
        raw = str(value or "").strip()
        # A year is required; dateutil otherwise fills missing fields with today.
        complete_date = (r"\d{4}[-/.]\d{1,2}[-/.]\d{1,2}(?:[T ].*)?|"
                         r"\d{1,2}[-/.]\d{1,2}[-/.](?:\d{2}|\d{4})(?:[T ].*)?|"
                         r"\d{1,2}[-/ .][A-Za-z]{3,9}[-/ .](?:\d{2}|\d{4})|"
                         r"[A-Za-z]{3,9}\s+\d{1,2},?\s+(?:\d{2}|\d{4})")
        if not re.fullmatch(complete_date, raw):
            raise ValueError("A complete transaction date including year is required.")
        iso = bool(re.match(r"^\d{4}[-/.]", raw))
        result = parser.parse(raw, dayfirst=not iso and order != "monthfirst", yearfirst=iso, fuzzy=False)
    return result.replace(tzinfo=timezone.utc) if result.tzinfo is None else result


def direction(value):
    token = str(value or "").strip().lower()
    if token in {"debit", "dr", "d", "withdrawal", "withdraw", "out", "paid out", "-"}:
        return "debit"
    if token in {"credit", "cr", "c", "deposit", "in", "paid in", "+"}:
        return "credit"
    if token:
        raise ValueError(f"Unknown debit/credit indicator: {value}.")
    return None
