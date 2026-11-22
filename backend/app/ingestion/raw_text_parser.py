"""Robust raw text statement parser for bank statements.

Extracts line-by-line transaction records (Date, Description, Amount, Direction, Channel)
from unformatted text, OCR output, and text-based PDFs that lack explicit table grids.
"""
import re
from typing import Any, Dict, List, Tuple
from app.ingestion.statement_values import money, statement_date

# Matches various international & Indian bank date patterns:
# YYYY-MM-DD, DD/MM/YYYY, DD-MM-YYYY, DD Mon YYYY, Mon DD YYYY
DATE_REGEX = re.compile(
    r"(?P<date>\b\d{4}[-/.]\d{1,2}[-/.]\d{1,2}\b|"
    r"\b\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}\b|"
    r"\b\d{1,2}[-/.][a-z]{3,9}[-/.]\d{2,4}\b|"
    r"\b\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4}\b|"
    r"\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{2,4}\b)",
    re.IGNORECASE,
)

_NUMBER = r"[+-]?(?:\d{1,3}(?:,\d{2,3})+|\d+)(?:\.\d{1,2})?"
_CURRENCY = r"[\u20b9$\u20ac\u00a3]|INR|USD|EUR|GBP|Rs\.?"
_AMOUNT_TOKEN = re.compile(rf"(?<![\w/.-])(?:(?:{_CURRENCY})\s*)?({_NUMBER})(?![\w/.-])", re.I)
_EMPTY_CELL = r"(?:--?|[\u2013\u2014\u2212]|n/a|nil)"
_MONEY_CELL = rf"(?:(?:{_CURRENCY})\s*)?{_NUMBER}"
_SPLIT_CELL = rf"(?:{_EMPTY_CELL}|{_MONEY_CELL})"
_UNLABELED_SPLIT = re.compile(
    rf"\s(?:{_MONEY_CELL}\s+{_EMPTY_CELL}|{_EMPTY_CELL}\s+{_MONEY_CELL})\s+{_MONEY_CELL}\s*$", re.I
)
_HEADER_COLUMNS = re.compile(
    r"(?P<debit>\b(?:debits?|withdrawals?|paid out|money out|dr\s+(?:amount|amt))\b)|"
    r"(?P<credit>\b(?:credits?|deposits?|paid in|money in|cr\s+(?:amount|amt))\b)|"
    r"(?P<balance>\bbalance\b)",
    re.I,
)
_DATE_START = re.compile(r"^\s*(?:\d+\s+)?" + DATE_REGEX.pattern, re.I)
_OTHER_LINE = re.compile(r"(?i)balance|\bpage\b|\bstatement\b|\btotal\b|date.*amount|account |ifsc|customer")


def statement_amount_columns(line):
    """Read amount column order from a table header in flattened text."""
    date = re.search(r"\bdate\b", line, re.I)
    if not date:
        return None
    columns = tuple(match.lastgroup for match in _HEADER_COLUMNS.finditer(line, date.end()))
    if "debit" in columns and "credit" in columns and not re.search(r"\b(?:debit\s*/\s*credit|credit\s*/\s*debit)\b", line, re.I):
        return columns
    if "balance" in columns or re.search(r"\b(?:amount|amt)\b", line[date.end():], re.I):
        return ("balance",) if "balance" in columns else ("amount",)
    return None


def _read_split_amounts(rest, columns):
    """Use the explicit blank cells to recover split debit and credit columns."""
    if len(columns) != len(set(columns)):
        raise ValueError("The statement has multiple debit, credit or balance headers; review their column mapping.")
    fields = [rf"(?P<{column}>{_SPLIT_CELL})" if column != "balance" else
              rf"(?P<balance>{_MONEY_CELL}(?:\s+(?:DR|CR))?)" for column in columns]
    match = re.search(r"(?<=\s)" + r"\s+".join(fields) + r"\s*$", rest, re.I)
    if not match:
        raise ValueError("Cannot tell which amount is a debit or credit. Use a copy with visible columns or a CSV/Excel export.")
    values = match.groupdict()
    debit = money(values["debit"], optional=True)
    credit = money(values["credit"], optional=True)
    if debit == 0 and credit is None:
        return rest[:match.start()].strip(), debit, "debit", values.get("balance", "")
    if credit == 0 and debit is None:
        return rest[:match.start()].strip(), credit, "credit", values.get("balance", "")
    if bool(debit) == bool(credit):
        raise ValueError("Expected one non-zero amount in the debit or credit column; review this row.")
    return rest[:match.start()].strip(), abs(debit or credit), "debit" if debit else "credit", values.get("balance", "")


