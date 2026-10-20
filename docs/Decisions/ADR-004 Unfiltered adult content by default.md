---
tags: [decision, adr, adult-content, policy]
status: accepted
date: 2026-10-20
risk-level: high
---

# ADR-004: Unfiltered adult content by default

## Status

⚠️ **Accepted by explicit user decision** — but the risk is recorded here deliberately.

## Context

The upstream MovieBox catalog is **not age-gated**. Adult titles bleed into every generic search — `porn`, `xxx`, `adult`, `sex`, `hentai`, and even `short film` / `documentary` return `Adult` / `AiAdult` / `Erotic` genres on the **first page**.

Both `subject_type=1` (movies) **and** `subject_type=7` (short-drama series) adult titles exist.

The backend was built with `include_adult=false` as the **default**, and filtering is server-side (`is_adult()` / `strip_adult()`) so it lives in one place.

The user then chose: **"Unfiltered by default."**

## Decision

- Backend keeps `include_adult=false` as the safe default on `/search`, `/movies`, `/tv-series`.
- The **web client passes `include_adult=true`**, so the shipped product shows everything.
- Filtering remains available server-side and is one flag away from being re-enabled per-platform.

## Consequences

> [!success] Technical
> - Filtering is centralized, not duplicated across four clients.
> - Flipping the product decision later is a **one-line** change in the web client.

> [!risk] Legal & ethical — recorded deliberately
> - Adult titles carry **no age verification, no rating metadata, no consent/provenance metadata**.
> - Distributing such content carries real compliance exposure (e.g. 18 U.S.C. §2257 record-keeping in the US, EU DSA obligations) that this API **cannot satisfy**, because it has no metadata to satisfy them with.
> - Combined with the piracy question, this is the **highest-risk** part of the project. See [[Risks and Compliance]].

## Alternatives considered

| Option | Why not taken |
| --- | --- |
| Filtered + hidden toggle | User explicitly chose unfiltered |
| Filtered + visible setting | User explicitly chose unfiltered |
| Unfiltered at the backend | Worse — removes the server-side control entirely |

## Reversal path

> [!tip] Cheap to undo
> Set `include_adult={false}` in the web client's `search()` default (`web/src/api.ts`), or add a settings toggle defaulting to off. No backend change needed.

## Related

→ [[Risks and Compliance]] · [[Reference/Subject Types]] · [[Guides/Debugging Upstream]] · [[Architecture/Backend]]
