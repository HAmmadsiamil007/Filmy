---
tags: [roadmap, desktop, tauri]
status: next
priority: high
depends_on: [ADR-003, ADR-001]
---

# Tauri Desktop

> [!info] Next milestone
> Wraps the existing `web/dist` verbatim. Little new UI — this is mostly config + a Rust shell.

## Why Tauri, not Electron

| | Tauri | Electron |
| --- | --- | --- |
| Bundle size | ~10 MB | ~150 MB |
| WebView | system (WebView2/WebKit) | bundled Chromium |
| Backend | Rust | Node |

The web app is already built and verified ([[Project/Status]]), so the desktop target is close to free.

## Build plan — STEP 1 DONE (scaffold written 2026-10-20)

- [x] `@tauri-apps/cli` 2.12.3 added as npm devDependency (npm-only, no Rust touched)
- [x] npm scripts: `tauri`, `tauri:dev`, `tauri:build`, `tauri:icons`
- [x] `web/src-tauri/` written — see **Scaffold contents** below
- [x] `tauri.conf.json` + `capabilities/default.json` JSON-validated; `tauri info` parses it (`tauri: 2`)
- [x] CSP set so a **remote** API base works: `connect-src 'self' http: https: ws: wss:`, `media-src … http/https/blob`
- [ ] **Install Rust + MSVC** (blocked on you — see below)
- [ ] Generate icons (`npm run tauri:icons`)
- [ ] `npm run tauri:dev` → first real launch

### Scaffold contents

```
web/src-tauri/
├── Cargo.toml               # tauri 2, tauri-build 2, release profile stripped+lto
├── tauri.conf.json          # identifier com.moviebox.desktop, frontendDist ../dist
├── build.rs
├── capabilities/default.json
├── .gitignore               # /target, /gen/schemas
└── src/
    ├── main.rs              # thin shell — all product logic stays in React
    └── lib.rs               # tauri::Builder, mobile_entry_point ready
```

> [!success] Deliberately thin
> `main.rs` only builds the webview. Per [[Decisions/ADR-005 Remote self-hosted backend]] there is **no Python sidecar** — the client points at a remote/self-hosted API via the in-app settings.

## Blockers

> [!danger] 1. Toolchain missing — you install this
> | Requirement | Status |
> | --- | --- |
> | WebView2 | ✅ 155.0.4283.62 |
> | Node / npm / bun | ✅ 24.18.0 / 11.11.1 / 1.4.3 |
> | **rustc / cargo / rustup** | ❌ not installed |
> | **MSVC Build Tools** | ❌ not detected |
>
> ~2 GB+, admin rights, often a reboot. Install via [rustup.rs](https://rustup.rs) + [vs_BuildTools.exe](https://aka.ms/vs/17/release/vs_BuildTools.exe) with the **Desktop development with C++** workload.

> [!failure] 2. Icons do not exist — `tauri build` will fail
> `bundle.icon` lists 5 files under `src-tauri/icons/` (`32x32.png`, `128x128.png`, `128x128@2x.png`, `icon.ico`, `icon.png`). None are present yet.
>
> **Fix:** drop a source PNG at `web/src-tauri/app-icon.png` (≥1024×1024), then `npm run tauri:icons`.

## Backend hosting — ✅ DECIDED

> [!success] Resolved 2026-10-20 → ADR-005
> **Remote / self-hosted.** The client ships thin, no Python sidecar, and points at a user-supplied API base via the in-app settings.
>
> Rationale: bundling would make *every end user's machine* a relay for copyrighted bytes — a far worse posture than a single operator-controlled host. See [[Risks and Compliance]] § Personal-use vs distribution.
>
> Full record: [[Decisions/ADR-005 Remote self-hosted backend]] · implementation: [[Architecture/Runtime Config]].

## Risks

- Stream URLs **expire** (`&t=<epoch>`) — resolve at play time, never persist.
- Upstream `429`s — backend backs off; the desktop UI should surface retry.
- Icons missing → `tauri build` fails until generated (see Blockers above).

## Related

→ [[Decisions/ADR-005 Remote self-hosted backend]] · [[Architecture/Runtime Config]] · [[Decisions/ADR-003 Two codebases]] · [[Roadmap/Android]] · [[Roadmap/Backlog]]
