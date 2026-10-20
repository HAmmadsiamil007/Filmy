// Runtime-configurable API endpoint.
//
// Product decision (ADR: backend hosting = remote/self-hosted): clients ship
// thin and point at a user-supplied API base URL. An empty string means
// "same origin", which is what the Vite dev proxy and any reverse-proxied
// production deploy use.
//
// Persisted to localStorage so desktop (Tauri) and web remember it. Android
// mirrors this shape with SharedPreferences.

const STORAGE_KEY = "moviebox.apiBase";

/** Strip trailing slashes so path joining never produces double slashes. */
function normalize(raw: string): string {
  return raw.trim().replace(/\/+$/, "");
}

let cached: string | null = null;

/**
 * Current API base. Empty string = same origin.
 * Falls back to same-origin if nothing is stored.
 */
export function getApiBase(): string {
  if (cached !== null) return cached;
  try {
    cached = normalize(window.localStorage.getItem(STORAGE_KEY) ?? "");
  } catch {
    // Private mode / blocked storage — fall back to same origin.
    cached = "";
  }
  return cached;
}

/** Persist a new API base. Pass "" to return to same-origin. */
export function setApiBase(raw: string): string {
  const value = normalize(raw);
  cached = value;
  try {
    if (value) window.localStorage.setItem(STORAGE_KEY, value);
    else window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Non-fatal: the value is still applied for this session via `cached`.
  }
  return value;
}

/** True when the app is running inside a Tauri window. */
export function isTauri(): boolean {
  return typeof window !== "undefined" && "__TAURI_INTERNALS__" in window;
}

/**
 * Resolve an API path to an absolute URL.
 *
 * Same-origin (empty base) returns the bare path so the dev proxy handles it.
 * A configured base is joined as an absolute origin.
 */
export function apiUrl(path: string): string {
  const base = getApiBase();
  if (!base) return path;
  return `${base}${path.startsWith("/") ? path : `/${path}`}`;
}

/** Probe the configured backend. Throws on network/HTTP failure. */
export async function checkHealth(timeoutMs = 8000): Promise<{
  ok: boolean;
  status?: number;
  version?: string;
  error?: string;
}> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(apiUrl("/health"), { signal: controller.signal });
    if (!res.ok) return { ok: false, status: res.status, error: `HTTP ${res.status}` };
    const data = (await res.json()) as { version?: string };
    return { ok: true, status: res.status, version: data.version };
  } catch (err) {
    const message =
      err instanceof DOMException && err.name === "AbortError"
        ? `Timed out after ${timeoutMs}ms`
        : err instanceof Error
          ? err.message
          : "Unknown error";
    return { ok: false, error: message };
  } finally {
    window.clearTimeout(timer);
  }
}
