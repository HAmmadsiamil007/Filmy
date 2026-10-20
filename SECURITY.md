# Security Policy

---

## ⚠️ Read this first

This project is a **scraper proxy** for third-party content.

- The API has **no authentication and no user data**. It is deliberately open.
- The content is **piracy-adjacent**, and some catalog titles are adult material
  with no upstream age/consent metadata.

**Do not attach private data, credentials, personal information or user accounts
to this service.** Doing so would change its threat model — see
[`docs/Risks and Compliance.md`](<docs/Risks and Compliance.md>).

---

## Reporting a vulnerability

**Do not open a public issue.**

1. Report privately to the repository owner via GitHub Security Advisories
   (preferred) or a direct message.
2. Include: affected file/route, reproduction steps, impact, and whether you
   have already disclosed it anywhere.
3. Allow reasonable time for a fix before public disclosure.

**In scope (encouraged):**

- SSRF / open-relay behaviour in `/proxy` or `/proxy/stream`
- Injection (path traversal, header injection, response splitting)
- Cache poisoning or signed-URL mishandling
- Anything that lets a caller reach hosts outside `PROXY_ALLOWED_HOST_SUFFIXES`
- Denial of service reachable by a single unauthenticated request

**Out of scope:**

- The upstream service's own behaviour, availability, or content
- Legal/copyright questions (see `docs/Risks and Compliance.md`)
- Issues requiring prior authentication (there is none)
- Missing rate limiting on search/catalog endpoints — known and accepted for a
  local/self-hosted tool

---

## Security model

### The one boundary that matters

```
client ──► /proxy/stream?url=…
             ├─ scheme ∈ {http, https}     else 400
             └─ is_proxyable(host)         else 403   ◄── SSRF boundary
```

`is_proxyable()` in `api.py` compares the host against
`PROXY_ALLOWED_HOST_SUFFIXES` using exact-match or `.suffix` boundaries, and
**fails closed** on empty or unparseable input.

Covered by `tests/test_proxy_security.py`, which asserts rejection of:

- typosquat hosts (`evil…suffix`, `suffix.attacker.example`)
- `127.0.0.1`, `localhost`, `0.0.0.0`, `[::1]`
- `169.254.169.254` (cloud metadata endpoint)
- arbitrary private/internal hosts
- empty, relative and unparseable URLs

**Changing `PROXY_ALLOWED_HOST_SUFFIXES` is a security change.** Justify it in
the PR and extend the tests.

### Transport

- Upstream → API: `httpx`, forwarding `Referer`/`Origin`/`UA` server-side.
  The CDN does not pin TLS/JA3 (verified), so fingerprint impersonation is
  unnecessary — see [`docs/Decisions/ADR-002 httpx over curl_cffi for streaming.md`](<docs/Decisions/ADR-002 httpx over curl_cffi for streaming.md>).
- API → client: `httpx` mirrors the client's `Range` header; response carries
  `Access-Control-Allow-Origin: *`.

### Deliberately absent

| Control | Why not |
|---------|---------|
| Authentication | Self-hosted single-user tool; adding it would falsely imply multi-tenant safety |
| Rate limiting on catalog routes | Local tool; the upstream already rate-limits harder |
| Content moderation | Out of scope — see `docs/Risks and Compliance.md` |
| Secrets in the repo | `api.py` derives request signing material at runtime; `.env` is gitignored |

---

## Client-side notes

| Client | Consideration |
|--------|---------------|
| Web / Tauri | CSP permits the runtime-configured API origin; the Settings modal probes `/health` before persisting a base URL |
| Android | `network_security_config.xml` permits cleartext **only** because the backend is frequently `http://` on a LAN; production CDN traffic is https. Android rejects CIDR rules, so this is a documented `base-config`, not a range list |
| All | Base URL is user-supplied — never fetch it without validating it is what the user intended |

---

## Supported versions

| Version | Supported |
|---------|-----------|
| `main` (2.5.x) | ✅ |
| ≤ 2.0.0 | ❌ — no backports |
