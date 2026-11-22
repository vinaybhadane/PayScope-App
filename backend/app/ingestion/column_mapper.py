"""Column mapping and auto-detection for statement ingestion."""
from typing import Dict, List, Optional, Set
import re


class ColumnMapper:
    """Detects and maps source headers to standard internal transaction schema."""

    TARGET_COLUMNS = [
        "transaction_date",
        "raw_descriptor",
        "amount",
        "debit_amount",
        "credit_amount",
        "balance",
        "currency",
        "direction",
        "payment_channel",
        "payment_reference",
        "external_synthetic_id",
        "transaction_status",
    ]

    REQUIRED_TARGETS: Set[str] = {"transaction_date", "raw_descriptor"}

    ALIASES: Dict[str, List[str]] = {
        "transaction_date": [
            "date",
            "transaction_date",
            "txn_date",
            "value_date",
            "posting_date",
            "trans_date",
            "txn date",
            "transaction date",
            "trans date",
        ],
        "raw_descriptor": [
            "description",
            "raw_descriptor",
            "narration",
            "particulars",
            "merchant",
            "details",
            "remarks",
            "transaction_details",
            "narrative",
            "payee",
        ],
        "amount": [
            "amount",
            "txn_amount",
            "transaction_amount",
            "net amount",
            "amt",
        ],
        "debit_amount": ["debit", "withdrawal", "withdrawal_amount", "debit_amount", "withdrawals", "withdrawal amt", "debit amt", "paid out", "money out"],
        "credit_amount": ["credit", "deposit", "deposit_amount", "credit_amount", "deposits", "deposit amt", "credit amt", "paid in", "money in"],
        "balance": ["balance", "closing balance", "running balance", "available balance"],
        "currency": ["currency", "curr", "ccy"],
        "direction": ["direction", "type", "dr_cr", "cr_dr", "txn_type", "debit/credit", "dr/cr"],
        "payment_channel": ["channel", "payment_channel", "mode", "payment_mode", "instrument"],
        "payment_reference": ["mandate_reference", "mandate_id", "umrn", "reference", "ref", "payment_reference", "ref_no", "reference_number", "utr", "cheque_no"],
        "external_synthetic_id": ["external_synthetic_id", "external_id", "synthetic_id", "id", "txn_id", "transaction_id"],
        "transaction_status": ["status", "transaction_status", "txn_status"],
    }

    @classmethod
    def auto_detect(cls, headers: List[str]) -> Dict[str, str]:
        """Automatically map source headers to standard target fields."""
        mapping: Dict[str, str] = {}  # target_field -> source_header
        assigned_sources: Set[str] = set()

        # Clean source headers for comparison
        def clean(value):
            value = re.sub(r"\((?:inr|rs\.?|₹|usd)\)", "", value, flags=re.I)
            return re.sub(r"[^a-z0-9]", "", value.lower())
        cleaned_headers = {h: clean(h) for h in headers}

        for target, aliases in cls.ALIASES.items():
            for alias in aliases:
                norm_alias = clean(alias)
                for orig_h, clean_h in cleaned_headers.items():
                    if orig_h not in assigned_sources and clean_h == norm_alias:
                        mapping[target] = orig_h
                        assigned_sources.add(orig_h)
                        break
                if target in mapping:
                    break

        # Retain the legacy amount mapping for debit-only exports while also
        # exposing split columns. The normalizer always gives split columns priority.
        if "amount" not in mapping and "debit_amount" in mapping:
            mapping["amount"] = mapping["debit_amount"]
        return mapping

    @classmethod
    def apply_overrides(
        cls,
        detected_mapping: Dict[str, str],
        user_overrides: Optional[Dict[str, str]] = None,
    ) -> Dict[str, str]:
        """Merge auto-detected mapping with user-supplied column mappings."""
        final_mapping = dict(detected_mapping)
        if user_overrides:
            for target, source in user_overrides.items():
                if source:
                    final_mapping[target] = source
                elif target in final_mapping:
                    del final_mapping[target]
            disabled = [target for target, source in user_overrides.items() if not source and target in cls.TARGET_COLUMNS]
            if disabled:
                final_mapping["_disabled_targets"] = ",".join(disabled)
        return final_mapping

    @classmethod
    def validate_mapping(cls, mapping: Dict[str, str], available_headers: List[str]) -> List[str]:
        """Check whether required target columns are mapped to valid available headers."""
        errors: List[str] = []
        if not any(mapping.get(key) for key in ("amount", "debit_amount", "credit_amount")):
            errors.append("Missing amount column: map Amount or Debit/Credit columns.")
        for req in cls.REQUIRED_TARGETS:
            if req not in mapping or not mapping[req]:
                errors.append(f"Missing required column mapping for '{req}'.")
            elif mapping[req] not in available_headers:
                errors.append(f"Mapped header '{mapping[req]}' for '{req}' not found in file headers.")
        for target, source in mapping.items():
            if target in cls.TARGET_COLUMNS and source not in available_headers:
                errors.append(f"Mapped header '{source}' for '{target}' not found in file headers.")
        return errors

