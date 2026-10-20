// Library entrypoint so the app can be built/tested as a crate.
// Kept minimal: the desktop target is a shell over the React frontend.

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
