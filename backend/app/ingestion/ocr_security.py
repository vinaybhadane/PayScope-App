"""Validate OCR data before normalizers or AI can trust it as financial evidence."""
from datetime import date
from decimal import Decimal
import math
import re
import unicodedata
from pydantic import BaseModel, ConfigDict, Field, field_validator
from app.core.ai_security import clean_invisible, log_quarantine, text_risks
from app.ingestion.raw_text_parser import RawTextStatementParser

MIN_OCR_CONFIDENCE = 0.80


class OCRRecord(BaseModel):
    model_config = ConfigDict(extra="forbid")
    date: str
    descriptor: str = Field(min_length=2, max_length=1000)
    amount: Decimal = Field(ge=0, allow_inf_nan=False, max_digits=18, decimal_places=2)

    @field_validator("date")
    @classmethod
    def iso_date(cls, value):
        if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
            raise ValueError("OCR date must be YYYY-MM-DD")
        date.fromisoformat(value)
        return value

    @field_validator("descriptor")
    @classmethod
    def allowed_descriptor(cls, value):
        if any(not (unicodedata.category(c)[0] in {"L", "N"} or c in " .,/&()'\"+_:@#*%-") for c in value):
            raise ValueError("Unsupported OCR descriptor characters")
        return value


def confidence_score(value):
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        score = float(value)
        if not math.isfinite(score) or score < 0 or score > 100:
            return 0.0
        return score / 100 if score > 1 else score
    return None


def parse_ocr_result(result, *, initial_columns=()):
    lines = getattr(result, "lines", None)
    confidence_by_line = {}
    if lines:
        text = "\n".join(getattr(line, "text", "") for line in lines)
        for number, line in enumerate(lines, 1):
            values = [confidence_score(getattr(line, "confidence", None))]
            values.extend(confidence_score(getattr(word, "confidence", None)) for word in (getattr(line, "words", None) or []))
            available = [value for value in values if value is not None]
            confidence_by_line[number] = min(available) if available else confidence_score(getattr(result, "confidence", None))
    else:
        text = getattr(result, "text", "")
    return RawTextStatementParser.HEADERS, parse_ocr_text(
        text, confidence_score(getattr(result, "confidence", None)),
        confidence_by_line=confidence_by_line, initial_columns=initial_columns,
    )


def parse_ocr_text(text, confidence=None, line_offset=0, confidence_by_line=None, *, initial_columns=()):
    _, parsed = RawTextStatementParser.parse(clean_invisible(text), initial_columns=initial_columns)
    rows = []
    original_lines = text.splitlines()
    for row in parsed:
        line_number = row.get("_row_number", 1)
        raw_line = original_lines[line_number - 1] if line_number <= len(original_lines) else row.get("_raw_line", "")
        reasons = text_risks(raw_line) + text_risks(row["Description"], descriptor=True)
        if not row.get("_parse_error"):
            try:
                OCRRecord(date=row["Date"], descriptor=row["Description"], amount=str(row["Amount"]))
            except ValueError:
                reasons.append("invalid_ocr_schema")
        score = (confidence_by_line or {}).get(line_number, confidence)
        if score is not None and score < MIN_OCR_CONFIDENCE:
            reasons.append("low_ocr_confidence")
        row["_ocr_security"] = {"reasons": sorted(set(reasons)), "confidence": score, "line": line_number + line_offset}
        if reasons:
            log_quarantine(raw_line, reasons, line=line_number, confidence=score)
        rows.append(row)
    return rows
