# Core

Filmy REST API + clients. Scrapes/proxies the MovieBox (moviebox.ph) catalog; does not host content.

## Source map
- api.py - the entire backend. Single-file FastAPI app. Routes live at the file bottom; everything above is the `engine` scraper class + helpers.
- web/ - React + Vite client (see `mem:frontend/core`).
- web/src/Player.tsx - adaptive quality player; quirks in `mem:frontend/player_quality`.
- requirements.txt, Procfile, railway.json, vercel.json - deploy targets (Heroku/Railway/Vercel).

## Invariants (do not break)
1. All video playback MUST go through `/proxy/stream`. The upstream CDN is Referer-locked; a browser <video> tag cannot send a Referer header and gets 403. See `mem:backend/stream_proxy`.
2. Never cache signed stream URLs beyond ~2 min. They carry `&t=<epoch>` and expire.
3. The proxy MUST keep its host allowlist (PROXY_ALLOWED_HOST_SUFFIXES). Removing it turns the API into an open SSRF relay.
4. subject_type values 6/7/8 are NOT plain series - always use classify_subject_type(), never the old `Movie if t==1 else Series` ternary.
5. Adult titles bleed into every generic search. is_adult()/strip_adult() exist for this; the web client deliberately opts OUT of filtering.

## Domain facts
- Upstream hosts: h5-api.aoneroom.com (web BFF), api3.aoneroom.com (mobile native), apig.inmoviebox.com (auth bootstrap token).
- Stream CDN: *.hakunaymatata.com
- Catalog mixes real films with YouTube-style music/jukebox junk; sort=NEW surfaces junk heaviest.

## Related
- Backend internals: `mem:backend/core`, `mem:backend/stream_proxy`
- Upstream breakage patterns: `mem:debugging/upstream_quirks`
- Client architecture: `mem:architecture/clients`
- Backend hosting (remote, thin client): `mem:architecture/remote_hosting`
- Desktop/Tauri scaffold + missing toolchain: `mem:architecture/desktop_tauri`
- Runtime API config + settings UI: `mem:frontend/settings`
- Legal/compliance exposure: `mem:legal_risks`
- Android APK (written, UNCOMPILED) + bugs caught by review: `mem:architecture/android_build`
- Backend unit suite: 129 tests, pytest: `mem:backend/tests`
- Repo-level project files (rules/AGENTS/architecture/design/CI/link-checker): `mem:project/root_files`
- Project renamed to Filmy - upstream-vs-ours naming rule + migrations: `mem:project/identity`
- gitignore traps + commit practice: `mem:project/git_hygiene`