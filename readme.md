# MovieBox REST API

[![CI](https://github.com/walterwhite-69/Moviebox-API/actions/workflows/ci.yml/badge.svg)](https://github.com/walterwhite-69/Moviebox-API/actions/workflows/ci.yml)
[![FastAPI](https://img.shields.io/badge/FastAPI-005571?style=flat-square&logo=fastapi)](https://fastapi.tiangolo.com/)
[![Python](https://img.shields.io/badge/Python-3.14-3776AB?style=flat-square&logo=python&logoColor=white)](https://www.python.org/)
[![Tests](https://img.shields.io/badge/tests-129_passing-brightgreen?style=flat-square)](tests/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

A self-hostable streaming proxy with **adaptive-quality playback** across web,
desktop and Android — built around a single-file FastAPI backend that resolves
Referer-locked streams and relays them with full `Range` support.

> [!warning]
> This proxies third-party content, including **adult titles** (unfiltered by
> default). The code is MIT; the content is not ours. Read
> [`docs/Risks and Compliance.md`](<docs/Risks and Compliance.md>) before
> distributing it.

---

## What's here

| Path | What it is | State |
|------|-----------|-------|
| `api.py` | The backend: 13 routes, stream proxy, TTL cache | ✅ verified live |
| `web/` | React + Vite client with adaptive player | ✅ verified |
| `web/src-tauri/` | Desktop shell (Tauri v2) wrapping `web/` | 🟡 needs Rust/MSVC |
| `android/` | Native Kotlin client (Compose + Media3) | 🟡 written, never compiled |
| `tests/` | 129 backend unit tests | ✅ green |
| `docs/` | Obsidian vault: architecture, ADRs, guides | ✅ 0 broken links |
| `.serena/` | Agent memory graph | ✅ 20 memories |

**Start here:** [`rules.md`](rules.md) → [`architecture.md`](architecture.md) → [`docs/Project/Status.md`](<docs/Project/Status.md>)

---

## Quick start

```powershell
# 1. backend
python -m venv .venv
.venv\Scripts\pip.exe install -r requirements.txt
.venv\Scripts\uvicorn.exe api:app --host 127.0.0.1 --port 8000

# 2. web client
cd web
npm install
npx vite --port 5173
```

Then open <http://127.0.0.1:5173>.

macOS/Linux equivalents use `.venv/bin/…`; all commands are listed in
[`docs/Reference/Commands.md`](<docs/Reference/Commands.md>).

### Verify your setup

```powershell
.\.venv\Scripts\pip.exe install -r requirements-dev.txt
.\.venv\Scripts\pytest.exe tests -q        # 129 passed
python scripts\check_wikilinks.py          # 0 broken
cd web; npx tsc -b; npm run build          # both exit 0
```

---

## API

**13 routes.** Full reference: [`docs/Reference/API Endpoints.md`](<docs/Reference/API Endpoints.md>)

### Playback flow

```http
GET /search?q=Avatar
GET /api/stream/{subject_id}                    # signed URLs, expire ~120s
GET /api/stream/{subject_id}/captions
GET /proxy/stream?url={encoded_cdn_url}         # 206 + Content-Range, seekable
```

TV series add `?se=<season>&ep=<episode>`:

```http
GET /episodes/{subject_id}                      # season/episode breakdown
GET /api/stream/{subject_id}?se=1&ep=2
```

### Routes

| Group | Routes |
|-------|--------|
| Meta | `/health`, `/proxy`, `/` (dashboard) |
| Search | `/search`, `/search/suggest`, `/detail/{id}`, `/episodes/{id}` |
| Catalog | `/movies`, `/tv-series`, `/home` |
| Playback | `/api/stream/{id}`, `/api/stream/{id}/captions`, `/proxy/stream` |

### Query parameters

| Param | Default | Effect |
|-------|---------|--------|
| `include_adult` | `false` | Opt **in** to adult titles (ADR-004) |
| `page` / `per_page` / `sort` | `1` / `24` / `RECOMMEND` | Catalog paging |
| `se` / `ep` | `0` / `0` | Season / episode selection |

---

## Design highlights

- **`/proxy/stream`** — the CDN is `Referer`-locked, so browsers and ExoPlayer
  can't fetch directly. The API injects headers server-side and forwards `Range`,
  making one URL work identically in `<video>`, ExoPlayer and the Tauri webview.
  An SSRF allowlist stops it becoming an open relay — see
  [`SECURITY.md`](SECURITY.md).
- **Adaptive quality** — auto mode steps up/down on buffer health, plus a manual
  1080/720/480/360 picker. Same model in both clients.
- **Runtime-configurable backend** — every client stores its own API base URL and
  probes `/health` before saving it (ADR-005): no bundled Python sidecar.
- **TTL cache + 429 backoff** — cold metadata 906 ms → warm 32 ms.

---

## Development

| Document | Purpose |
|----------|---------|
| [`rules.md`](rules.md) | Invariants — **read before changing anything** |
| [`AGENTS.md`](AGENTS.md) | Entry point for AI coding tools |
| [`architecture.md`](architecture.md) | System map & request paths |
| [`design.md`](design.md) | Shared design tokens (web ↔ Android) |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Workflow, verification, commit style |
| [`CHANGELOG.md`](CHANGELOG.md) | What changed |
| [`SECURITY.md`](SECURITY.md) | Threat model & reporting |

CI runs tests, type-check, production build and the doc link check on every push
and PR.

### Project layout

```
api.py            FastAPI backend (single file: config → helpers → engine → routes)
web/              React + Vite client; src-tauri/ wraps it for desktop
android/          Kotlin + Compose + Media3 client; TV extends it
tests/            129 pytest tests — SSRF, classification, cache, backoff
docs/             Obsidian vault (24 wikilinked notes, ADR-001…005)
.serena/          Agent memory graph (20 memories)
scripts/          Icon generators + link checker
.github/workflows/ CI
```

---

## License

Distributed under the MIT License — see [`LICENSE`](LICENSE).
