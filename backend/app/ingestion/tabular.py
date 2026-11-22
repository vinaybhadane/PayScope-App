"""Find statement tables below bank metadata, retaining column positions."""
import re
from datetime import date, datetime
from app.ingestion.column_mapper import ColumnMapper


def statement_matrix(matrix, *, sheet=None):
    headers, rows = [], []
    for number, values in enumerate(matrix, 1):
        values = list(values)
        if not any(value is not None and str(value).strip() for value in values):
            continue
        possible = [re.sub(r"\s+", " ", str(value or "")).strip() for value in values]
        mapping = ColumnMapper.auto_detect(possible)
        if not ColumnMapper.validate_mapping(mapping, possible):
            headers = [value or f"Column {index + 1}" for index, value in enumerate(possible)]
            if len(set(headers)) != len(headers):
                raise ValueError("Statement has duplicate column names. Rename them before importing.")
            continue
        if not headers:
            continue
        row = {header: values[index] if index < len(values) else None for index, header in enumerate(headers)}
        mapping = ColumnMapper.auto_detect(headers)
        date_value = row.get(mapping.get("transaction_date"))
        if re.match(r"(?i)^(?:total|opening balance|closing balance|page |statement summary|generated on)", str(date_value or "").strip()):
            continue
        for key, value in row.items():
            if isinstance(value, (date, datetime)):
                row[key] = value.isoformat()
            elif isinstance(value, str):
                row[key] = value.strip()
        row["_row_number"] = number
        if sheet:
            row["_sheet"] = sheet
        rows.append(row)
    return headers, rows
