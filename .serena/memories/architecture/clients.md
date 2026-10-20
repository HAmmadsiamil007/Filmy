# Client Architecture

Decided 2026-10-20. Read `mem:core` first.

## Why 2 codebases, not 4
Web + desktop share one React app; Android phone + TV share one Kotlin app. Backend-first was chosen because every client depends on it.

## Targets
1. Web - React + Vite + hls.js/dash.js. DONE (`mem:frontend/core`).
2. Desktop - Tauri wrapping the exact same web/dist. Rust shell, ~10 MB vs Electron ~150 MB. Near-zero new UI.
3. Android phone APK - Kotlin + Jetpack Compose + Media3/ExoPlayer. Chosen over WebView because WebView video plays badly (no hardware DASH, weak seeking, battery drain).
4. Android TV - Kotlin + Compose for TV (Leanback) + Media3. Genuinely different UX (D-pad nav, 10-foot UI, focus rings), not just a bigger phone.

## Why the proxy makes this cheap
Because /proxy/stream normalizes Referer + Range + CORS, every client plays the SAME url with a plain player. No per-platform header logic. ExoPlayer, <video>, and a Tauri webview all consume it identically.

## Open decision (blocking Android + packaging)
Backend hosting: bundle the Python API inside the desktop app, or point clients at a self-hosted/remote API? Affects APK size, update flow, and whether the proxy relays copyrighted bytes through your own server.

## Risks to carry into Android
- ExoPlayer needs the proxy url plus no custom headers (the proxy handles it).
- Stream URLs expire: resolve at play time, never persist to disk.
- 429s from upstream: the client should surface a retry, the backend already backs off.