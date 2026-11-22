"""Shared, deterministic checks on untrusted text at ingestion and model boundaries."""
import hashlib
import logging
import re
import unicodedata

logger = logging.getLogger(__name__)
INSTRUCTION = re.compile(r"\b(system|ignore|instructions?|admin|prompt|override)\b", re.I)
URL = re.compile(r"(?:[a-z][a-z0-9+.-]*://|www\.|mailto:|javascript:|data:|//[a-z0-9])", re.I)
ACCOUNT = re.compile(r"\b\d{9,18}\b")
IFSC = re.compile(r"\b[A-Z]{4}0[A-Z0-9]{6}\b", re.I)
UPI = re.compile(r"[\w.\-+]+@([a-zA-Z][a-zA-Z0-9.-]*)")


def redact_sensitive(value: str) -> str:
    return UPI.sub(r"***@\1", IFSC.sub("[REDACTED_IFSC]", ACCOUNT.sub("[REDACTED_ACCT]", value)))


def clean_invisible(value: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFKC", value) if unicodedata.category(c) not in {"Cf", "Co"})


def text_risks(value: str, *, descriptor: bool = False) -> list[str]:
    clean = clean_invisible(value)
    reasons = []
    if any(unicodedata.category(c) in {"Cf", "Co"} for c in value):
        reasons.append("invisible_unicode")
    # Standing Instruction is a legitimate bank narration, including in source
    # descriptors. Other instruction-like phrases still require review.
    instruction_text = re.sub(r"\bstanding instructions?\b", "", clean, flags=re.I)
    if INSTRUCTION.search(instruction_text):
        reasons.append("instruction_like_text")
    if URL.search(clean):
        reasons.append("url_in_text")
    if descriptor and (not clean.strip() or len(clean) > 1000):
        reasons.append("descriptor_length")
    return reasons


def log_quarantine(value: str, reasons: list[str], **metadata):
    # Raw text remains in the owner-scoped review queue; logs contain no account PII.
    logger.warning("Security quarantine: digest=%s reasons=%s metadata=%s",
                   hashlib.sha256(value.encode()).hexdigest(), ",".join(reasons), metadata)
