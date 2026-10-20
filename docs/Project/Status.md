---
tags: [project, status]
status: active
updated: 2026-10-20
---

# Status Board

> [!success] Backend + Web are DONE
> Everything below was verified against the live server, not just written.

## ✅ Repo hygiene — project files + CI (2026-10-20)

The repo now carries the standard project files, **all of them linked rather than duplicated** — details in [[Reference/Project Files]].

| Added | Purpose |
| --- | --- |
| `rules.md` | Invariants with the bug each one exists for |
| `architecture.md` | One-page system map + both request paths |
| `design.md` | Shared design tokens, incl. known web↔Android divergences |
| `AGENTS.md` | AI-agent entry point (auto-read by tooling) |
| `CONTRIBUTING.md` | **Rewritten** — had mojibake emoji and a dead `verify.py` reference |
| `SECURITY.md` | Threat model + the SSRF boundary |
| `CHANGELOG.md` | Keep a Changelog, 2.5.0 |
| `.editorconfig` · `.env.example` | Formatting; documents that no env vars are read |
| `.github/workflows/ci.yml` | pytest · tsc+vite · link check on every push/PR |
| `scripts/check_wikilinks.py` | 53 files, 219 links, 0 broken |

> [!warning] CI is written, not executed
> GitHub Actions cannot run from here. Every command *inside* the workflow was
> run locally and is green — the workflow YAML itself is unverified.

### Two gitignore bugs caught before committing

1. **Inline `#` comments aren't supported** — `*.tsbuildinfo   # note` made the whole tail part of the pattern, so it matched nothing.
2. **Unanchored `moviebox/` matched `com/moviebox/app/`** — it was silently swallowing the entire Android source tree. A dry-run showed 27 files with **zero `.kt` files**; committing would have shipped `android/` with no code.

Full detail + the rules: [[Guides/Git Workflow]].

### Git state

- **9 commits local** on `main`, **not pushed** (decision: 2026-10-20).
- Serena memory graph: **20 memories** (root `mem:core`).

## ✅ Backend — complete (2026-10-20)

Five blockers fixed in `api.py`:

| # | Fix | Verification |
| --- | --- | --- |
| 1 | `/proxy/stream` — Referer-locked relay w/ Range | `206`, `Content-Range: bytes 0-2047/5184360316` ✅ |
| 2 | `classify_subject_type()` — real labels | `Movie / Series / Anime / ShortDrama / Promo` ✅ |
| 3 | `strip_adult()` — genre filter | `porn` → **0** default, **20** with `include_adult=true` ✅ |
| 4 | TTL cache + 429 backoff | cold **906ms** → warm **32ms** ✅ |
| 5 | `/health` | `{"status":"ok","version":"2.5.0",…}` ✅ |

Details: [[Architecture/Backend]], [[Architecture/Stream Proxy]].

## ✅ Web frontend — complete (2026-10-20)

Live at **http://127.0.0.1:5173** (Vite dev server → backend on 8000).

| Check | Result |
| --- | --- |
| App HTML | `200` ✅ |
| Search via Vite proxy | `200`, 20 results ✅ |
| Video bytes, full chain | `206` ✅ |
| `tsc -b` | exit 0 ✅ |
| `vite build` | 33 modules, 1.4s, 150 kB / 48 kB gz ✅ |

Quality picker: `Auto · 1080p` with manual override to 720/480/360. Logic: [[Architecture/Adaptive Quality]].

## ✅ Backend unit tests — 129 passing (2026-10-20)

Zero coverage → **129 tests, exit 0, ~1.6 s**. Pure-logic + in-process endpoint checks; no network.

| File | Covers |
| --- | --- |
| `test_proxy_security.py` | `is_proxyable` — the SSRF allowlist (lookalikes, loopback, metadata IP, fail-closed) |
| `test_classify_subject_type.py` | taxonomy + fallback quirks |
| `test_adult_filter.py` | `is_adult` / `strip_adult`, incl. non-string + falsy inputs |
| `test_cache.py` | TTL expiry, falsy-value hits, blunt 2048 eviction |
| `test_backoff.py` | 429-only retry, exponential cap, propagation |
| `test_endpoints.py` | `/health`, `/proxy`, `/proxy/stream` 400/403 guards |

