# Remote / Self-Hosted Backend (ADR)

DECIDED 2026-10-20 by the user.

## Decision
Clients ship THIN and point at a user-supplied API base URL. The backend is NOT bundled as a sidecar.
Rejected the bundle-Python-in-app alternative: adds size, process-lifecycle complexity, and would turn
every user machine into a relay for copyrighted bytes (see mem:legal_risks).

## Implementation
- web/src/config.ts: getApiBase, setApiBase, apiUrl, checkHealth, isTauri. localStorage key filmy.apiBase.
- Empty base = SAME ORIGIN. That is what the Vite dev proxy and any reverse-proxied deploy use.
- web/src/Settings.tsx: modal with Test (calls /health) and Save.
- web/src/api.ts: getJSON and proxiedUrl both route through apiUrl. NEVER build URLs from window.location.origin directly; with a remote backend that points video at the wrong host.

## Invariants
- apiUrl(path) is the ONLY place a base is joined. Do not concatenate getApiBase manually elsewhere.
- proxiedUrl must follow the API base, not location.origin, or remote playback breaks.
- checkHealth temporarily applies the draft base and REVERTS it if the probe fails. Preserve that.
- isTauri detects __TAURI_INTERNALS__ in window. Do not hardcode tauri-specific behaviour.

## Related
mem:frontend/core, mem:architecture/clients, mem:frontend/settings