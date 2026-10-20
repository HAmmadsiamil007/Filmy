# Project Rules

> **Read this before changing anything.**
> These are the invariants that keep this project working. Most of them exist
> because a specific bug already happened — the "why" is included so you can
> decide when a rule may safely be bent.
>
> AI agents: start here, then read [`AGENTS.md`](AGENTS.md).

---

## 1. Non-negotiable invariants

### Backend (`api.py`)

| # | Rule | Why |
|---|------|-----|
| B1 | **Never branch on raw `subject_type`.** Use the classified `type` from `classify_subject_type()`. | `6/7/8` are Anime / ShortDrama / Promo. The original `if t == 1 then Movie else Series` mislabelled all three, so clients fetched episodes for movies. |
| B2 | **Every upstream request goes through `fetch_with_backoff()`.** | The CDN rate-limits aggressively. A raw 429 surfaces to the user as a random failure. 429 ≠ Referer rejection — never "fix" a 429 by changing headers. |
| B3 | **`/proxy/stream` must keep its SSRF allowlist.** Only `PROXY_ALLOWED_HOST_SUFFIXES` hosts are relayed, and `is_proxyable()` fails **closed**. | Without it, `/proxy` is an open relay that fetches arbitrary URLs from the server (loopback, cloud metadata, internal networks). Widening this list is a security change — justify it. |
| B4 | **Never cache a signed stream URL beyond `CACHE_TTL_STREAM` (120 s), and never persist one.** | Stream URLs carry `&t=<epoch>` and expire in ~120 s. Serving a stale one produces an upstream 403 that looks like a backend bug. |
| B5 | **Never store `None` in the cache.** | `cache_get()` returns `None` for both "miss" and "stored `None`" — they are indistinguishable. Pinned by `test_cache.py`. |
| B6 | **Adult filtering defaults to OFF in results** (`include_adult=false`); clients opt **in**. | ADR-004 — one shared implementation so all four clients behave identically. Flipping the default silently changes every client's results. |
| B7 | **429 handling belongs to `fetch_with_backoff()`, not call sites.** | Duplicated retry logic drifts. |

### Playback & quality

| # | Rule | Why |
|---|------|-----|
| P1 | **Dedupe renditions by `quality_value`, preferring h264.** | Upstream returns a ~0.8 MB hevc DASH fragment mislabelled `format:"MP4"` at `quality_value=1080` next to the real h264 file. Not deduping produces duplicate picker rows, duplicate React keys, and a stream that dies at ~1 s. |
| P2 | **A declared-but-unwired step-up threshold is a bug.** `GOOD_BUFFER_SEC` must be a *step-up condition*, not just a constant. | Declared it, never used it → the player oscillated climb → drain → drop. |
| P3 | **Android adapts on buffer health, never on byte-counting analytics.** Use `Player.bufferedPosition` / `currentPosition`. | `AnalyticsListener.onPlaybackStatsUpdate(EventSender, …)` was a phantom API. Buffer health is downstream of connection speed, so it is a valid adaptive signal without unstable APIs. |
| P4 | **Look up third-party API signatures before using them** (`PlaybackException.errorMessage` does not exist; Android rejects CIDR in `network_security_config.xml`). | Three of these already survived authoring and were caught only by review. |

### Clients

| # | Rule | Why |
|---|------|-----|
| C1 | **The API base URL is runtime-configurable** (`config.ts` / `Config.kt`, stored under `moviebox.apiBase`). Never hard-code a host, and never derive playback URLs from `window.location.origin`. | ADR-005 — all clients are thin and point at a user-supplied backend. Deriving from `location` silently points video at the wrong host once the backend is remote. |
| C2 | **Only two codebases exist:** `web/` (which Tauri wraps) and `android/` (which TV extends). | ADR-003. Do not create a third tree for the desktop or TV targets. |
| C3 | **Web passes `include_adult=true`; do not remove it casually.** | ADR-004 makes the server default filtered; the web client is the explicit opt-in. |

---

