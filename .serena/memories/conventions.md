# Conventions

## Backend (api.py)
- Routes are thin `def` handlers at the file bottom; heavy lifting lives in `engine.<method>()`.
- Errors: raise HTTPException with 502 for upstream failure, 403 for proxy allowlist rejection, 400 for bad input.
- Caching: cache_get/cache_set with explicit TTL constants (CACHE_TTL_METADATA 900s, CACHE_TTL_STREAM 120s).
- Upstream calls that can 429 are wrapped in fetch_with_backoff(lambda: ...).
- New helpers get a `# ----` banner comment explaining WHY, not just what.
- Response keys are snake_case: subject_id, is_adult, release_date.

## Frontend (web/src)
- TypeScript strict. Types live in api.ts, mirrored from backend snake_case.
- Components return early for empty states rather than nesting ternaries.
- Use useCallback for anything passed to media event handlers. Re-creating them re-fires loadedmetadata and stalls playback.
- Mutable values read inside media event handlers go in useRef (modeRef, lastSwitchRef), never state, to avoid stale closures.
- CSS: one plain file, -- custom properties in :root, dark theme, no utility framework.

## Style
- Comments explain non-obvious WHY (why httpx not curl_cffi, why dedupe by quality_value). Keep doing this.