# Backend Core (api.py)

Single-file FastAPI app. Read `mem:core` first for invariants.

## Structure
- Top: imports, stdout reconfigure, app + CORS (allow_origins=["*"]).
- Constants: SECRET_KEY_*_B64, MODERN_USER_AGENT, WEB_USER_AGENT, API_BASE, NATIVE_API_BASE, BOOTSTRAP_HOST.
- Proxy allowlist + TTL cache + fetch_with_backoff helpers (added, see below).
- `engine` class: bootstrap_token() gets an anonymous token from apig.inmoviebox.com; api_request() relays with 401/441 re-auth retry.
- Bottom: route handlers.

## engine methods
- search(query, page, per_page) - POST /wefeed-mobile-bff/subject-api/search
- get_details(subject_id) - GET .../subject-api/get
- get_episodes(subject_id) - GET .../subject-api/season-info. Returns season/episode_count/resolutions.
- get_streams(subject_id, season, episode) - resolves Web CDN + Web DASH + Mobile Native streams, sorted by quality_value desc.
- get_subtitles(...) - .../subject-api/get-stream-captions

## Added helpers (do not duplicate)
- classify_subject_type(raw) - 1 Movie, 2 Series, 3/6 Anime, 7 ShortDrama, 8 Promo, else Series.
- is_adult(genre) / strip_adult(items) - token match on adult/erotic/porn/xxx/hentai/18+.
- cache_get/cache_set + CACHE_TTL_* constants.
- fetch_with_backoff(fn) - retries on 429 with exp backoff + jitter.
- is_proxyable(url) - host suffix allowlist check.
- _catalog(tab_id, ...) - shared by /movies (tabId 2) and /tv-series (tabId 5).

## Endpoints
/search (include_adult=false default), /search/suggest, /detail/{id}, /episodes/{id},
/api/stream/{id}, /api/stream/{id}/captions, /movies, /tv-series, /home,
/proxy/stream, /proxy, /health.

## Gotchas
- /movies and /tv-series previously duplicated ~25 lines each; now both call _catalog(). Keep it that way.
- /home returns the raw upstream JSON unmodified - no shaping, no caching.
- curl_cffi Session is sync and blocking; uvicorn runs these handlers in a threadpool.