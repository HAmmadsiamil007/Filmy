---
tags: [reference, project-files, root]
status: active
updated: 2026-10-20
---

# Project Files

> [!info] Every file at the repo root and why it exists
> Root files are **overviews that link into the vault**, never duplicates of it.
> Depth lives in [[Project/Status]], [[Architecture/Backend]] and the ADRs.

---

## The three requested documents

| File | Purpose |
| --- | --- |
| [[rules.md]] | The invariants file — read before changing anything |
| [[architecture.md]] | One-page system map + both request paths |
| [[design.md]] | Design tokens shared by web and Android |

## Agent & process files

| File | Purpose |
| --- | --- |
| [[AGENTS.md]] | Entry point AI tools auto-read. Short by design — points at `rules.md` |
| [[CONTRIBUTING.md]] | Setup, verify-before-done matrix, commit style (rewritten — had mojibake + a dead `verify.py` reference) |
| [[SECURITY.md]] | Threat model, the SSRF boundary, reporting policy |
| [[CHANGELOG.md]] | Keep a Changelog — 2.5.0 covers the 2026-10-20 work |

## Configuration

| File | Purpose |
| --- | --- |
| `.editorconfig` | Indent/charset/EOL per language; Markdown keeps trailing spaces (hard line breaks) |
| `.env.example` | Documents that the backend reads **no** env vars — see [[Decisions/ADR-005 Remote self-hosted backend]] |
| `.gitignore` | Node/Android/TS artifacts + anchored `/moviebox/` — see [[Guides/Git Workflow]] |
| `.github/workflows/ci.yml` | Three jobs: pytest · tsc + vite build · link check |

## Application

| File | Purpose |
| --- | --- |
| `api.py` | The backend — see [[Architecture/Backend]] |
| `main.py` | Thin uvicorn entrypoint (used by Vercel's `functions` config) |
| `requirements.txt` | 4 runtime deps |
| `requirements-dev.txt` | pytest only — test deps kept separate |
| `Procfile` / `railway.json` / `vercel.json` | Deploy targets; `$PORT` is read by the **shell**, not Python |

---

## Deliberately not created

| File | Why not |
| --- | --- |
| `CODE_OF_CONDUCT.md` | Covered by `CONTRIBUTING.md` §9 |
| `.nvmrc` / `.python-version` | Single-dev project; versions documented in `readme.md` and CI |
| Tauri CI job | Needs Rust + MSVC — not installed anywhere in this project |
| Android CI job | The sources have **never compiled**; enabling it would make CI permanently red. Turn it on after the first green `gradlew assembleDebug` |

---

## Enforcement

`scripts/check_wikilinks.py` validates every `[[wikilink]]` **and** every
relative markdown link across all 49 Markdown files. Prose mentions like
`` `[[wikilinks]]` `` are ignored on purpose.

```powershell
python scripts\check_wikilinks.py           # exit 0 = clean
python scripts\check_wikilinks.py --verbose # list every link
```

Run it in CI (done), and locally before claiming a docs change is done —
see [[Guides/Verification]] § Link integrity.

---

## Related

- [[Guides/Git Workflow]] — the two gitignore bugs caught while committing these
- [[Guides/Verification]] — the full pre-done gate
- [[Reference/Commands]] — command cheatsheet
- [[Project/Status]] — what is verified vs merely written
