# Tech Stack

## Backend
- Python 3.14.6, FastAPI 0.150, uvicorn 0.54.
- curl_cffi (TLS/JA3 impersonation, impersonate="chrome120") for upstream API calls.
- httpx for the stream proxy - deliberately NOT curl_cffi. See `mem:backend/stream_proxy`.
- No DB. In-process dict cache + threading.Lock. No auth.

## Frontend (web/)
- React 18 + TypeScript 5.6 (strict, noUnusedLocals) + Vite 5.
- hls.js is a declared dependency but NOT yet wired - the current player is MP4-only.
- No router, no state lib. Local useState in App.tsx.

## Platform
- Windows. PowerShell 5.1 - no tail, no bash-isms. Quote paths.
- Node v24.18.0, npm 11.11.1.
- venv at .venv/ (repo root, gitignored).

## Not yet built
- Tauri desktop shell (next milestone), Android phone APK, Android TV. See `mem:architecture/clients`.