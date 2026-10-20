---
tags: [decision, adr, proxy]
status: accepted
date: 2026-10-20
supersedes: direct-playback
---

# ADR-001: Proxy for Referer-locked playback

## Status

✅ **Accepted** — implemented, verified with `206` + `Content-Range`.

## Context

The video CDN (`bcdnxw.hakunaymatata.com`) is **Referer-locked** to the source domain (e.g. `https://123moviesfree.club/`).

A browser `<video>` element **cannot set a Referer header**. Therefore:

- Direct playback from any web/desktop client → `403`.
- This blocks the **entire** client strategy, not just one platform.

Empirically confirmed the CDN checks **only** `Referer` + `User-Agent` — **not** TLS/JA3 — because a plain `.NET HttpWebRequest` (Schannel stack) fetched the bytes successfully with those headers set.

## Decision

Add `GET /proxy/stream?url=<encoded>&referer=<domain>` to the backend, which:

1. Validates the URL against a **host allowlist** (SSRF control).
2. Forwards `User-Agent` + `Referer` + `Origin` + the client's `Range`.
3. Streams bytes back with `206`/`Content-Range`/`Accept-Ranges` and `Access-Control-Allow-Origin: *`.

Every client plays through this one URL with **zero custom headers**.

## Alternatives considered

| Option | Why rejected |
| --- | --- |
| **Set headers client-side** | Impossible in `<video>`; possible in ExoPlayer but wouldn't help web/desktop → 4 codebases |
| **Service Worker header injection** | Only covers web; fragile; doesn't fix Tauri/Android |
| **CORS proxy via third party** | Adds a dependency + trust boundary for copyrighted bytes |
| **Client-side MSE with credentialed fetch** | Requires CORS `Allow-Credentials` + non-`*` origin; CDN sends `*` |

## Consequences

> [!success] Positive
> - One playback path for all four clients — this is what makes [[ADR-003 Two codebases]] viable.
> - Seeking works (`Range` passthrough).
> - TLS spoofing unnecessary for video → simpler, faster relay.

> [!risk] Negative
> - **The proxy relays copyrighted bytes through your own server** → bandwidth cost and legal exposure. See [[Risks and Compliance]].
> - The allowlist is now a **security-critical** control. Widening it creates an open SSRF relay.
> - Single point of failure for playback.

## Invariants

> [!danger] Non-negotiable
> - Keep `PROXY_ALLOWED_HOST_SUFFIXES` tight. Never accept unvalidated full URLs.
> - Verify with **`206` + `Content-Range`**, never a bare `200`. See [[Guides/Verification]].

## Related

→ [[Architecture/Stream Proxy]] · [[ADR-002 httpx over curl_cffi for streaming]] · [[ADR-003 Two codebases]]
