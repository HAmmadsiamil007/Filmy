---
tags: [decision, adr, hosting, architecture]
status: accepted
date: 2026-10-20
supersedes: bundled-backend
---

# ADR-005: Remote / self-hosted backend

## Status

✅ **Accepted** — user-selected. Implemented in `web/src/config.ts` + `Settings.tsx`.

## Context

Once [[Decisions/ADR-003 Two codebases]] landed, the remaining packaging question was: **where does the Python API actually run?**

Two options:

| | **Bundle as sidecar** | **Remote / self-hosted** |
| --- | --- | --- |
| User setup | zero | must supply an API URL |
| App size | +~25 MB + process mgmt | thin client |
| Updates | ship a new app | land server-side |
| Who relays the bytes | **every end user's machine** | the operator only |

## Decision

**Remote / self-hosted.** Clients ship thin and point at a user-supplied API base URL.

Rejecting bundling was primarily a **risk** decision: a bundled sidecar means *every user's machine* becomes a relay for copyrighted video — a far worse posture than a single operator-controlled host. See [[Risks and Compliance]] § Personal-use vs distribution.

## Implementation

- [[Architecture/Runtime Config]] — `web/src/config.ts`, localStorage-backed, empty = same-origin.
- `web/src/Settings.tsx` — modal with **Test** (`/health` probe) + **Save**.
- `web/src/api.ts` — `getJSON()` and `proxiedUrl()` both route through `apiUrl()`.
- Tauri CSP explicitly allows `connect-src … http: https: ws: wss:` so a remote base works from the desktop shell.

## Consequences

> [!success] Positive
> - Thin client → small APK, trivial updates.
> - Only the **operator** runs the relay, not every end user.
> - One config value switches between local dev, reverse-proxied prod, and remote.

> [!warning] Negative
> - Cross-origin by default → depends entirely on the backend emitting `Access-Control-Allow-Origin: *` (it does, on both the API and `/proxy/stream`).
> - User must host something; onboarding friction.

> [!danger] Invariant
> `apiUrl()` is the **only** place the base is joined. Never build playback URLs from `window.location.origin` — with a remote backend that points video at the wrong host.

## Reversal path

Bundling later is additive: spawn the Python process as a Tauri sidecar and set the default base to `http://127.0.0.1:<port>`. No client refactor needed.

## Related

→ [[Architecture/Runtime Config]] · [[Decisions/ADR-003 Two codebases]] · [[Roadmap/Tauri Desktop]] · [[Risks and Compliance]]