class RawTextStatementParser:
    """Parses arbitrary raw text and OCR statement lines into structured rows."""

    HEADERS = ["Date", "Description", "Amount", "Currency", "Direction", "Channel", "Balance"]

    @classmethod
    def parse(cls, text: str, *, initial_columns=()) -> Tuple[List[str], List[Dict[str, Any]]]:
        if not text or not text.strip():
            return [], []

        rows = []
        entries = []
        columns = initial_columns
        for number, line in enumerate(text.splitlines(), 1):
            match = _DATE_START.search(line)
            if match:
                entries.append([number, match.group("date"), line[match.end():].strip(), columns])
                continue
            header = statement_amount_columns(line)
            if header is not None:
                columns = header
            elif entries and line.strip() and not _OTHER_LINE.search(line):
                entries[-1][2] += " " + line.strip()
        for number, date_text, rest, columns in entries:
            row = {"Date": date_text, "Description": rest, "Amount": "", "Currency": "INR", "Direction": "debit", "Channel": "unknown", "Balance": "", "_row_number": number, "_raw_line": date_text + " " + rest}
            row["_raw_date"] = date_text
            row["_amount_columns"] = list(columns)
            try:
                row["Date"] = statement_date(date_text).strftime("%Y-%m-%d")
                if "debit" in columns and "credit" in columns:
                    desc, amount, flow, balance = _read_split_amounts(rest, columns)
                else:
                    if not columns and _UNLABELED_SPLIT.search(rest):
                        raise ValueError("This row looks like it has separate debit, credit and balance cells. Include the column header to identify them.")
                    desc, amount, flow, balance = _read_single_amount(rest, has_balance="balance" in columns)
                # A second date is a value date, not part of the merchant name.
                desc = re.sub(r"^\s*" + DATE_REGEX.pattern, "", desc, count=1, flags=re.I).strip()
                if not desc:
                    raise ValueError("Description is missing.")
                row["Description"] = desc
                row["Amount"] = str(amount)
                row["Direction"] = flow
                row["Balance"] = balance
                currency = re.search(r"\b(INR|USD|EUR|GBP)\b|([$\u20ac\u00a3\u20b9])", rest)
                if currency:
                    row["Currency"] = currency.group(1) or {"$": "USD", "\u20ac": "EUR", "\u00a3": "GBP", "\u20b9": "INR"}[currency.group(2)]
            except ValueError as exc:
                row["_parse_error"] = str(exc)
            rows.append(row)
        return (cls.HEADERS, rows) if rows else ([], [])


def _read_single_amount(rest, *, has_balance):
    # Only trailing monetary columns count; narration numbers can be references.
    ending = []
    for candidate in reversed(list(_AMOUNT_TOKEN.finditer(rest))):
        tail = rest[candidate.end():ending[-1].start() if ending else len(rest)]
        if re.sub(r"(?i)\b(?:dr|cr|debit|credit|bal|balance|inr|rs|usd|eur|gbp)\b|[\u20b9$\u20ac\u00a3\s:.]", "", tail):
            break
        ending.append(candidate)
    if not ending:
        raise ValueError("No unambiguous trailing transaction amount; review this line.")
    if len(ending) != (2 if has_balance else 1):
        raise ValueError("Multiple amounts or references cannot be assigned safely; review this line.")
    chosen = ending[-1]
    value = chosen.group(1)
    if len(value.replace(",", "").lstrip("+-")) >= 9 and "." not in value:
        raise ValueError("Trailing number could be a payment reference; no decimal amount was found.")
    suffix = rest[chosen.end():ending[0].start() if has_balance else len(rest)]
    flow = "credit" if re.search(r"(?i)\b(?:CR|credit)\b", suffix) or value.startswith("+") else "debit"
    return rest[:chosen.start()].strip(), abs(money(value)), flow, ending[0].group(1) if has_balance else ""
