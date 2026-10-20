---
tags: [risk, legal, compliance, security]
status: active
risk-level: high
---

# Risks & Compliance

> [!warning] Read before distributing anything
> The code is MIT-licensed. **The content it indexes is not.** These are separate things and the distinction matters legally.

## 1. Copyright — the dominant risk

The API serves **unauthorized direct streams** of commercial films and series.

- The repo's **MIT license covers the code only**. It grants nothing over the indexed content.
- Relaying streams through your own `/proxy/stream` means **your server** is actively redistributing copyrighted video — that is harder to defend than merely linking.
- Upstream's own ToS are violated regardless.

**Mitigations:** don't distribute; don't host publicly; if you must, host the *metadata* API but not the byte relay. See [[Decisions/ADR-001 Proxy for Referer-locked playback]] § Consequences.

## 2. Adult content — unquantified exposure

Product decision was to ship **unfiltered** ([[Decisions/ADR-004 Unfiltered adult content by default]]). The API cannot meet the compliance bar that category carries:

- **No age verification** of performers.
- **No consent / provenance metadata.**
- **No rating metadata** — nothing lets a client gate access.
- Cannot satisfy record-keeping regimes (e.g. 18 U.S.C. §2257 in the US) because there are no records to keep.
- EU **DSA** obligations (illegal-content notice, minor protection) would apply to any EU-facing deployment.

> [!danger] This is the highest-risk part of the project
> Higher than the piracy question in practice, because the failure modes are criminal rather than civil in several jurisdictions.

## 3. Security — the proxy is an SSRF surface

`/proxy/stream?url=…` accepts a caller-supplied URL. Without `PROXY_ALLOWED_HOST_SUFFIXES` it becomes an **open SSRF relay** into your network.

**Invariant:** the allowlist is security-critical. Never widen it, never accept unvalidated URLs. See [[Architecture/Stream Proxy]].

Also currently missing:

- **No auth** — the API is wide open.
- **No rate limiting** — one caller can exhaust upstream goodwill for everyone (and get you `429`-locked).
- **CORS is `allow_origins=["*"]`** — intentional for the clients, but combined with no auth it means anyone can drive your proxy.

## 4. Operational

| Risk | Detail |
| --- | --- |
| Upstream fragility | Single scraper target; auth/token derivation can change silently |
| Aggressive `429`s | One noisy client locks everyone out for minutes |
| Bandwidth cost | Proxying multi-GB files is real money if exposed publicly |
| No tests | Zero coverage — a refactor can break playback silently |

## 5. Personal-use vs distribution

> [!tip] The practical line
> Running this locally for yourself sits in a very different risk category than publishing an APK, hosting a public API, or running a Tauri app that auto-relays for other people.
>
> The **backend-hosting decision** ([[Roadmap/Tauri Desktop]] § Open decision) is where this becomes concrete: bundling the API inside a distributed app means *every user's machine* becomes a relay.

## Related

→ [[Decisions/ADR-004 Unfiltered adult content by default]] · [[Decisions/ADR-001 Proxy for Referer-locked playback]] · [[Project/Overview]] · [[Roadmap/Tauri Desktop]]
