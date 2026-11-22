"""Schema normalizer and row validator for transactions and notifications."""
import hashlib
from dataclasses import dataclass, field
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from typing import Any, Dict, List, Optional
from dateutil import parser as date_parser
from app.core.ai_security import clean_invisible, log_quarantine, text_risks


@dataclass
class RowError:
    """Error encountered during row parsing or validation."""

    row: int
    column: str
    value: Any
    reason: str

    def to_dict(self) -> Dict[str, Any]:
        return {
            "row": self.row,
            "column": self.column,
            "value": str(self.value) if self.value is not None else "",
            "reason": self.reason,
        }


@dataclass
class NormalizedRowResult:
    """Outcome of schema normalization across a batch of raw records."""

    valid_records: List[Dict[str, Any]]
    errors: List[RowError]
    total_rows: int
    quarantined_records: List[Dict[str, Any]] = field(default_factory=list)


class SchemaNormalizer:
    """Normalizes raw input rows into validated domain models with full error tracking."""

    @classmethod
    def normalize_transactions(
        cls,
        rows: List[Dict[str, Any]],
        mapping: Dict[str, str],
    ) -> NormalizedRowResult:
        """Normalize bank statement rows using provided column mapping."""
        valid_records: List[Dict[str, Any]] = []
        errors: List[RowError] = []
        quarantined_records = []

        from collections import Counter
        import json
        import re
        from app.ingestion.statement_values import money, statement_date, direction as parse_direction

        base_mapping = mapping
        occurrences = Counter()
        for row in rows:
            mapping = {**row.get("_column_mapping", {}),
                       **{key: value for key, value in base_mapping.items() if value in row or key.startswith("_")}}
            for key in base_mapping.get("_disabled_targets", "").split(","):
                mapping.pop(key, None)
            date_col = mapping.get("transaction_date")
            desc_col = mapping.get("raw_descriptor")
            row_num = row.get("_row_number", 0)
            raw_desc = str(row.get(desc_col) or "").strip()
            reasons = list(row.get("_ocr_security", {}).get("reasons", []))
            for key, value in row.items():
                if not key.startswith("_") and isinstance(value, str):
                    reasons.extend(text_risks(value, descriptor=key == desc_col and bool(raw_desc)))
            if reasons:
                reasons = sorted(set(reasons))
                log_quarantine(raw_desc, reasons, row=row_num)
                quarantined_records.append({"row": row_num, "descriptor": clean_invisible(raw_desc)[:120],
                                            "reasons": reasons, "source": row})
                errors.append(RowError(row_num, desc_col or "raw_descriptor", "[QUARANTINED]", "Manual review required: " + ", ".join(reasons)))
                continue
            if row.get("_parse_error"):
                errors.append(RowError(row_num, "amount", row.get("_raw_line", ""), row["_parse_error"]))
                continue
            row_errors = []
            if not raw_desc:
                row_errors.append(RowError(row_num, desc_col or "raw_descriptor", "", "Description cannot be empty."))
            parsed_date = None
            try:
                parsed_date = statement_date(row.get("_raw_date", row.get(date_col)), mapping.get("_date_order", "dayfirst"))
            except (ValueError, TypeError, OverflowError) as exc:
                row_errors.append(RowError(row_num, date_col or "transaction_date", row.get(date_col), f"Invalid date: {exc}"))

            parsed_amount, flow = None, None
            raw_amount = row.get(mapping.get("amount"))
            try:
                explicit = parse_direction(row.get(mapping.get("direction")))
                debit = money(row.get(mapping.get("debit_amount")), optional=True)
                credit = money(row.get(mapping.get("credit_amount")), optional=True)
                split = bool(mapping.get("debit_amount") or mapping.get("credit_amount"))
                if split:
                    if debit and credit:
                        raise ValueError("Both debit and credit have non-zero amounts; review this row.")
                    if debit is not None and debit != 0:
                        parsed_amount, flow = abs(debit), "debit"
                    elif credit is not None and credit != 0:
                        parsed_amount, flow = abs(credit), "credit"
                    elif debit == 0 or credit == 0:
                        parsed_amount, flow = Decimal("0.00"), explicit or ("debit" if debit is not None else "credit")
                    else:
                        raise ValueError("Debit and credit amounts are empty.")
                    if explicit and explicit != flow:
                        raise ValueError("Direction conflicts with the debit/credit column.")
                else:
                    signed = money(raw_amount)
                    suffix = re.search(r"(?i)\b(DR|CR)\s*$", str(raw_amount))
                    flow = explicit or (parse_direction(suffix.group(1)) if suffix else ("debit" if signed < 0 else "credit") if mapping.get("_amount_convention") == "negative_debit" else "credit" if str(raw_amount).strip().startswith("+") else "debit")
                    parsed_amount = abs(signed)
                currency = str(row.get(mapping.get("currency")) or "INR").strip().upper()
                if not re.fullmatch(r"[A-Z]{3}", currency):
                    raise ValueError("Currency must be a three-letter code.")
                balance = money(row.get(mapping.get("balance")), optional=True)
            except (ValueError, TypeError) as exc:
                row_errors.append(RowError(row_num, "amount", raw_amount, str(exc)))
                currency, balance = "INR", None

            status = str(row.get(mapping.get("transaction_status")) or "successful").strip().lower()
            status = {"success": "successful", "completed": "successful", "posted": "successful", "failure": "failed"}.get(status, status)
            if status not in {"successful", "failed", "pending", "reversed", "cancelled"}:
                row_errors.append(RowError(row_num, "transaction_status", status, "Unrecognized transaction status."))
            reference = str(row.get(mapping.get("payment_reference")) or "").strip() or None
            ext_id = str(row.get(mapping.get("external_synthetic_id")) or "").strip() or None
            if row_errors:
                errors.extend(row_errors)
                continue
            # Identity excludes file name and row order. An occurrence counter preserves
            # multiple identical legitimate entries in one file without a bank reference.
            if not ext_id:
                identity = [parsed_date.isoformat(), re.sub(r"\s+", " ", raw_desc).casefold(),
                            str(parsed_amount), currency, flow, reference]
                base = json.dumps(identity, ensure_ascii=False)
                occurrences[base] += 1
                digest = hashlib.sha256(f"{base}:{occurrences[base]}".encode()).hexdigest()
                ext_id = f"IMP-{digest[:40]}"
            snapshot = {k: (v.isoformat() if isinstance(v, datetime) else v) for k, v in row.items()}
            valid_records.append({
                "transaction_date": parsed_date, "raw_descriptor": raw_desc,
                "normalised_descriptor": raw_desc, "amount": parsed_amount,
                "currency": currency, "direction": flow, "transaction_status": status,
                "payment_channel": str(row.get(mapping.get("payment_channel")) or "unknown").strip().lower(),
                "payment_reference": reference, "external_synthetic_id": ext_id,
                "source_row_number": row.get("_source_row_number", row_num), "raw_source_snapshot": snapshot, "is_synthetic": False,
            })
        return NormalizedRowResult(valid_records, errors, len(rows), quarantined_records)

    @classmethod
    def normalize_notifications(
        cls,
        rows: List[Dict[str, Any]],
    ) -> NormalizedRowResult:
        """Normalize payment notification rows with full schema validation."""
        valid_records: List[Dict[str, Any]] = []
        errors: List[RowError] = []

        for row in rows:
            row_num = row.get("_row_number", 0)
            row_errors: List[RowError] = []

            # 1. External Synthetic ID
            ext_id = str(row.get("external_synthetic_id", "")).strip()
            if not ext_id:
                row_errors.append(RowError(row_num, "external_synthetic_id", "", "External synthetic ID is required."))

            # 2. Event Timestamp
            raw_ts = row.get("event_timestamp")
            parsed_ts: Optional[datetime] = None
            if not raw_ts:
                row_errors.append(RowError(row_num, "event_timestamp", "", "Event timestamp is required."))
            else:
                try:
                    if isinstance(raw_ts, datetime):
                        parsed_ts = raw_ts
                    else:
                        parsed_ts = date_parser.parse(str(raw_ts))
                    if parsed_ts.tzinfo is None:
                        parsed_ts = parsed_ts.replace(tzinfo=timezone.utc)
                except Exception as e:
                    row_errors.append(RowError(row_num, "event_timestamp", raw_ts, f"Invalid timestamp: {str(e)}"))

            # 3. Event Type
            event_type = str(row.get("event_type", "")).strip()
            if not event_type:
                row_errors.append(RowError(row_num, "event_type", "", "Event type is required."))

            # 4. Message Text
            message_text = str(row.get("message_text", "")).strip()
            if not message_text:
                row_errors.append(RowError(row_num, "message_text", "", "Message text is required."))

            # 5. Optional amount
            raw_amount = row.get("amount")
            parsed_amount: Optional[Decimal] = None
            if raw_amount is not None and str(raw_amount).strip() != "":
                try:
                    parsed_amount = Decimal(str(raw_amount)).quantize(Decimal("0.01"))
                except (InvalidOperation, ValueError):
                    row_errors.append(RowError(row_num, "amount", raw_amount, "Invalid numeric amount in notification."))

            merchant_hint = str(row.get("merchant_hint")).strip() if row.get("merchant_hint") else None
            currency = str(row.get("currency", "INR")).strip().upper() if row.get("currency") else "INR"
            mechanism = str(row.get("payment_mechanism")).strip() if row.get("payment_mechanism") else None
            mandate_ref = str(row.get("mandate_external_ref")).strip() if row.get("mandate_external_ref") else None

            if row_errors:
                errors.extend(row_errors)
            else:
                valid_records.append({
                    "external_synthetic_id": ext_id,
                    "event_timestamp": parsed_ts,
                    "event_type": event_type,
                    "merchant_hint": merchant_hint,
                    "amount": parsed_amount,
                    "currency": currency,
                    "payment_mechanism": mechanism,
                    "mandate_external_ref": mandate_ref,
                    "message_text": message_text,
                    "raw_payload": {k: v for k, v in row.items() if not k.startswith("_")},
                    "source_row_number": row_num,
                    "is_synthetic": True,
                })

        return NormalizedRowResult(
            valid_records=valid_records,
            errors=errors,
            total_rows=len(rows),
        )

