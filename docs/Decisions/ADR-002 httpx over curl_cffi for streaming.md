---
tags: [decision, adr, httpx, curl_cffi]
status: accepted
date: 2026-10-20
---

# ADR-002: httpx over curl_cffi for streaming

## Status

✅ **Accepted** — after a real bug forced the question.

## Context

The backend already uses `curl_cffi` (`impersonate="chrome120"`) for **API** calls, because the MovieBox API endpoints check TLS/JA3 fingerprints. It seemed natural to reuse it for the video proxy.

**It failed.** `curl_cffi`'s `.stream()` returns a **generator**, not a context manager:

```python
upstream = session.stream("GET", url, ...)   # Generator, not CM
resp = upstream.__enter__()                  # throws -> surfaced as 502
```

The failure surfaced as a **misleading `502 Bad Gateway`** — it looked like an upstream problem, not a type error.

Separately, empirical testing showed the **video CDN does not pin TLS at all**: a plain `.NET HttpWebRequest` (Schannel, a completely different JA3 from Chrome) fetched the bytes fine, as long as `Referer`/`UA` were set.

## Decision

Use **`httpx`** for `/proxy/stream`. Keep `curl_cffi` for API calls only.

## Rationale

| Concern | `httpx` | `curl_cffi` |
| --- | --- | --- |
| `.stream()` contract | real async context manager | generator |
| TLS spoofing needed for video? | **no** (verified) | n/a |
| Streaming reliability | battle-tested | footgun here |

Since the CDN doesn't check JA3, `curl_cffi`'s only advantage is irrelevant for this path — while its API shape actively caused a bug.

## Consequences

> [!success] Positive
> - Proxy works; `206` + `Content-Range` verified.
> - No JA3 spoofing overhead on a multi-GB relay.
> - Clear separation: `curl_cffi` = API, `httpx` = bytes.

> [!warning] Negative
> - Two HTTP clients in one file. Mitigated by a comment at the call site explaining **why**.

> [!danger] Invariant
> Do **not** "simplify" by switching the proxy back to `curl_cffi`. The generator-vs-CM mismatch will reintroduce the 502.

## Related

→ [[Architecture/Stream Proxy]] · [[Guides/Debugging Upstream]] · [[ADR-001 Proxy for Referer-locked playback]]
