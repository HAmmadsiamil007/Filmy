// Thin desktop shell. All product logic lives in the React app (web/src).
//
// Hosting decision: the backend is REMOTE / SELF-HOSTED. This app deliberately
// does NOT bundle the Python API as a sidecar — the user points the client at
// an API base URL via the in-app settings dialog (web/src/Settings.tsx), which
// persists to localStorage and works identically here and in the browser.

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    moviebox_desktop_lib::run()
}
