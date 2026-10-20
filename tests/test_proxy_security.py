"""SSRF control — `PROXY_ALLOWED_HOST_SUFFIXES` / `is_proxyable()`.

This allowlist is the ONLY thing standing between `/proxy/stream` and an open
relay that fetches arbitrary URLs from the server (SSRF). It is the single
most security-critical pure function in `api.py`, so its matching rules are
pinned here.

INVARIANTS UNDER TEST
---------------------
* Exact match and any-depth subdomain are allowed.
* A lookalike that merely *ends with* the suffix (evil moviebox.ph) is NOT.
* Subdomains of the allowlisted host are allowed; the allowlisted host used as
  a subdomain of an attacker domain is NOT.
* Loopback / link-local / metadata IPs are NOT proxyable.
* Empty or unparseable input fails CLOSED (returns False).

Endpoint-level guards (scheme check, 403 on reject) are covered in
test_endpoints.py.
"""
import pytest

from api import PROXY_ALLOWED_HOST_SUFFIXES, is_proxyable

# Use a real entry rather than hardcoding, so the test cannot silently pass
# if the allowlist is edited.
SUFFIX = PROXY_ALLOWED_HOST_SUFFIXES[0]


class TestAllowedHosts:
    @pytest.mark.parametrize("suffix", PROXY_ALLOWED_HOST_SUFFIXES)
    def test_exact_host_allowed(self, suffix):
        assert is_proxyable(f"https://{suffix}/video.mp4") is True

    @pytest.mark.parametrize("suffix", PROXY_ALLOWED_HOST_SUFFIXES)
    def test_single_subdomain_allowed(self, suffix):
        assert is_proxyable(f"https://cdn.{suffix}/video.mp4") is True

    def test_deep_subdomain_allowed(self):
        assert is_proxyable(f"https://a.b.c.{SUFFIX}/v.mp4") is True

    def test_host_with_port_allowed(self):
        # `hostname` strips the port; a port must not defeat the allowlist.
        assert is_proxyable(f"https://{SUFFIX}:8443/video.mp4") is True

    def test_uppercase_host_is_case_insensitive(self):
        assert is_proxyable(f"https://CDN.{SUFFIX.upper()}/v.mp4") is True

    def test_query_and_fragment_ignored(self):
        assert is_proxyable(f"https://cdn.{SUFFIX}/v.mp4?t=1&x=2#frag") is True


class TestRejectedHosts:
    def test_suffix_string_injection_not_allowed(self):
        # "evil" + suffix gives a host that ENDS WITH the suffix but is not
        # preceded by a dot — endswith("." + suffix) must not match it.
        assert is_proxyable(f"https://evil{SUFFIX}/v.mp4") is False

    def test_allowlisted_host_as_subdomain_of_attacker(self):
        # moviebox.ph.evil.com — host ENDS with the suffix only after a dot we
        # do not want, so this must be rejected.
        assert is_proxyable(f"https://{SUFFIX}.attacker.example/v.mp4") is False

    @pytest.mark.parametrize(
        "host",
        [
            "127.0.0.1",
            "localhost",
            "0.0.0.0",
            "169.254.169.254",  # cloud metadata endpoint
            "[::1]",
            "internal.corp.example",
            "example.com",
        ],
    )
    def test_loopback_metadata_and_arbitrary_hosts_rejected(self, host):
        assert is_proxyable(f"http://{host}/v.mp4") is False

    @pytest.mark.parametrize(
        "bad",
        [
            "",
            "not-a-url",
            "/relative/path.mp4",
            "https:///nohost.mp4",
            "////",
        ],
    )
    def test_missing_host_fails_closed(self, bad):
        assert is_proxyable(bad) is False

    def test_non_string_input_fails_closed(self):
        # urlsplit(None) raises TypeError -> caught -> False.
        assert is_proxyable(None) is False  # type: ignore[arg-type]

    def test_suffix_not_matched_without_dot_boundary(self):
        # "evil-moviebox.ph" does not equal the suffix and does not end with
        # ".moviebox.ph" (hyphen, not dot) -> rejected.
        assert is_proxyable("https://evil-moviebox.ph/v.mp4") is False

    @pytest.mark.parametrize("suffix", PROXY_ALLOWED_HOST_SUFFIXES)
    def test_lookalike_prefix_rejected(self, suffix):
        assert is_proxyable(f"https://not{suffix}/v.mp4") is False


# --- scheme handling lives at the endpoint, not here ------------------------
def test_is_proxyable_ignores_scheme():
    """Documented: is_proxyable() only checks HOST.

    Scheme enforcement (http/https only) is the endpoint's job — see
    test_endpoints.py asserting 400 for ftp://. Keeping this split explicit so
    nobody removes the scheme check believing is_proxyable covers it.
    """
    assert is_proxyable(f"ftp://cdn.{SUFFIX}/v.mp4") is True
