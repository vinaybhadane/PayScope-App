"""Use the host OCR engine; Tesseract provides Linux/macOS/Windows support."""
import asyncio
from types import SimpleNamespace


def recognize(image):
    try:
        import winocr
        operation = winocr.recognize_pil(image, lang="en")
        if hasattr(operation, "get"):
            return operation.get()
        if asyncio.iscoroutine(operation):
            return asyncio.run(operation)
        return operation
    except (ImportError, OSError, RuntimeError):
        pass
    try:
        import pytesseract
        from PIL import ImageOps
        prepared = ImageOps.autocontrast(ImageOps.grayscale(ImageOps.exif_transpose(image)))
        data = pytesseract.image_to_data(prepared, config="--psm 6", output_type=pytesseract.Output.DICT, timeout=90)
        groups = {}
        for index, word in enumerate(data["text"]):
            if not word.strip():
                continue
            key = (data["block_num"][index], data["par_num"][index], data["line_num"][index])
            groups.setdefault(key, []).append(SimpleNamespace(text=word, confidence=float(data["conf"][index])))
        lines = [SimpleNamespace(text=" ".join(word.text for word in words), words=words) for words in groups.values()]
        prepared.close()
        return SimpleNamespace(text="\n".join(line.text for line in lines), lines=lines)
    except Exception as exc:
        raise ValueError("OCR is unavailable. Install Tesseract with English language data on the API host, or upload a selectable-text PDF, CSV or Excel statement.") from exc
