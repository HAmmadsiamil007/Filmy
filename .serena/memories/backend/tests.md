# Backend test suite

LANDED 2026-00-00 -> 2026-10-20. 129 tests, exit 0, ~1.6s, ZERO network.

Run:  .\.venv\Scripts\pytest.exe tests -q
Deps: requirements-dev.txt (pytest==9.1.1 only; runtime deps stay in requirements.txt)

## Layout (tests/)
- conftest.py           sys.path -> repo root; `client` (TestClient, NOT a context
                        manager so lifespan never runs) and `clean_cache` fixtures.
- test_proxy_security.py  is_proxyable = the SSRF allowlist. THE critical test.
- test_classify_subject_type.py  taxonomy + fallback quirks.
- test_adult_filter.py   is_adult / strip_adult incl. non-string inputs.
- test_cache.py          TTL, falsy-value hits, blunt 2048 eviction.
- test_backoff.py        429-only retry, exponential cap, propagation.
- test_endpoints.py      /health, /proxy, /proxy/stream 400/403 guards.

## Why these, not others
Chosen because they are pure functions and the security control. The /proxy/stream
206 + Content-Range check CANNOT be unit-tested - it needs the live upstream.
That one stays in docs/Guides/Verification.md. Still open: automating it with a
stubbed/recorded upstream.

## Behaviour actually pinned (quirks, do not "fix" silently)
- classify_subject_type: 0 -> "Unknown", but ANY other unmapped positive int
  (e.g. 99, -1) -> "Series". Clients rely on non-Movie => fetch episodes.
- cache_get uses `if not hit` on the (expires_at, value) TUPLE, so [] / 0 / "" /
  False are hits. Do NOT cache None - indistinguishable from a miss (pinned).
- cache_set clears the ENTIRE cache once len > 2048 (no LRU). Crossing the cap
  causes a cold burst upstream. Pinned so an LRU rewrite is a visible change.
- is_proxyable checks HOST ONLY. Scheme enforcement is the endpoint's job
  (400 for ftp:/file:/data:). Split is deliberate - documented in the test.
- fetch_with_backoff: retries=4, sleep = min(2**attempt*0.5, 4.0) + jitter(0..0.3).
  Only HTTPException 429 and non-HTTP exceptions retry; other HTTP codes fail fast.

## Test-authoring failure modes caught HERE (run, don't just read)
1. A test calling a function guaranteed to raise had no pytest.raises() wrapper.
2. `import pytest` placed at the BOTTOM of test_cache.py -> NameError, because
   @pytest.mark.parametrize is evaluated at class-definition time (module import).
Both were mine, both found only by executing the suite.

## Env note
pytest was NOT preinstalled; installed into the existing .venv. It is ~2MB, not a
toolchain - see mem:suggested_commands for the toolchain-installation policy.