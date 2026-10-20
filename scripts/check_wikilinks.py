#!/usr/bin/env python3
"""Validate documentation links.

Checks two link forms across every Markdown file in the repo:

1. Obsidian wikilinks   ``[[Note]]``, ``[[Folder/Note]]``, ``[[Note|alias]]``,
                        ``[[Note#heading]]``   -> the target note must exist.
2. Relative Markdown    ``[text](path)`` and ``[text](<path with spaces>)``
                        -> the target file or directory must exist.

External (http/https/mailto), anchors-only (``#frag``) and absolute paths are
skipped. This is the automated version of the manual wikilink sweep that
previously lived in PowerShell; CI runs it, and so should you:

    python scripts/check_wikilinks.py            # exit 0 = no broken links
    python scripts/check_wikilinks.py --verbose  # also list every link found

Exit codes: 0 = clean, 1 = broken links found, 2 = nothing to check.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

# Roots that contain documentation. Markdown anywhere else (web/dist,
# node_modules, .venv, build outputs, moviebox/) is deliberately ignored.
DOC_ROOTS = (REPO_ROOT / "docs", REPO_ROOT)

SKIP_DIR_NAMES = {
    "node_modules",
    "web",
    "dist",
    "build",
    ".venv",
    "venv",
    "__pycache__",
    ".git",
    ".pytest_cache",
    "target",
    "moviebox",       # stray local vault, gitignored
    ".gradle",
}

WIKILINK_RE = re.compile(r"\[\[([^\[\]\n]+)\]\]")
# Markdown link/image: [text](target) or ![alt](target). `<...>` allows spaces.
MDLINK_RE = re.compile(r"!?\[[^\]\n]*\]\(\s*<?([^)>\s]+)>?\s*\)")

# Prose that *mentions* link syntax is not a link. Fenced blocks and inline
# spans are blanked before scanning so `[[wikilink]]` in a sentence like
# "link with [[wikilinks]]" is not reported as a missing note.
FENCED_RE = re.compile(r"```.*?```", re.S)
INLINE_CODE_RE = re.compile(r"`[^`\n]*`")


def collect_markdown() -> list[Path]:
    files: list[Path] = []
    for path in REPO_ROOT.rglob("*.md"):
        rel_parts = path.relative_to(REPO_ROOT).parts
        if any(part in SKIP_DIR_NAMES for part in rel_parts):
            continue
        files.append(path)
    return sorted(files)


def build_targets(files: list[Path]) -> tuple[set[str], set[str]]:
    """Return (note-names, relative-paths) that a link may resolve to."""
    notes: set[str] = set()
    paths: set[str] = set()
    for f in files:
        rel = f.relative_to(REPO_ROOT)
        as_posix = rel.as_posix()
        notes.add(f.stem)                                  # "Status"
        notes.add(as_posix)                                # "docs/Project/Status.md"
        paths.add(as_posix)                                # "docs/Project/Status.md"
        paths.add(rel.with_suffix("").as_posix())          # "docs/Project/Status"
        # Obsidian also resolves "Project/Status" style targets
        if rel.parts[0] == "docs":
            trimmed = Path(*rel.parts[1:]).as_posix()
            notes.add(trimmed)
            notes.add(Path(trimmed).with_suffix("").as_posix())
        paths.add(as_posix)                                # keep trailing too
    return notes, paths


def clean_wikilink(raw: str) -> str:
    """Strip alias and heading, then normalise separators."""
    target = raw.split("|", 1)[0]
    target = target.split("#", 1)[0]
    target = target.split("^", 1)[0]      # block refs
    return target.strip().replace("\\", "/").removeprefix("docs/")


def is_external(target: str) -> bool:
    return bool(re.match(r"^(https?:|mailto:|tel:|ftp:)", target, re.I))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verbose", "-v", action="store_true",
                        help="print every link found, not just broken ones")
    args = parser.parse_args()

    files = collect_markdown()
    if not files:
        print("check_wikilinks: no markdown files found", file=sys.stderr)
        return 2

    notes, paths = build_targets(files)

    broken: list[str] = []
    total = 0
    shown: list[str] = []

    for f in files:
        rel = f.relative_to(REPO_ROOT).as_posix()
        try:
            text = f.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            print(f"  !! could not decode {rel}", file=sys.stderr)
            broken.append(f"{rel} -> <undecodable file>")
            continue

        # Blank out code so syntax examples are never treated as real links.
        scannable = FENCED_RE.sub(" ", text)
        scannable = INLINE_CODE_RE.sub(" ", scannable)

        for match in WIKILINK_RE.finditer(scannable):
            total += 1
            raw = match.group(1)
            target = clean_wikilink(raw)
            shown.append(f"{rel} -> [[{raw}]]")
            if not target:                     # [[]]
                broken.append(f"{rel} -> [[{raw}]] (empty target)")
            elif target not in notes:
                broken.append(f"{rel} -> [[{raw}]] (no such note)")

        for match in MDLINK_RE.finditer(scannable):
            target = match.group(1)
            if is_external(target) or target.startswith("#"):
                continue
            total += 1
            shown.append(f"{rel} -> ({target})")
            # Anchors on internal links: strip "#fragment"
            bare = target.split("#", 1)[0]
            if not bare:
                continue
            resolved = (f.parent / bare).resolve()
            try:
                resolved.relative_to(REPO_ROOT)
            except ValueError:
                broken.append(f"{rel} -> ({target}) (escapes repo root)")
                continue
            if not resolved.exists():
                broken.append(f"{rel} -> ({target}) (missing file)")

    if args.verbose:
        for line in shown:
            print(f"  {line}")

    print(f"markdown files : {len(files)}")
    print(f"links checked  : {total}")
    print(f"broken         : {len(broken)}")
    sys.stdout.flush()   # keep the summary ahead of the stderr block below

    if broken:
        print("\nBROKEN LINKS:", file=sys.stderr)
        for line in broken:
            print(f"  {line}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
