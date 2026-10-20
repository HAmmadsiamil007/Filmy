// Tauri build script. Generates the context that links the Rust side to the
// frontend assets declared in tauri.conf.json (frontendDist: ../dist).
fn main() {
    tauri_build::build()
}
