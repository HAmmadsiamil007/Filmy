"""Endpoint-level guards that run entirely in-process (no network).

Coverage:
* ``/health`` — the liveness probe all four clients call on startup.
* ``/proxy`` — the usage/allowlist discovery endpoint.
* ``/proxy/stream`` input validation — the two guards that reject a request
  BEFORE any outbound connection is made:

    1. scheme must be http/https      -> 400
    2. host must be in the allowlist  -> 403  (the SSRF control)

These tests deliberately use only hosts that FAIL the allowlist, so a broken
guard would show up as an unexpected network attempt / 502 rather than silently
hitting the internet.

Positive streaming behaviour (206 + Content-Range through the proxy) is a
LIVE integration check — see docs/Guides/Verification.md; it needs the real
backend + upstream and does not belong in the unit suite.
"""
from api import PROXY_ALLOWED_HOST_SUFFIXES

SUFFIX = PROXY_ALLOWED_HOST_SUFFIXES[0]


class TestHealth:
    def test_returns_200_with_ok_status(self, client):
        resp = client.get("/health")
        assert resp.status_code == 200
        body = resp.json()
        assert body["status"] == "ok"
        assert body["proxy_enabled"] is True

    def test_exposes_version(self, client):
        body = client.get("/health").json()
        assert isinstance(body["version"], str) and body["version"]

    def test_advertise_the_actual_allowlist(self, client):
        # Clients use this to explain why a URL was rejected; it must match
        # the tuple the guard actually enforces.
        body = client.get("/health").json()
        assert body["proxy_allowed_hosts"] == list(PROXY_ALLOWED_HOST_SUFFIXES)


class TestProxyInfo:
    def test_returns_usage_documentation(self, client):
        resp = client.get("/proxy")
        assert resp.status_code == 200
        body = resp.json()
        assert "/proxy/stream" in body["usage"]
        assert body["allowed_host_suffixes"] == list(PROXY_ALLOWED_HOST_SUFFIXES)


class TestProxyStreamValidation:
    def test_missing_url_is_a_validation_error(self, client):
        resp = client.get("/proxy/stream")
        assert resp.status_code == 422

    def test_empty_url_is_a_validation_error(self, client):
        # Query(..., min_length=1)
        resp = client.get("/proxy/stream", params={"url": ""})
        assert resp.status_code == 422

    def test_non_http_scheme_rejected_before_any_connection(self, client):
        for bad in (
            f"ftp://cdn.{SUFFIX}/video.mp4",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "data:text/html,hi",
        ):
            resp = client.get("/proxy/stream", params={"url": bad})
            assert resp.status_code == 400, f"{bad} -> {resp.status_code}"

    def test_disallowed_host_rejected_with_403(self, client):
        resp = client.get("/proxy/stream", params={"url": "http://example.com/v.mp4"})
        assert resp.status_code == 403

    def test_ssrf_attempt_against_loopback_rejected(self, client):
        resp = client.get(
            "/proxy/stream", params={"url": "http://127.0.0.1:8000/admin"}
        )
        assert resp.status_code == 403

    def test_ssrf_attempt_against_metadata_endpoint_rejected(self, client):
        # Cloud metadata service — the canonical SSRF target.
        resp = client.get(
            "/proxy/stream",
            params={"url": "http://169.254.169.254/latest/meta-data/"},
        )
        assert resp.status_code == 403

    def test_ssrf_attempt_against_private_range_rejected(self, client):
        resp = client.get(
            "/proxy/stream", params={"url": "http://10.0.0.5/internal"}
        )
        assert resp.status_code == 403

    def test_lookalike_host_rejected(self, client):
        resp = client.get(
            "/proxy/stream", params={"url": f"http://evil{SUFFIX}/v.mp4"}
        )
        assert resp.status_code == 403

    def test_rejection_details_are_actionable(self, client):
        # Error body must tell a client developer which guard tripped.
        resp = client.get("/proxy/stream", params={"url": "http://example.com/x"})
        detail = resp.json().get("detail", "")
        assert "allowlist" in detail.lower()


def test_allowlist_is_nonempty_and_tuples_of_str():
    assert isinstance(PROXY_ALLOWED_HOST_SUFFIXES, tuple)
    assert PROXY_ALLOWED_HOST_SUFFIXES
    assert all(isinstance(s, str) and s for s in PROXY_ALLOWED_HOST_SUFFIXES)
    # No leading dots — matching is host == s or host.endswith("." + s).
    assert all(not s.startswith(".") for s in PROXY_ALLOWED_HOST_SUFFIXES)


class TestDetailRobustness:
    """``/detail/{id}`` is normally fed IDs that came from search/home, but a
    stale bookmark or a fabricated ID makes upstream answer ``"data": null``.

    A dict default (``.get("data", {})``) only applies when the key is ABSENT,
    so a null payload used to reach ``data.get(...)`` and surface as an
    unhandled ``AttributeError`` -> HTTP 500. Clients should get a well-formed
    (if empty) detail object instead.
    """

    def test_null_data_does_not_become_a_500(self, client, monkeypatch):
        import api

        monkeypatch.setattr(api.engine, "api_request", lambda *a, **k: {"data": None})
        resp = client.get("/detail/0000000000000000000")
        assert resp.status_code == 200
        assert resp.json()["title"] is None

    def test_absent_data_key_also_survives(self, client, monkeypatch):
        import api

        monkeypatch.setattr(api.engine, "api_request", lambda *a, **k: {})
        assert client.get("/detail/0000000000000000000").status_code == 200
