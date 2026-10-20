"""In-process TTL cache — `cache_get()` / `cache_set()`.

Two failure modes these tests guard against:

1. **TTL not honoured** -> expired signed stream URLs (`&t=<epoch>`) would be
   served after the upstream has already expired them (403 upstream).
2. **Falsy values treated as misses** -> `cache_get` uses `if not hit` on the
   stored *tuple*, so falsy payload values (`[]`, `0`, `""`) must still be
   returned as hits. Caching `[]` (a real "no results" response) is the
   common case, so this is the regression that matters.

The cache is process-global, so tests are isolated via the `clean_cache`
fixture in conftest.py.
"""
import pytest

import api
from api import CACHE_TTL_METADATA, CACHE_TTL_STREAM, CACHE_TTL_SUGGEST, cache_get, cache_set


def test_set_then_get_round_trips(clean_cache):
    cache_set("k", {"a": 1}, 60)
    assert cache_get("k") == {"a": 1}


def test_miss_returns_none(clean_cache):
    assert cache_get("nope") is None


def test_expired_entry_is_a_miss(clean_cache):
    # ttl <= 0 puts expires_at in the past on the very next time() tick.
    cache_set("k", "value", ttl=-1)
    assert cache_get("k") is None


def test_expired_entry_is_evicted_not_just_hidden(clean_cache):
    cache_set("k", "value", ttl=-1)
    assert cache_get("k") is None
    # A miss must actually remove the key, or the entry lingers forever.
    assert "k" not in api._CACHE


def test_unexpired_entry_still_returns(clean_cache):
    cache_set("k", "value", CACHE_TTL_METADATA)
    assert cache_get("k") == "value"


class TestFalsyValuesAreHits:
    """`if not hit` reads on the (expires_at, value) tuple — pin that."""

    def test_empty_list_is_a_hit_not_a_miss(self, clean_cache):
        # The realistic case: a search returning zero results.
        cache_set("empty", [], 60)
        assert cache_get("empty") == []

    @pytest.mark.parametrize("value", [0, "", False])
    def test_other_falsy_values_are_hits(self, value, clean_cache):
        cache_set("falsy", value, 60)
        assert cache_get("falsy") == value
        assert "falsy" in api._CACHE

    def test_none_value_is_indistinguishable_from_miss(self):
        # Documented limitation: cache_get returns None for both "expired /
        # absent" and "stored None". Harmless today (nothing caches None) but
        # do not start caching None without fixing cache_get's sentinel.
        cache_set("none", None, 60)
        assert cache_get("none") is None


def test_ttl_constants_are_sane():
    # Stream URLs are signed and expire -> MUST be the shortest TTL.
    assert CACHE_TTL_STREAM < CACHE_TTL_SUGGEST < CACHE_TTL_METADATA
    assert CACHE_TTL_STREAM > 0


def test_overwrite_replaces_value(clean_cache):
    cache_set("k", "old", 60)
    cache_set("k", "new", 60)
    assert cache_get("k") == "new"


def test_distinct_keys_do_not_collide(clean_cache):
    cache_set("a", 1, 60)
    cache_set("b", 2, 60)
    assert cache_get("a") == 1
    assert cache_get("b") == 2


def test_blunt_eviction_at_capacity(clean_cache):
    """cache_set clears the WHOLE cache once it exceeds 2048 entries.

    Pinned because this is deliberately crude: crossing the cap wipes every
    warm entry, causing a cold burst against upstream. If someone implements
    LRU, this test documents the behaviour that changed.
    """
    n = 2050
    for i in range(n):
        cache_set(f"k{i}", i, CACHE_TTL_METADATA)

    # The write that first exceeds the cap clears everything before storing.
    assert cache_get(f"k{n - 1}") == n - 1        # newest survives
    assert cache_get("k0") is None                # oldest were wiped
    assert len(api._CACHE) == 1
