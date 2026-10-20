# Legal and Compliance Risks

Recorded deliberately. Read before distributing anything.

# 1. Copyright - dominant risk
- API serves UNAUTHORIZED direct streams of commercial films and series.
- MIT license covers the CODE ONLY. It grants nothing over the indexed content.
- /proxy/stream means YOUR server actively redistributes copyrighted video - harder to defend than merely linking.

# 2. Adult content - unquantified exposure
- Product ships UNFILTERED (user decision). API cannot meet the compliance bar: no age verification,
  no consent or provenance metadata, no rating metadata, cannot satisfy 18 USC 2257 or EU DSA.
- Arguably HIGHER risk than piracy in practice - failure modes are criminal in several jurisdictions.

# 3. Security
- /proxy/stream?url= is an SSRF surface. The allowlist PROXY_ALLOWED_HOST_SUFFIXES is a SECURITY CONTROL - never widen it.
- NO auth, NO rate limiting, CORS allow_origins is a wildcard. Anyone can drive the proxy.

# 4. Operational
- Single upstream target; token derivation can change silently.
- Aggressive 429s: one noisy client locks everyone out for minutes.
- Zero test coverage anywhere.

# 5. Personal use vs distribution
- Local personal use is a very different category than publishing an APK, hosting a public API, or distributing a Tauri app.
- The remote/self-hosted decision (mem:architecture/remote_hosting) keeps the client thin; the operator, not every end user, runs the relay.

# Related
mem:architecture/remote_hosting, mem:core, mem:backend/stream_proxy