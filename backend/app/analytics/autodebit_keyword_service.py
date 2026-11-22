"""High-performance keyword matching and auto-debit rail classification engine.

Uses the 908-line PayScope Auto-Debit Keyword Dictionary to classify transaction
descriptors into verified Indian and international recurring rails (UPI AutoPay,
NACH/ECS, Standing Instructions, Card Recurring, EMI, Insurance, etc.).
"""
import json
import logging
import os
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

logger = logging.getLogger(__name__)


@dataclass
class AutoDebitAnalysis:
    is_autodebit_candidate: bool
    is_failure_or_reversal: bool
    payment_rail: str
    category_hint: Optional[str]
    frequency_hint: Optional[str]
    matched_keywords: List[str] = field(default_factory=list)
    matched_categories: List[str] = field(default_factory=list)
    confidence_score: float = 0.0
    explanation: str = ""


class AutoDebitKeywordService:
    _instance: Optional["AutoDebitKeywordService"] = None

    def __init__(self, dictionary_path: Optional[str] = None):
        self.dictionary: Dict = {}
        self.categories: Dict[str, List[str]] = {}
        self.failure_keywords: Set[str] = set()
        self.frequency_keywords: Dict[str, str] = {}
        self.rail_patterns: List[Tuple[str, re.Pattern, str]] = []
        self._load_dictionary(dictionary_path)

    @classmethod
    def get_instance(cls) -> "AutoDebitKeywordService":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def _load_dictionary(self, custom_path: Optional[str] = None) -> None:
        """Locates and loads the JSON dictionary."""
        candidate_paths = [
            custom_path,
            os.path.join(os.path.dirname(__file__), "payscope_autodebit_keywords.json"),
            os.path.join(os.getcwd(), "payscope_autodebit_keywords (1).json"),
            os.path.join(os.getcwd(), "payscope_autodebit_keywords.json"),
            os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../payscope_autodebit_keywords (1).json")),
        ]

        loaded_path = None
        for p in candidate_paths:
            if p and os.path.exists(p):
                try:
                    with open(p, "r", encoding="utf-8") as f:
                        self.dictionary = json.load(f)
                    loaded_path = p
                    break
                except Exception as e:
                    logger.warning(f"Failed loading keyword dictionary from {p}: {e}")

        if not self.dictionary:
            logger.error("Could not load payscope_autodebit_keywords.json; using fallback patterns.")
            self.categories = {}
        else:
            self.categories = self.dictionary.get("categories", {})
            logger.info(f"Loaded Auto-Debit Keyword Dictionary from {loaded_path} with {len(self.categories)} categories.")

        self._compile_patterns()

    def _compile_patterns(self) -> None:
        """Pre-compiles regex patterns for ultra-fast transaction scanning."""
        # 1. Failure / Reversal keywords
        raw_failures = self.categories.get("failure_reversal_cancellation", [
            "FAILED", "REVERSED", "BOUNCE", "REVERSAL", "RETURNED", "CANCELLED", "DECLINED"
        ])
        self.failure_keywords = {k.strip().upper() for k in raw_failures if k}

        # 2. Frequency hints mapping
        freq_list = self.categories.get("frequency_billing_cycles", [])
        self.frequency_keywords = {}
        for item in freq_list:
            upper = item.strip().upper()
            if any(w in upper for w in ["MONTH", "MO", "MNTH"]):
                self.frequency_keywords[upper] = "monthly"
            elif any(w in upper for w in ["ANNUAL", "YEAR", "YR"]):
                self.frequency_keywords[upper] = "annual"
            elif any(w in upper for w in ["QUARTER", "QTR", "3-MONTH"]):
                self.frequency_keywords[upper] = "quarterly"
            elif any(w in upper for w in ["WEEK", "WK"]):
                self.frequency_keywords[upper] = "weekly"
            elif any(w in upper for w in ["FORTNIGHT", "BIWEEK", "14-DAY"]):
                self.frequency_keywords[upper] = "fortnightly"

        # 3. Categorized Rail Patterns (Sorted by keyword length descending to avoid substring bugs)
        rail_mapping = [
            ("upi_autopay_mandates", "UPI AutoPay"),
            ("nach_ecs_ach_electronic_mandates", "NACH / e-Mandate"),
            ("standing_instructions_scheduled_payments", "Standing Instruction"),
            ("card_international_recurring", "Credit Card Recurring"),
            ("subscriptions_memberships", "Subscription Mandate"),
            ("emi_loans_credit", "Loan / EMI Mandate"),
            ("insurance_investments", "Insurance Mandate"),
            ("utilities_household_bills", "Utility Auto-Pay"),
            ("software_ai_cloud_saas_merchants", "SaaS & Cloud Recurring"),
            ("entertainment_streaming_merchants", "Streaming Subscription"),
        ]

        self.rail_patterns = []
        for cat_key, rail_name in rail_mapping:
            keywords = self.categories.get(cat_key, [])
            if not keywords:
                continue

            # Sort keywords by length descending so longer phrases match first
            sorted_kw = sorted([re.escape(k.strip()) for k in keywords if len(k.strip()) >= 2], key=len, reverse=True)
            if not sorted_kw:
                continue

            # Word boundary regex
            pattern_str = r"(?i)(?:\b|_|/|-)(?:" + "|".join(sorted_kw) + r")(?:\b|_|/|-|\d|$)"
            try:
                compiled = re.compile(pattern_str)
                self.rail_patterns.append((cat_key, compiled, rail_name))
            except Exception as e:
                logger.error(f"Failed compiling regex for category {cat_key}: {e}")

    def analyze_descriptor(self, descriptor: str) -> AutoDebitAnalysis:
        """Scans a transaction descriptor against the keyword dictionary."""
        if not descriptor:
            return AutoDebitAnalysis(
                is_autodebit_candidate=False,
                is_failure_or_reversal=False,
                payment_rail="General Debit",
                category_hint=None,
                frequency_hint=None,
                matched_keywords=[],
                confidence_score=0.0,
                explanation="Empty descriptor.",
            )

        clean_text = " " + re.sub(r"\s+", " ", descriptor.strip()).upper() + " "

        # 1. Check for failure or reversal
        is_failure = False
        for fk in self.failure_keywords:
            if re.search(r"(?i)\b" + re.escape(fk) + r"\b", clean_text):
                is_failure = True
                break

        # 2. Check for frequency hint
        freq_hint = None
        for fkw, fval in self.frequency_keywords.items():
            if re.search(r"(?i)\b" + re.escape(fkw) + r"\b", clean_text):
                freq_hint = fval
                break

        # 3. Match rails and categories
        matched_categories: List[str] = []
        matched_rails: List[str] = []
        matched_tokens: List[str] = []

        for cat_key, regex, rail_name in self.rail_patterns:
            matches = regex.findall(clean_text)
            if matches:
                matched_categories.append(cat_key)
                if rail_name not in matched_rails:
                    matched_rails.append(rail_name)
                for m in matches:
                    token = m.strip().strip("/-_ ")
                    if token and token not in matched_tokens:
                        matched_tokens.append(token)

        # 4. Resolve dominant payment rail and category hint
        primary_rail = "General Debit"
        category_hint = None
        confidence = 0.0

        if any(c in matched_categories for c in ["upi_autopay_mandates"]):
            primary_rail = "UPI AutoPay"
            confidence = 0.95
        elif any(c in matched_categories for c in ["nach_ecs_ach_electronic_mandates"]):
            primary_rail = "NACH / e-Mandate"
            confidence = 0.95
        elif any(c in matched_categories for c in ["standing_instructions_scheduled_payments"]):
            primary_rail = "Standing Instruction"
            confidence = 0.90
        elif any(c in matched_categories for c in ["card_international_recurring"]):
            primary_rail = "Credit Card Recurring"
            confidence = 0.85
        elif any(c in matched_categories for c in ["subscriptions_memberships"]):
            primary_rail = "Subscription Mandate"
            confidence = 0.80
        elif any(c in matched_categories for c in ["emi_loans_credit"]):
            primary_rail = "Loan / EMI Mandate"
            confidence = 0.85
        elif any(c in matched_categories for c in ["insurance_investments"]):
            primary_rail = "Insurance Mandate"
            confidence = 0.85
        elif any(c in matched_categories for c in ["utilities_household_bills"]):
            primary_rail = "Utility Auto-Pay"
            confidence = 0.80
        elif any(c in matched_categories for c in ["entertainment_streaming_merchants"]):
            primary_rail = "Streaming Subscription"
            confidence = 0.85
        elif any(c in matched_categories for c in ["software_ai_cloud_saas_merchants"]):
            primary_rail = "SaaS & Cloud Recurring"
            confidence = 0.85

        # Category Hint resolution
        if "entertainment_streaming_merchants" in matched_categories:
            category_hint = "Entertainment & Streaming"
        elif "software_ai_cloud_saas_merchants" in matched_categories:
            category_hint = "Software, AI & Cloud"
        elif "insurance_investments" in matched_categories:
            category_hint = "Insurance & Investments"
        elif "emi_loans_credit" in matched_categories:
            category_hint = "EMI, Loans & Credit"
        elif "utilities_household_bills" in matched_categories:
            category_hint = "Utilities & Bills"
        elif "subscriptions_memberships" in matched_categories:
            category_hint = "Subscription"

        is_candidate = confidence >= 0.70 and not is_failure

        explanation = f"Matched {len(matched_tokens)} auto-debit keyword(s) on {primary_rail} rail." if matched_tokens else "Standard transaction line item."

        return AutoDebitAnalysis(
            is_autodebit_candidate=is_candidate,
            is_failure_or_reversal=is_failure,
            payment_rail=primary_rail,
            category_hint=category_hint,
            frequency_hint=freq_hint,
            matched_keywords=matched_tokens[:5],
            matched_categories=matched_categories,
            confidence_score=confidence,
            explanation=explanation,
        )


def analyze_transaction_descriptor(descriptor: str) -> AutoDebitAnalysis:
    """Convenience helper."""
    return AutoDebitKeywordService.get_instance().analyze_descriptor(descriptor)
