# Tauri Desktop Scaffold

SCAFFOLDED 2026-10-20. Files written, toolchain intentionally NOT installed.

## State
- web/src-tauri/ exists: Cargo.toml, tauri.conf.json, build.rs, src/main.rs, src/lib.rs, capabilities/default.json, .gitignore.
- @tauri-apps/cli 2.12.3 installed as npm devDependency. npm scripts: tauri, tauri:dev, tauri:build, tauri:icons.
- Config JSON validated. tauri info parses it and reports tauri: 2.
- The shell is THIN. ALL product logic stays in the React app. main.rs just builds the webview.

## NOT done - toolchain missing (verified 2026-10-20)
- rustc, cargo, rustup: NOT installed.
- MSVC Build Tools: NOT installed (vswhere absent).
- WebView2: PRESENT 155.0.4283.62. Node 24.18, npm 11.11.1, bun 1.4.3.
- => tauri dev and tauri build CANNOT run until the user installs Rust + MSVC C++ workload (~2 GB, admin, maybe reboot). User chose to install later themselves.

## Known blocker before any successful build
- tauri.conf.json bundle.icon lists 5 files under src-tauri/icons/ that DO NOT EXIST yet.
- tauri build WILL FAIL on missing icons. Generate with npm run tauri:icons from a source PNG at src-tauri/app-icon.png.

## Config facts
- identifier com.filmy.desktop, productName Filmy.
- frontendDist ../dist, devUrl http://127.0.0.1:5173, beforeBuildCommand npm run build.
- CSP: connect-src allows self plus http/https/ws/wss so a remote API base works; media-src allows http/https/blob for the proxy.

## Related
mem:architecture/remote_hosting, mem:architecture/clients, mem:task_completion