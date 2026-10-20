---
tags: [guides, debugging, upstream]
status: active
---

# Debugging Upstream

> [!abstract] When to read this
> Something that worked before just broke, or a response shape looks wrong. These are the **non-obvious** failure modes.

## `429 Too Many Requests`

> [!warning] Not a Referer problem
> The stream CDN rate-limits **aggressively**. A `429` is **not** a Referer rejection — do not start debugging headers when you see one.
>
> Back off (`fetch_with_backoff` does exponential backoff + jitter). Repeated probing during a session can lock you out for **minutes**.

## `curl_cffi` `.stream()` is a generator

Not a context manager. Calling `__enter__()` on it throws — surfacing as a **misleading `502 Bad Gateway`** from the proxy.

**Fix:** use `httpx` for streaming relays. Reasoning + proof: [[Decisions/ADR-002 httpx over curl_cffi for streaming]].

## DASH fragments mislabelled as MP4

Upstream returns entries with `format="MP4"` that are actually tiny (~0.8 MB) **hevc** DASH fragments, sharing a `quality_value` with the real h264 file.

**Symptoms:** duplicate quality labels · duplicate React keys · playback dying at ~1s.

**Rule:** always dedupe by `quality_value` and sanity-check `size_mb` + `codec` before treating an entry as playable. See [[Architecture/Adaptive Quality]].

## `subject_type` mislabelling

Types 6/7/8 exist (Anime / ShortDrama / Promo). The original code collapsed them all to `"Series"`.

**Rule:** always `classify_subject_type()`. Taxonomy: [[Reference/Subject Types]].

## Adult content in generic search

Searching almost **anything** returns `Adult` / `AiAdult` / `Erotic` titles — including queries like `short film` or `documentary`.

> [!info] This is upstream behaviour, not a query bug
> Filter with `is_adult()` / `strip_adult()`. Decision record: [[Decisions/ADR-004 Unfiltered adult content by default]].

## Catalog junk

Music videos, jukeboxes, worship streams and podcasts are interleaved with real films. `sort=NEW` surfaces junk heaviest.

**Rule:** never treat catalog order as relevance.

## Signed URLs expire

Stream URLs carry `&t=<epoch>`. They fail at **fetch** time, not resolve time.

**Rule:** resolve fresh at play; never cache past ~2 min (`CACHE_TTL_STREAM`).

## Auth bootstrap failures

`api_request()` retries **once** on `401`/`441` by forcing a fresh bootstrap token. If *everything* 401s, the bootstrap host or token derivation likely changed — check `apig.inmoviebox.com` and the `SECRET_KEY_*_B64` constants.

## TLS fingerprinting — what is and isn't checked

> [!tip] Verified empirically
> The **API** endpoints need JA3 impersonation (`curl_cffi`, `impersonate="chrome120"`).
>
> The **video CDN does not** — proof: a plain `.NET` client fetched bytes fine with only `Referer`/`UA` set. This is why the proxy can use plain `httpx`.

## Related

→ [[Architecture/Stream Proxy]] · [[Guides/Verification]] · [[Architecture/Backend]]
