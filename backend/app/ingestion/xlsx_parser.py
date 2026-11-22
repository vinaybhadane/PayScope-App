"""Read statement tables from every XLSX or legacy XLS worksheet."""
import io
import openpyxl
from app.ingestion.tabular import statement_matrix
from app.ingestion.column_mapper import ColumnMapper


class XLSXParser:
    @staticmethod
    def parse(content):
        collected, headers = [], []
        try:
            if content.startswith(b"\xd0\xcf\x11\xe0"):
                import xlrd
                workbook = xlrd.open_workbook(file_contents=content)
                sheets = []
                for sheet in workbook.sheets():
                    matrix = []
                    for index in range(sheet.nrows):
                        values = []
                        for cell in sheet.row(index):
                            values.append(xlrd.xldate_as_datetime(cell.value, workbook.datemode)
                                          if cell.ctype == xlrd.XL_CELL_DATE else cell.value)
                        matrix.append(values)
                    sheets.append((sheet.name, matrix))
            else:
                workbook = openpyxl.load_workbook(io.BytesIO(content), data_only=True, read_only=True)
                sheets = [(sheet.title, list(sheet.iter_rows(values_only=True))) for sheet in workbook.worksheets]
            try:
                row_offset = 0
                for name, matrix in sheets:
                    found, rows = statement_matrix(matrix, sheet=name)
                    for header in found:
                        if header not in headers:
                            headers.append(header)
                    for row in rows:
                        row["_column_mapping"] = ColumnMapper.auto_detect(found)
                        row["_source_row_number"] = row["_row_number"]
                        row["_row_number"] += row_offset
                    collected.extend(rows)
                    row_offset += len(matrix)
            finally:
                if hasattr(workbook, "close"):
                    workbook.close()
                elif hasattr(workbook, "release_resources"):
                    workbook.release_resources()
        except Exception as exc:
            raise ValueError(f"Failed to read Excel statement: {exc}") from exc
        if not headers:
            raise ValueError("No statement table found. Include Date, Description, and Amount or Debit/Credit headers.")
        return headers, collected