## 2. Verification before "done"

**Never claim something works without running the check for it.**

| Change | Required check |
|--------|----------------|
| Any backend change | `.\.venv\Scripts\pytest.exe tests -q` → must exit 0 |
| Backend import safety | `.\.venv\Scripts\python.exe -c "import api; print('IMPORT OK')"` |
| Proxy / `Range` change | Live `206` + `Content-Range` request (unit tests cannot cover this — needs the real upstream) |
| Frontend change | `cd web; npx tsc -b` **and** `npm run build`, both exit 0 |
| Android change | Static sweeps now (package-vs-path, import sweep); `.\gradlew.bat assembleDebug` when a toolchain exists |

Android has **no toolchain** in the authoring environment. Say so explicitly —
an Android change is **uncompiled** until someone runs Gradle.

---

## 3. Git conventions

- **Conventional Commits**: `feat:` / `fix:` / `test:` / `docs:` / `chore:`, with a
  scope where useful (`feat(android): …`).
- **Commit in logical units**, not one blob. The history should read as a plan.
- **`git add --dry-run` before `git add`** on any new directory. This caught a rule
  that would have committed `android/` with *zero* source files (see §5).
- **Do not commit build artifacts**: `*.tsbuildinfo`, `web/dist/`, `node_modules/`,
  `android/**/build/`, `android/local.properties`, `moviebox/`.
- **Never `git add -A` blindly** — verify `git status --short` first.

---

## 4. Documentation & memory sync

After any change that alters behaviour:

1. Update the **Obsidian vault** (`docs/`) — the human-readable source of truth.
2. Update the **Serena memory graph** (`.serena/memories/`) — the agent-readable one.
3. Both must agree. `docs/Guides/Verification.md` § Memory sync defines the loop.

If a rule here changes, update `rules.md` in the same commit as the code.

---

## 5. Hard-won lessons (do not relearn these)

**`.gitignore` has no inline comments.**
```gitignore
*.tsbuildinfo   # tsc cache      ← WRONG: the whole tail becomes part of the pattern
```
```gitignore
# tsc cache
*.tsbuildinfo                     ← correct
```
Both `*.tsbuildinfo` and `android/local.properties` were silently matching
nothing because of a trailing `#` comment.

**Unanchored directory patterns match at any depth.**
```gitignore
moviebox/       ← matched android/app/src/main/java/com/moviebox/app/
/moviebox/      ← repo root only
```
The stray-vault rule was swallowing the entire Android source tree. It was
caught by a dry-run, seconds before committing a project with no code.

**Tests must be executed, not read.** Two of my own test bugs were invisible by
inspection: a missing `pytest.raises()` wrapper, and an `import pytest` placed
below a decorator that needed it.

**Authoring bugs scale with authoring speed.** The Android code review found six
real defects in code that "looked right". Assume the same of anything uncompiled.

---

## 6. Anti-patterns — already rejected here

| Don't | Instead |
|-------|---------|
| Wrap the site in a WebView | Native: Compose + Media3 (`android/`), React + `hls.js` (`web/`) |
| Ship a Python sidecar in the desktop app | Thin client, remote/self-hosted backend (ADR-005) |
| Probe bandwidth with byte counters | Buffer-driven adaptation (P3) |
| Filter adult content in each client | Filter once server-side, opt in per client (B6) |
| Use `curl_cffi` for the stream relay | `httpx` (ADR-002) — `curl_cffi`'s `.stream()` is not a context manager |
| Hard-code `https://…/api` in a client | Runtime base URL + Settings screen (C1) |
| Write `subject_type == 1 ? … : …` | `classify_subject_type()` (B1) |

---

## 7. Known risks this project carries

- **Copyright** — the dominant exposure. The code is MIT; the content is not ours.
- **Adult content** — no age/consent metadata upstream. Compliance is the operator's responsibility.
- **No authentication** — the API is intentionally open. Do not attach private data to it.

Full assessment: `docs/Risks and Compliance.md` and `mem:legal_risks`.
