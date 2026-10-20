# Frontend Settings and Runtime Config

## config.ts
- STORAGE_KEY = filmy.apiBase. normalize() strips trailing slashes.
- getApiBase caches in a module-level var; reads localStorage lazily; try/catch for private mode.
- apiUrl(path) returns path when base is empty, else base + path.
- checkHealth(timeoutMs=8000) uses AbortController; returns a result object and NEVER throws.
- isTauri checks for __TAURI_INTERNALS__ on window.

## Settings.tsx
- Props: open / onClose / onApplied. Draft state reset on open via useEffect.
- Probe states are a discriminated union: idle, checking, ok, fail.
- test() applies the draft, probes, and REVERTS on failure so a bad host is never persisted.
- apply() saves then calls onApplied, which in App.tsx clears results/detail/streams and re-runs the query.

## Gotcha
- The settings button title doubles as a status readout: API: <base> or API: same origin.
- Styles live in styles.css under .modal*, .icon-btn, .btn-ghost, .btn-primary.

## Related
mem:architecture/remote_hosting, mem:frontend/core