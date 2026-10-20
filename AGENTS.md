# Agent Instructions

> **AGENTS.md** — the entry point AI coding tools read automatically.
> This file is short on purpose: orientation, then pointers. The real rules are
> in [`rules.md`](rules.md).

---

## What this is

Filmy - a scraper proxy for the MovieBox catalog, plus three thin clients.

- **Backend** `api.py` — single-file FastAPI app, 13 routes, a stream proxy, a TTL cache.
- **Clients** `web/` (React + `hls.js`; Tauri wraps it) and `android/` (Kotlin + Compose + Media3; TV will extend it).

Content is piracy-adjacent and includes unfiltered adult titles by design —
read [`docs/Risks and Compliance.md`](<docs/Risks and Compliance.md>) before
broadening distribution.

---

## Read in this order

| # | File | Why |
|---|------|-----|
| 1 | [`rules.md`](rules.md) | Invariants that break things if violated, with the reason each exists |
| 2 | [`architecture.md`](architecture.md) | One-page system map + the two request paths |
| 3 | [`design.md`](design.md) | Tokens shared by `styles.css` and `Color.kt` |
| 4 | [`docs/Project/Status.md`](<docs/Project/Status.md>) | What is actually verified vs merely written |
| 5 | [`CONTRIBUTING.md`](CONTRIBUTING.md) | Verify-then-commit workflow |

---

## Commands that gate every change

```powershell
# backend — 129 tests, no network, ~1.5s
.\.venv\Scripts\pytest.exe tests -q

# frontend
cd web; npx tsc -b; npm run build

# wikilink integrity (docs/)
python scripts\check_wikilinks.py
```

All three must exit 0 before you call work "done". Full matrix:
[`docs/Guides/Verification.md`](<docs/Guides/Verification.md>).

---

## Before you write code

- **Search before you build.** This repo already solved many problems — grep it.
- **Check `docs/Project/Status.md`** so you don't re-implement something marked done.
- **Look up third-party APIs** rather than recalling them. Several guessed symbols
  already shipped and were caught only by review (`PlaybackException.errorMessage`,
  an `AnalyticsListener` overload, CIDR entries Android rejects).
- **Keep the two clients in sync.** A contract change in `api.ts` implies the same
  change in `Api.kt`, and vice versa.

---

## Constraints of this environment

| Toolchain | Available? | Consequence |
|-----------|-----------|-------------|
| Python + pytest | ✅ yes | backend changes are verifiable |
| Node + `tsc` + Vite | ✅ yes | web changes are verifiable |
| Rust / MSVC | ❌ no | Tauri cannot be built here |
| Android SDK / Gradle | ❌ no | **Android code cannot be compiled here** |

When touching `android/`, state explicitly that the change is **uncompiled** and
run the static checks instead (package-vs-path match, import sweep). Do not
report Android work as passing until Gradle has actually run.

---

## Memory & docs sync

If you change behaviour, update **both**:

1. **Obsidian vault** `docs/` — human source of truth (24 notes, wikilinked).
2. **Serena graph** `.serena/memories/` — agent source of truth (root `mem:core`).

They must agree. `docs/Guides/Verification.md` § Memory sync defines the loop.

---

## Style

- Conventional Commits: `feat:` `fix:` `test:` `docs:` `chore:` with a scope.
- Match the surrounding file. Prefer surgical edits over rewrites.
- Comments explain **why**, not what. No decorative comments on obvious code.
- No new dependencies without asking.
- Never commit build artifacts — see the gitignore list in `rules.md` §3.
