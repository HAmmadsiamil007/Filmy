"""Shared fixtures for MovieBox backend tests.

Adds the repo root to sys.path so `import api` works no matter where pytest is
invoked from (repo root, tests/, or an IDE runner).
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import pytest  # noqa: E402


@pytest.fixture
def client():
    """In-process ASGI client for endpoint tests.

    Deliberately NOT used as a context manager: that would run lifespan/startup
    hooks, and we want these tests to touch no network.
    """
    from fastapi.testclient import TestClient

    import api

    return TestClient(api.app)


@pytest.fixture
def clean_cache():
    """Isolate the process-global TTL cache around a test."""
    import api

    with api._CACHE_LOCK:
        api._CACHE.clear()
    yield
    with api._CACHE_LOCK:
        api._CACHE.clear()
