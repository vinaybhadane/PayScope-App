"""Image statement parser with OCR support for statement screenshots, scans, and photos."""
import asyncio
import io
import logging
import warnings
from typing import Any, Dict, List, Tuple
from PIL import Image
from app.ingestion.ocr_security import parse_ocr_result

Image.MAX_IMAGE_PIXELS = 15_000_000
# Pillow warns at MAX_IMAGE_PIXELS and errors only at twice the cap by default.
warnings.filterwarnings("error", category=Image.DecompressionBombWarning)

logger = logging.getLogger(__name__)


class ImageStatementParser:
    """Parses image statements (PNG, JPG, JPEG, WEBP) using native OCR and raw text extraction."""

    @classmethod
    def parse(cls, content: bytes, filename: str = "image.png") -> Tuple[List[str], List[Dict[str, Any]]]:
        try:
            pil_image = Image.open(io.BytesIO(content), formats=("PNG", "JPEG", "WEBP"))
            if pil_image.width * pil_image.height > Image.MAX_IMAGE_PIXELS:
                raise ValueError("Image exceeds the 15,000,000 pixel safety limit.")
            # Convert RGBA/P to RGB
            if pil_image.mode in ("RGBA", "P"):
                pil_image = pil_image.convert("RGB")
        except (Image.DecompressionBombWarning, Image.DecompressionBombError) as e:
            raise ValueError("Image exceeds the 15,000,000 pixel safety limit.") from e
        except Exception as e:
            raise ValueError(f"Invalid image file '{filename}': {str(e)}")

        rows = []
        try:
            from app.ingestion.ocr_engine import recognize
            ocr_result = recognize(pil_image)

            if ocr_result:
                headers, rows = parse_ocr_result(ocr_result)
        except ValueError:
            raise
        except Exception as ocr_err:
            logger.warning(f"Native Windows OCR error for '{filename}': {ocr_err}")
        finally:
            pil_image.close()

        if not rows:
            raise ValueError(
                f"No readable text could be recognized in the image '{filename}'. "
                "Please upload a clearer screenshot, PDF, or CSV export."
            )

        return headers, rows
