---
tags: [reference, commands, windows]
status: active
platform: windows
---

# Commands

> [!info] Windows-native
> PowerShell 5.1 — no `tail`, no bash-isms, quote paths. Node v24.18.0, npm 11.11.1, Python 3.14.6.

## Backend

```powershell
# one-time setup
python -m venv .venv
.venv\Scripts\pip.exe install -r requirements.txt

# run on port 8000
.venv\Scripts\uvicorn.exe api:app --host 127.0.0.1 --port 8000

# add --reload for dev (uvicorn does NOT hot-reload otherwise)
.venv\Scripts\uvicorn.exe api:app --host 127.0.0.1 --port 8000 --reload
```

### Kill a running server

No `pkill` on Windows:

```powershell
Get-CimInstance Win32_Process -Filter "Name like '%python%'" |
  Where-Object { $_.CommandLine -match 'uvicorn' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## Frontend

```powershell
Set-Location web
npm install
npm run dev      # vite on 5173, proxies to 127.0.0.1:8000
npm run build    # tsc -b && vite build -> web/dist
npx tsc -b       # type-check only
```

> [!note] npm warning is harmless
> `npm warn Unknown user config "allow-scripts"` appears on every command. Cosmetic — ignore it.

## Unit tests

129 tests, no network, ~1.6 s. **Run these before claiming a backend change works.**

```powershell
# one-time: test-only dep (not in requirements.txt)
.venv\Scripts\pip.exe install -r requirements-dev.txt

.venv\Scripts\pytest.exe tests -q            # 129 passed, exit 0
.venv\Scripts\pytest.exe tests\test_proxy_security.py -v
.venv\Scripts\pytest.exe -k "backoff or cache"   # subset
.venv\Scripts\pytest.exe --tb=long               # fuller tracebacks
```

Covers `is_proxyable` (SSRF), `classify_subject_type`, `is_adult`/`strip_adult`, the TTL cache, `fetch_with_backoff`, and endpoint guards. What is *not* covered: the proxy `206`/`Content-Range` check — that needs the live upstream ([[Guides/Verification]]).

## Tauri desktop

```powershell
Set-Location web
npm run tauri:dev      # needs Rust + MSVC installed (NOT yet)
npm run tauri:build
npm run tauri:icons    # regenerate src-tauri/icons/ from app-icon.png
powershell -File ..\scripts\gen-icon.ps1   # regenerate app-icon.png itself
```

## Android

```powershell
# needs Android SDK + Gradle (NOT yet); see android\README.md
Set-Location android
.\gradlew.bat assembleDebug       # -> app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat testDebugUnitTest   # QualityLogicTest (pure JVM, no device)
```

## Smoke tests

```powershell
Invoke-WebRequest "http://127.0.0.1:8000/health" -UseBasicParsing
Invoke-WebRequest "http://127.0.0.1:8000/search?q=Avatar" -UseBasicParsing
```

## Full-stack video check

Must go **through the Vite proxy chain** and expect `206`:

```powershell
$s = Invoke-WebRequest "http://127.0.0.1:5173/api/stream/8906247916759695608" -UseBasicParsing
$mp4 = ($s.Content | ConvertFrom-Json).streams |
       Where-Object { $_.format -eq 'MP4' } | Select-Object -First 1
$enc = [uri]::EscapeDataString($mp4.url)
$ref = [uri]::EscapeDataString($mp4.headers.Referer)

$req = [System.Net.HttpWebRequest]::Create("http://127.0.0.1:5173/proxy/stream?url=$enc&referer=$ref")
$req.AddRange(0, 2047)
$resp = $req.GetResponse()
[int]$resp.StatusCode   # expect 206
```

## Gotchas

> [!failure] Traps that waste time
> - `Measure-Command { Invoke-WebRequest … }` — use this to time cache cold vs warm.
> - `.NET HttpWebRequest` **throws** on 4xx/5xx — wrap in `try/catch` and read `$_.Exception.Response.StatusCode`.
> - `StreamReader.ReadToEnd()` on a video stream **errors** — the payload is binary.
> - PowerShell `-f` format strings with method calls inside (`([uri]$x).Host`) break parsing — assign to a variable first.

## Related

→ [[Guides/Verification]] · [[Project/Status]] · [[Architecture/Backend]]
