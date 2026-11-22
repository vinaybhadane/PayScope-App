"""Extract statement tables and fallback to raw text & OCR for unstructured or scanned PDFs."""
import asyncio
import io
import logging
import re
import math
from typing import Any, Dict, List, Tuple
import pdfplumber

from app.ingestion.column_mapper import ColumnMapper
from app.ingestion.raw_text_parser import RawTextStatementParser, statement_amount_columns
from app.ingestion.ocr_security import parse_ocr_result

OCR_DPI = 200
MAX_RENDER_PIXELS = 15_000_000
MAX_DOCUMENT_RENDER_PIXELS = 60_000_000


def page_render_pixels(page):
    # pdfplumber exposes width/height in points; other renderers expose page.rect.
    rect = getattr(page, "rect", None)
    width = rect.width if rect is not None else page.width
    height = rect.height if rect is not None else page.height
    width, height = float(width), float(height)
    # Cropping must not hide an enormous backing page. Some renderers allocate
    # the full MediaBox before cropping, and UserUnit scales PDF point units.
    for name in ("mediabox", "cropbox", "bbox"):
        box = getattr(page, name, None)
        if isinstance(box, (tuple, list)) and len(box) == 4:
            width = max(width, abs(float(box[2]) - float(box[0])))
            height = max(height, abs(float(box[3]) - float(box[1])))
    page_object = getattr(page, "page_obj", None)
    attributes = getattr(page_object, "attrs", {})
    unit = float(attributes.get("UserUnit", 1)) if isinstance(attributes, dict) else 1
    if not math.isfinite(unit) or unit <= 0:
        raise ValueError("PDF page has invalid scale units.")
    width *= max(1, unit)
    height *= max(1, unit)
    if not math.isfinite(width) or not math.isfinite(height) or width <= 0 or height <= 0:
        raise ValueError("PDF page has invalid dimensions.")
    pixels = math.ceil(width * OCR_DPI / 72) * math.ceil(height * OCR_DPI / 72)
    if pixels > MAX_RENDER_PIXELS:
        raise ValueError("PDF page exceeds the 15,000,000 pixel rendering safety limit at 200 DPI.")
    return pixels

logger = logging.getLogger(__name__)


class PDFParser:
    @staticmethod
    def parse(content: bytes) -> Tuple[List[str], List[Dict[str, Any]]]:
        if not content.startswith(b"%PDF-"):
            raise ValueError("This file is not a valid PDF.")

        headers: List[str] = []
        rows: List[Dict[str, Any]] = []

        with pdfplumber.open(io.BytesIO(content)) as document:
            if len(document.pages) > 100:
                raise ValueError("PDF statements must contain at most 100 pages.")
            for page in document.pages:
                page_render_pixels(page)

            from app.ingestion.tabular import statement_matrix
            from app.ingestion.ocr_engine import recognize
            total_pixels = 0
            scanned_pages = 0
            carried_headers = []
            amount_columns = ()
            for page_number, page in enumerate(document.pages, 1):
                page_rows, page_headers = [], []
                tables = page.extract_tables()
                if not tables:
                    tables = page.extract_tables({"vertical_strategy": "text", "horizontal_strategy": "text"})
                for table in tables:
                    if not isinstance(table, list):
                        continue
                    matrix = [[re.sub(r"\s+", " ", str(cell or "")).strip() for cell in cells] for cells in table]
                    # A continuation page may repeat no header at all.
                    candidate = matrix
                    if carried_headers and matrix and not any(not ColumnMapper.validate_mapping(ColumnMapper.auto_detect(cells), cells) for cells in matrix):
                        candidate = [carried_headers, *matrix]
                    found, extracted = statement_matrix(candidate)
                    if extracted:
                        page_headers = found
                        page_rows.extend(extracted)
                        carried_headers = found
                if not page_rows:
                    raw = page.extract_text() or ""
                    if isinstance(raw, str) and raw.strip():
                        page_headers, page_rows = RawTextStatementParser.parse(raw, initial_columns=amount_columns)
                # Process every page independently, including image pages following
                # selectable-text pages. Never publish only the first readable page.
                if not page_rows:
                    raw = page.extract_text() or ""
                    if isinstance(raw, str) and raw.strip():
                        # No dated records on this readable page: summary or cover.
                        continue
                    scanned_pages += 1
                    if scanned_pages > 15:
                        raise ValueError("Scanned PDFs must contain at most 15 scanned pages; split the document.")
                    total_pixels += page_render_pixels(page)
                    if total_pixels > MAX_DOCUMENT_RENDER_PIXELS:
                        raise ValueError("Scanned PDF exceeds the rendering pixel budget; split the document.")
                    image = page.to_image(resolution=OCR_DPI).original
                    if image is None:
                        raise ValueError(f"No readable statement transactions found on PDF page {page_number}.")
                    try:
                        page_headers, page_rows = parse_ocr_result(recognize(image), initial_columns=amount_columns)
                    finally:
                        image.close()
                    if not page_rows:
                        raise ValueError(f"No readable statement transactions found on scanned page {page_number}. Upload a clearer scan or CSV/Excel.")
                mapping = ColumnMapper.auto_detect(page_headers)
                amount_columns = page_rows[-1].get("_amount_columns")
                if amount_columns is None:
                    amount_columns = statement_amount_columns(" ".join(page_headers)) or ()
                for header in page_headers:
                    if header not in headers:
                        headers.append(header)
                for row in page_rows:
                    row["_column_mapping"] = mapping
                    row["_page"] = page_number
                    row["_source_row_number"] = row["_row_number"]
                    row["_row_number"] = len(rows) + 1
                    rows.append(row)
        if not rows:
            raise ValueError("No readable statement transactions found in this PDF. Include dates, descriptions and amounts.")
        return headers, rows
