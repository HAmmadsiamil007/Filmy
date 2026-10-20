# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning: [Semantic Versioning](https://semver.org/).

Pre-2.0.0 history is in `git log` (`4289bc6 feat: v2.0.0 rewrite…`).

---

## [Unreleased]

### Changed - project identity (rename)

- **Renamed the project to `Filmy`.** Only *our* brand strings changed; every
  upstream literal the backend depends on was left byte-for-byte identical
  (`apig.inmoviebox.com`, `moviebox.ph`, `inmoviebox.com`, `movieboxonline.net`,
  `movieboxhd.net`, `movie-box.co`, `aoneroom.com`, `x-source: moviebox`, the
  `PROXY_ALLOWED_HOST_SUFFIXES` allowlist and `WEB_DOMAINS`).
- **Touched identifiers** - changing any of these changes on-disk state, which
  is why each has a migration:

  | Surface | Before | After |
  | --- | --- | --- |
  | npm package | `moviebox-web` | `filmy-web` |
  | Tauri | `productName` `MovieBox`, `identifier` `com.moviebox.desktop` | `Filmy`, `com.filmy.desktop` |
  | Rust crate / lib | `moviebox-desktop`, `moviebox_desktop_lib` | `filmy-desktop`, `filmy_desktop_lib` |
  | Android | `namespace`/`applicationId` `com.moviebox.app` | `com.filmy.app` |
  | Android Gradle root | `rootProject.name` `MovieBox` | `Filmy` |
  | Android prefs file | `moviebox` | `filmy` |
  | Settings key (both clients) | `moviebox.apiBase` | `filmy.apiBase` |
  | Backend symbols | `MovieBoxEngine` | `FilmyEngine` |
  | Android symbols | `MovieBoxApp`, `MovieBoxTheme`, `Theme.MovieBox` | `FilmyApp`, `FilmyTheme`, `Theme.Filmy` |

- **Saved settings survive the upgrade.** `config.ts` reads the pre-rename
  `moviebox.apiBase` as a fallback and adopts it; `Config.kt` copies the old
  `moviebox` SharedPreferences file across on first `init()`. Without this the
  rename would silently revert every existing user's configured backend URL.
- **Historical gitignore evidence kept the old path deliberately.** The
  `rules.md` / `.gitignore` / `Git Workflow.md` / `Status.md` / `git_hygiene.md`
  examples showing `moviebox/` matching `.../com/moviebox/app/` still quote the
  pre-rename path - rewriting them would make the example untrue.
- Upstream-facing prose still says "MovieBox" where it means the *catalog being
  scraped* (`AGENTS.md`, `Overview.md`, ADR-002, ADR-004, `core.md`).
- **Android remains uncompiled** - the package move was verified statically
  (14/14 package-vs-path match, all `com.filmy.*` imports resolve), not by Gradle.

---

## [2.5.0] - 2026-10-20

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
