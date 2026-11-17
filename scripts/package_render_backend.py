"""Export only the standalone backend source; exclude local data and secrets."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "backend"
OUTPUT = ROOT / "output" / "render" / "payscope-backend.zip"
FILES = ["pyproject.toml", "uv.lock", "alembic.ini", "render.yaml", ".python-version",
         ".gitignore", ".env.example", "README.md", "DEPLOY_RENDER.md"]
SOURCE_EXTENSIONS = {".py", ".mako", ".md", ".json", ".sql", ".png", ".yaml", ".yml"}


def build_archive() -> Path:
    entries = [SOURCE / name for name in FILES]
    for directory in ("app", "alembic", "checks", "tests"):
        entries.extend(p for p in (SOURCE / directory).rglob("*") if p.is_file()
            and p.suffix in SOURCE_EXTENSIONS and "__pycache__" not in p.parts
            and not any(part.startswith(".env") for part in p.parts))
    for path in entries:
        if not path.is_file():
            raise FileNotFoundError(f"Required deployment file is missing: {path.name}")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(OUTPUT, "w", ZIP_DEFLATED) as archive:
        for path in sorted(set(entries)):
            archive.write(path, path.relative_to(SOURCE).as_posix())
    with ZipFile(OUTPUT) as archive:
        if archive.testzip() is not None:
            raise RuntimeError("Deployment archive verification failed.")
        if any(name.endswith("/.env") or name == ".env" or ".venv/" in name for name in archive.namelist()):
            raise RuntimeError("Deployment archive includes a forbidden local file.")
    return OUTPUT


if __name__ == "__main__":
    print(build_archive())
