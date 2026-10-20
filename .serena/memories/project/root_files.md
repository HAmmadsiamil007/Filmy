# Root project files

The repo root carries the standard "pro" project files. Created 2026-10-20.
They are the ENTRY POINT; docs/ is the depth. Do not duplicate content between
them - root files summarize and link into the vault.

| File | Role | Notes |
|------|------|-------|
| `rules.md` | THE invariants file. 7 sections: backend rules (B1-B7), playback (P1-P4), clients (C1-C3), verification matrix, git rules, docs sync, hard-won lessons, anti-patterns, risks. | Read before ANY change. Every rule carries a WHY (which past bug it exists for). |
| `AGENTS.md` | AI-agent entry point. Auto-read by the harness (confirmed: it is picked up as system instructions). | Deliberately short: orientation + pointers. Real rules in rules.md. |
| `architecture.md` | One-page system map: mermaid diagram, 2 request paths (metadata/cached vs playback/short-lived), api.py region table, client layout, trust boundary diagram. | Links to ADR-001..005 rather than restating them. |
| `design.md` | Design system: real tokens from styles.css + Color.kt, type scale, spacing, surfaces, component specs, motion, responsive, a11y checklist, platform-parity table. | Documents KNOWN DIVERGENCES (error red web #FF8A8A vs Android #EF4444). |
| `CONTRIBUTING.md` | REWRITTEN. Old one had mojibake emoji and referenced python verify.py (deleted in commit 12f70ae). | Setup, verify-before-done matrix, PR/commit style, standards. |
| `CHANGELOG.md` | Keep a Changelog format. **Unreleased** = the Filmy rename (identifiers + settings-key migrations); 2.5.0 = backend/web work; 2.0.0 = upstream. | Pre-2.0.0 history stays in git log. |
| `SECURITY.md` | Threat model: the SSRF boundary, in/out-of-scope reporting, deliberately-absent controls table, client-side notes. | Do NOT open public issues for vulns. |
| `.editorconfig` | indent/charset/EOL per language. md keeps trailing whitespace (hard line breaks). | |
| `.env.example` | Documents that the backend reads NO env vars (by design, ADR-005). Only $PORT, consumed by the SHELL. | Prevents hunting for config that does not exist. |
| `readme.md` | REWRITTEN for 2.5.0: status table, quickstart, API reference, design highlights. | Was stale at v2.0.0. |
| `.github/workflows/ci.yml` | CI: 3 jobs (pytest, tsc+vite build, link check). | UNVERIFIED - never executed here (no GitHub Actions locally). Every command inside it WAS run locally. |

## Intentionally NOT created
- CODE_OF_CONDUCT.md - covered by CONTRIBUTING.md section 9.
- .nvmrc / .python-version - single dev, documented in readme instead.
- Tauri/Android CI jobs - Tauri needs Rust; Android has never compiled and
  would make CI permanently red. Enable Android AFTER first green gradlew build.

## Relationship to the vault
- rules/architecture/design are OVERVIEWS that link into docs/.
- docs/ holds the depth (24 notes, ADRs, guides).
- scripts/check_wikilinks.py enforces that links between them resolve.