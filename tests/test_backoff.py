"""`fetch_with_backoff()` — 429-aware retry with exponential backoff + jitter.

The upstream CDN rate-limits aggressively (verified live: cold requests to the
same endpoint return 429). Without this, a burst of searches fails outright.

What is pinned here:
* Only 429 is retried as an HTTPException; other HTTP errors fail fast.
* Non-HTTP exceptions ARE retried (connection resets, timeouts).
* Retries stop at MAX_FETCH_RETRIES and the last exception propagates.
* Sleep grows as ``2**attempt * 0.5`` capped at 4.0s, plus 0..0.3s jitter.

``time.sleep`` is monkeypatched so the suite stays fast (real backoff would
spend ~8s per exhaustion test).
"""
import pytest
from fastapi import HTTPException

import api
from api import MAX_FETCH_RETRIES, fetch_with_backoff


@pytest.fixture
def sleeps(monkeypatch):
    """Replace time.sleep with a recorder that never actually sleeps."""
    recorded: list[float] = []
    monkeypatch.setattr(api.time, "sleep", recorded.append)
    return recorded


def test_success_returns_on_first_try(sleeps):
    calls = []

    def ok():
        calls.append(1)
        return "payload"

    assert fetch_with_backoff(ok) == "payload"
    assert len(calls) == 1
    assert sleeps == [], "must not back off when the first attempt succeeds"


def test_429_is_retried_then_succeeds(sleeps):
    state = {"n": 0}

    def flaky():
        state["n"] += 1
        if state["n"] < 3:
            raise HTTPException(status_code=429, detail="rate limited")
        return "ok"

    assert fetch_with_backoff(flaky) == "ok"
    assert state["n"] == 3, "expected two failed attempts then success"
    assert len(sleeps) == 2, "one sleep per retry"


def test_non_429_http_error_fails_immediately(sleeps):
    calls = []

    def boom():
        calls.append(1)
        raise HTTPException(status_code=500, detail="upstream broke")

    with pytest.raises(HTTPException) as exc:
        fetch_with_backoff(boom)

    assert exc.value.status_code == 500
    assert len(calls) == 1, "a non-429 must NOT be retried"
    assert sleeps == []


def test_non_429_client_error_fails_immediately(sleeps):
    calls = []

    def boom():
        calls.append(1)
        raise HTTPException(status_code=403, detail="forbidden")

    with pytest.raises(HTTPException) as exc:
        fetch_with_backoff(boom)

    assert exc.value.status_code == 403
    assert len(calls) == 1


def test_generic_exception_is_retried(sleeps):
    calls = []

    def conn_reset():
        calls.append(1)
        raise ConnectionResetError("connection reset by peer")

    with pytest.raises(ConnectionResetError):
        fetch_with_backoff(conn_reset, retries=3)

    assert len(calls) == 3, "transport errors must be retried"
    assert len(sleeps) == 2


def test_retries_stop_at_max_and_propagate_429(sleeps):
    calls = []

    def always_429():
        calls.append(1)
        raise HTTPException(status_code=429, detail="rate limited")

    with pytest.raises(HTTPException) as exc:
        fetch_with_backoff(always_429)

    assert exc.value.status_code == 429
    assert len(calls) == MAX_FETCH_RETRIES, "must give up after MAX_FETCH_RETRIES"
    assert len(sleeps) == MAX_FETCH_RETRIES - 1, "no sleep after the final attempt"


def test_retries_stop_at_max_and_propagate_generic(sleeps):
    calls = []

    def always_fails():
        calls.append(1)
        raise ValueError("always")

    with pytest.raises(ValueError):
        fetch_with_backoff(always_fails, retries=5)

    assert len(calls) == 5


def test_backoff_grows_exponentially_with_jitter(sleeps):
    state = {"n": 0}

    def flaky():
        state["n"] += 1
        if state["n"] < 4:
            raise HTTPException(status_code=429)
        return "ok"

    fetch_with_backoff(flaky)

    assert len(sleeps) == 3
    # Strictly increasing across the first three retries.
    assert sleeps[0] < sleeps[1] < sleeps[2]

    # Base delay is 2**attempt * 0.5 -> 0.5, 1.0, 2.0; jitter adds 0..0.3s.
    for sleep, base in zip(sleeps, (0.5, 1.0, 2.0)):
        assert base <= sleep <= base + 0.3


def test_backoff_is_capped_at_four_seconds(sleeps):
    calls = []

    def always_429():
        calls.append(1)
        raise HTTPException(status_code=429)

    # always_429 never succeeds, so fetch_with_backoff must end by propagating.
    with pytest.raises(HTTPException):
        fetch_with_backoff(always_429, retries=6)

    assert len(sleeps) == 5
    # Un-capped this would be 0.5, 1.0, 2.0, 4.0, 8.0 — the 5th sleep proves
    # min(2**attempt * 0.5, 4.0) is doing its job.
    assert sleeps[-1] < 5.0, f"backoff exceeded the 4s cap: {sleeps}"
    assert sleeps[-1] >= 4.0


def test_default_retries_is_four():
    assert MAX_FETCH_RETRIES == 4
