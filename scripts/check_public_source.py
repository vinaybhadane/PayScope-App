"""Scan outgoing Git history for credentials without printing matched values."""
import argparse
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PATTERNS = {
    "Google API key": re.compile(rb"AIza[0-9A-Za-z_-]{35}"),
    "GitHub access token": re.compile(rb"\b(?:ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,})"),
    "private key": re.compile(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "OpenAI secret": re.compile(rb"\bsk-proj-[A-Za-z0-9_-]{30,}"),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ref", default="HEAD")
    args = parser.parse_args()
    listing = subprocess.check_output(["git", "rev-list", "--objects", args.ref, "--"], cwd=ROOT, text=True)
    objects = {}
    for line in listing.splitlines():
        sha, _, name = line.partition(" ")
        objects[sha] = name
    result = subprocess.run(["git", "cat-file", "--batch"], cwd=ROOT,
                            input=("\n".join(objects) + "\n").encode(), capture_output=True, check=True).stdout
    offset, issues, blobs = 0, set(), 0
    while offset < len(result):
        end = result.index(b"\n", offset)
        sha, kind, length = result[offset:end].decode().split()
        offset = end + 1
        content = result[offset:offset + int(length)]
        offset += int(length) + 1
        if kind != "blob":
            continue
        blobs += 1
        path = objects[sha]
        name = Path(path).name
        if (name.startswith(".env") and name != ".env.example") or name in {"firebase.local.properties", "local.properties"} or name.startswith("google-services") and name.endswith(".json"):
            issues.add((path, "local configuration file"))
        for label, pattern in PATTERNS.items():
            if pattern.search(content):
                issues.add((path, label))
    if issues:
        for path, label in sorted(issues):
            print(f"BLOCKED: {label} in {path}")
        raise SystemExit(1)
    print(f"Public-source check passed: {blobs} unique blobs across outgoing history.")


if __name__ == "__main__":
    main()
