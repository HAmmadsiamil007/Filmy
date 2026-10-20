---
tags: [roadmap, backlog]
status: planned
---

# Backlog

> [!info] Not scheduled
> Things worth doing, roughly ordered by value. Nothing here is committed.

## Playback

- [ ] **Wire `hls.js` / `dash.js`** — installed but unused; the player is MP4-only today. Adaptive DASH would genuinely help on bad connections. See [[Architecture/Adaptive Quality]] § Known gap.
- [ ] **Subtitles UI** — `/api/stream/{id}/captions` returns tracks but nothing consumes them. Needs a picker + `<track>` / ExoPlayer `TextOutput`.
- [ ] **Resume / continue watching** — persist position per `subject_id`+`se`+`ep`.
- [ ] **Next-episode autoplay** — easy win for series.

## Backend

- [ ] **Auth + per-user rate limiting** — the API is currently **wide open**.
- [ ] **Cache `/home`** — the only catalog endpoint still uncached.
- [ ] **Cache disk-backed or Redis** — in-process dict is lost on restart and per-worker.
- [ ] **Pagination guard** — `/movies` and `/tv-series` report `total=1000000` (a capped placeholder, not a real count). Don't trust it.

## UX / catalog quality

- [ ] **Client-side junk filter** — music videos, jukeboxes, worship streams and podcasts are interleaved with real films. Worse on a TV rail. See [[Guides/Debugging Upstream]] § Catalog junk.
- [ ] **Dub switcher** — `/detail` returns `dubs[]`; same title exists as separate `subject_id` per language. Unexposed.
- [ ] **Genre/country/year filters** — the backend hardcodes `"ALL"` for all three. Could be exposed.

## Client

- [ ] **Tauri shell** — [[Roadmap/Tauri Desktop]]
- [ ] **Android** — [[Roadmap/Android]]
- [ ] **Settings screen** — quality default, cache clear, dub picker. *(Adult-content toggle + API base exist already: [[Architecture/Runtime Config]], [[Decisions/ADR-004 Unfiltered adult content by default]].)*

## Ops

- [ ] **`.gitignore` for `web/`** — currently untracked (`?? web/`). Decide whether to commit it.
- [ ] **CI** — nothing runs `tsc`/`build` automatically.
- [x] **Tests** — **unit suite landed 2026-10-20**: 129 tests, all green in ~1.6s. Covers `classify_subject_type`, `is_adult`/`strip_adult`, `is_proxyable` (SSRF allowlist), TTL cache, `fetch_with_backoff`, and `/health` + `/proxy/stream` validation. Run: `.\.venv\Scripts\pytest.exe tests`.
      Deps: `requirements-dev.txt` (pytest only).
- [ ] **Proxy `Range` integration test** — still open. The 206 + `Content-Range` check needs the live backend + real upstream, so it lives in [[Guides/Verification]], not the unit suite. Automating it would need a recorded/stubbed upstream response.

## Related

→ [[Project/Status]] · [[Architecture/Client Strategy]] · [[Guides/Verification]]
