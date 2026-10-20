# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning: [Semantic Versioning](https://semver.org/).

Pre-2.0.0 history is in `git log` (`4289bc6 feat: v2.0.0 rewrite…`).

---

## [2.5.0] — 2026-10-20

### Added — backend

- **`/proxy/stream`** — relays Referer-locked CDN bytes with `Range` support so
  seeking works in `<video>`, ExoPlayer and the Tauri webview alike.
  Guarded by a host allowlist so it cannot become an open SSRF relay.
- **`/proxy`** — self-describing usage + allowlist.
- **`/health`** — startup probe for all four clients.
- **`classify_subject_type()`** — maps `1/2/3/6/7/8` to `Movie / Series / Anime /
  ShortDrama / Promo`; the previous one-liner mislabelled the last three.
- **`is_adult()` / `strip_adult()`** — server-side adult filtering with
  `include_adult=true` as the opt-in (ADR-004).
- **TTL cache** — metadata 900 s · stream 120 s · suggest 600 s. Cold 906 ms →
  warm 32 ms.
- **`fetch_with_backoff()`** — exponential retry with jitter for the CDN's 429s.

### Added — web client (`web/`)

- React + Vite client: search, detail, episodes, catalog, player.
- **Adaptive quality** — buffer-driven auto mode plus a manual
  1080/720/480/360 picker (ADR-tied to `docs/Architecture/Adaptive Quality.md`).
- **Runtime API base URL** — persisted in `localStorage`, editable in a Settings
  modal that probes `/health` and reverts on failure (ADR-005).
- Rendition dedupe preferring h264 (upstream ships a mislabelled hevc fragment).

### Added — desktop (`web/src-tauri/`)

- Tauri v2 shell over the same React app: no sidecar, CSP allowing the remote API.
- Generated icon set from `scripts/gen-icon.ps1`.

### Added — Android (`android/`)

- Native Kotlin client (Compose + Media3), 14 source files: data layer, 4 screens,
  navigation, theme.
- `QualityLogic` — pure adaptive/manual quality module, 17 JVM unit tests.
- Buffer-driven adaptation on stable `Player` APIs.
- Adaptive + legacy launcher icons via `scripts/gen-android-icons.ps1`.
- **Status: written and hand-reviewed, never compiled** — no Android SDK here.

### Added — quality & docs

- **129 backend unit tests** (was zero): SSRF allowlist, classification, adult
  filter, cache, backoff, endpoint guards.
- **Obsidian vault** `docs/` — 24 wikilinked notes including ADR-001…ADR-005.
- **Serena memory graph** — 20 memories, root `mem:core`.
- Project files: `rules.md`, `architecture.md`, `design.md`, `AGENTS.md`,
  `SECURITY.md`, `CHANGELOG.md`, `.editorconfig`, `.env.example`.
- CI workflow: tests, type-check, production build, doc link check.
- `scripts/check_wikilinks.py` — validates every `[[wikilink]]` resolves.

### Changed

- Stream proxy switched from `curl_cffi` to `httpx` (ADR-002).
- Adult filter default is now `false` server-side (ADR-004).
- `CONTRIBUTING.md` rewritten — it had corrupted emoji and referenced the
  removed `verify.py`.
- `.gitignore` reworked: Node/Android/TS build artifacts, anchored `/moviebox/`.

### Fixed

- `/proxy/stream` returned 502 because `curl_cffi`'s `.stream()` is a generator,
  not a context manager.
- Duplicate 1080p picker rows and a stream dying at ~1 s from the mislabelled
  hevc fragment.
- Step-up threshold `GOOD_BUFFER_SEC` was declared but never applied, causing
  oscillation (climb → drain → drop).
- Player URLs derived from `window.location.origin` broke remote backends.

---

## [2.0.0] — upstream

- 1080p stream extraction, adaptive DASH manifests, multi-language subtitles.
- Signed Edge-Cache-Cookie / CloudFront-Policy decoding.
- TV series season & episode workflow.
- Embedded dashboard at `/`.
