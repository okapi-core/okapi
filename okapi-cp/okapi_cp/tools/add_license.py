# Copyright The OkapiCore Authors
# SPDX-License-Identifier: Apache-2.0

"""Apply SPDX license headers to okapi-cp Python sources."""

from __future__ import annotations

from pathlib import Path

HEADER_LINES = [
    "# Copyright The OkapiCore Authors",
    "# SPDX-License-Identifier: Apache-2.0",
]

SKIP_DIRS = {
    ".git",
    ".venv",
    "__pycache__",
    "dist",
    "build",
}


def _has_header(text: str) -> bool:
    first_lines = text.splitlines()[:5]
    return any("SPDX-License-Identifier: Apache-2.0" in line for line in first_lines)


def _apply_header_to_text(text: str) -> str:
    if _has_header(text):
        return text

    header = "\n".join(HEADER_LINES) + "\n\n"
    if text.startswith("#!"):
        lines = text.splitlines()
        if len(lines) == 1:
            return lines[0] + "\n" + header
        return lines[0] + "\n" + header + "\n".join(lines[1:]) + "\n"
    return header + text


def apply_headers(root: Path) -> list[Path]:
    updated: list[Path] = []
    for path in root.rglob("*.py"):
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        text = path.read_text(encoding="utf-8")
        new_text = _apply_header_to_text(text)
        if new_text != text:
            path.write_text(new_text, encoding="utf-8")
            updated.append(path)
    return updated


def main() -> None:
    project_root = Path(__file__).resolve().parents[2]
    updated = apply_headers(project_root)
    print(f"Updated {len(updated)} file(s).")
