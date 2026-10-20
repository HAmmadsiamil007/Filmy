# Upstream Quirks & Failure Modes

Read when something breaks that worked before. These are the non-obvious ones.

## 429 Too Many Requests
The stream CDN rate-limits aggressively. A 429 is NOT a Referer rejection - do not debug headers when you see it. Back off (fetch_with_backoff does exp backoff + jitter). Repeated probing during a session will lock you out for minutes.

## curl_cffi .stream() is a generator
Not a context manager. Calling __enter__() on it throws. This surfaced as a misleading 502 Bad Gateway from the proxy. Use httpx for streaming relays.

## DASH fragments mislabelled as MP4
Upstream returns entries with format="MP4" that are actually tiny (~0.8 MB) hevc DASH fragments, sharing a quality_value with the real h264 file. Always dedupe by quality_value and sanity-check size_mb + codec before treating an entry as playable.

## subject_type mislabelling
Types 6/7/8 exist (Anime/ShortDrama/Promo). The original code collapsed them all to "Series". Use classify_subject_type().

## Adult content in generic search
Searching almost anything returns Adult/AiAdult/Erotic titles - including queries like "short film" or "documentary". This is upstream behavior, not a bug in the query. Filter with is_adult()/strip_adult().

## Catalog junk
Music videos, jukeboxes, worship streams and podcasts are interleaved with real films. sort=NEW surfaces junk heaviest. Do not treat catalog order as relevance.

## Signed URLs expire
Stream URLs carry &t=<epoch>. Expired URLs fail at fetch time, not at resolve time. Resolve fresh at play; never cache past ~2 min (CACHE_TTL_STREAM).

## Auth bootstrap
api_request() retries once on 401/441 by forcing a fresh bootstrap token. If everything 401s, the bootstrap host or token derivation likely changed.