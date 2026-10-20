---
tags: [reference, api]
status: active
---

# API Endpoints

Base: `http://127.0.0.1:8000` · Health: [`/health`](#health)

> [!tip] Adult filtering
> `/search`, `/movies`, `/tv-series` take `include_adult`. Backend default is `false`; the **web client passes `true`** by product decision — see [[Decisions/ADR-004 Unfiltered adult content by default]].

## Health

```
GET /health
→ {"status":"ok","version":"2.5.0","proxy_enabled":true,
   "proxy_allowed_hosts":[…],"adult_filter_default":"enabled …"}
```

## Search & metadata

| Endpoint | Method | Params | Notes |
| --- | --- | --- | --- |
| `/search` | GET | `q`*, `page`, `per_page`, `include_adult` | Returns `results[]` + `count` |
| `/search/suggest` | GET | `q`* | Autocomplete |
| `/detail/{id}` | GET | — | Metadata, genres, `dubs[]` |
| `/episodes/{id}` | GET | — | Season breakdown + per-resolution counts |

## Streams

| Endpoint | Params | Notes |
| --- | --- | --- |
| `/api/stream/{id}` | `se`, `ep`, `detail_path` | ~6 variants: MP4 + DASH @ 1080/720/480/360 |
| `/api/stream/{id}/captions` | `se`, `ep`, `stream_id` | `.srt`/`.vtt` tracks, signed URLs |

## Catalog

| Endpoint | Params | Upstream `tabId` |
| --- | --- | --- |
| `/movies` | `page`, `per_page`, `sort`, `include_adult` | 2 |
| `/tv-series` | `page`, `per_page`, `sort`, `include_adult` | 5 |
| `/home` | — | raw pass-through, uncached |

> [!warning] Catalog junk
> Music videos, jukeboxes, worship streams and podcasts are interleaved with real films. `sort=NEW` surfaces junk heaviest. Never treat catalog order as relevance — see [[Guides/Debugging Upstream]].

## Proxy

```
GET /proxy/stream?url=<encoded>&referer=<domain>
GET /proxy          # usage + allowlist
```

- Allowlist: `hakunaymatata.com`, `aoneroom.com`, `moviebox.ph`, `inmoviebox.com` — else `403`.
- Honours `Range` → `206` + `Content-Range`. Emits `ACA-Origin: *`.

> [!danger] Security
> The allowlist is an **SSRF control**. Full detail + invariants: [[Architecture/Stream Proxy]].

## Workflow examples

**Movie:**
```
GET /search?q=Avatar
GET /api/stream/8906247916759695608
GET /api/stream/8906247916759695608/captions
```

**TV series (Breaking Bad, S01E01):**
```
GET /search?q=Breaking+Bad
GET /episodes/6207982430134357800        # 8 seasons: 10/10/10/10/10/10/7/6
GET /api/stream/6207982430134357800?se=1&ep=1
GET /api/stream/6207982430134357800/captions?se=1&ep=1
```

## Related

→ [[Reference/Subject Types]] · [[Reference/Commands]] · [[Architecture/Stream Proxy]]