```powershell
.\.venv\Scripts\pytest.exe tests     # 129 passed, exit 0
```

> [!note] Two things found only by running
> 1. My own first draft of `test_backoff_is_capped_at_four_seconds` was missing its `pytest.raises(...)` wrapper — the exception escaped and pytest caught it. Authoring tests has the same failure mode as authoring code.
> 2. A bottom-of-file `import pytest` in `test_cache.py` would have raised `NameError`, since decorators evaluate at class-definition time.

**Still open:** the proxy `Range` integration test (needs live upstream) → [[Roadmap/Backlog]].

## 🐛 Bugs caught during build

> [!warning] All three were found by verification, not by reading code
> 1. **`GOOD_BUFFER_SEC` declared but unused** — `tsc` flagged it. The step-**up** gate was never wired → player oscillated. Wired it in rather than deleting.
> 2. **Duplicate `1080p` in picker** — upstream returns a 0.8 MB `hevc` DASH fragment *mislabelled* `format:"MP4"` beside the real 4.9 GB `h264` file, both `quality_value=1080`. Caused duplicate labels, duplicate React keys, and a stream dying at ~1s. Fixed via dedupe + h264 preference.
> 3. **`curl_cffi` `.stream()` is a generator, not a context manager** — `__enter__()` threw → misleading `502`. See [[Decisions/ADR-002 httpx over curl_cffi for streaming]].

## ⬜ Tauri desktop — SCAFFOLDED, blocked ONLY on Rust

→ [[Roadmap/Tauri Desktop]]

**Done 2026-10-20:** `web/src-tauri/` written (thin shell, no sidecar), `@tauri-apps/cli` 2.12.3 installed, config JSON validated, CSP set for a remote API base.

**✅ Icons resolved 2026-10-20:** source generated at `web/src-tauri/app-icon.png` (brand-matched: `#0b0d12` bg, `#e50914` tile, white play triangle), then `npm run tauri:icons` produced the full set. All 5 files referenced by `bundle.icon` verified present (`32x32`, `128x128`, `128x128@2x`, `icon.ico`, `icon.png`) — plus icns, AppStore and iOS assets. Regenerate with `scripts/gen-icon.ps1` → `npm run tauri:icons`.

**Still blocked on you (one thing):**
1. **Rust + MSVC Build Tools** — not installed (~2 GB+, admin, possible reboot). `tauri dev`/`build` cannot run until then.

Backend hosting resolved → **remote/self-hosted**, see [[Decisions/ADR-005 Remote self-hosted backend]] and [[Architecture/Runtime Config]].

## 🟡 Android phone APK — WRITTEN, NEVER COMPILED

→ [[Roadmap/Android]] · full setup in `android/README.md`

`android/` written 2026-10-20: **14 Kotlin files (~88 KB)**, Gradle config, resources + generated icons, **17 unit tests**. Mirrors `api.ts`/`Player.tsx` contract-for-contract.

> [!warning] Not verified — no toolchain
> No Android SDK / Gradle / `ANDROID_HOME` in this environment (checked 2026-10-20). Code was **hand-reviewed, not compiled**. First `assembleDebug` will likely surface a few compile errors.

**Verification performed without a compiler:** 9/9 adaptive-logic assertions traced against an independent reimplementation · every package declaration matches its path · import sweep clean across all `.kt`.

**6 real bugs caught by review and fixed:** non-existent `PlaybackException.errorMessage`; phantom `AnalyticsListener` signature (replaced by buffer-driven adaptation); invalid CIDR in `network_security_config.xml`; missing imports + unresolved `Color_Error`; invalid method-chain syntax; duplicated dead `selectPlayable()`.

**Blocked on:** Android SDK (API 35) + Gradle 8.9. Recommended: open `android/` in Android Studio.

Android TV variant — not started.

## 🗄️ Memory sync

Serena memory graph is written (**20 memories**, root `mem:core`). This vault mirrors it for humans. After any backend/player change, update **both** — see [[Guides/Verification]] § Memory sync.
