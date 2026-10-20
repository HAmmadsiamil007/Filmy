# Suggested Commands

Windows/PowerShell notes baked in.

## Backend
```powershell
# one-time setup
python -m venv .venv
.\.venv\Scripts\pip.exe install -r requirements.txt

# run on port 8000
.\.venv\Scripts\uvicorn.exe api:app --host 127.0.0.1 --port 8000

# kill a running server (no pkill on Windows)
Get-CimInstance Win32_Process -Filter "Name like "%python%"" |
  Where-Object { $_.CommandLine -match "uvicorn" } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## Frontend
```powershell
Set-Location web
npm install
npm run dev      # vite on 5173, proxies /api /proxy /search /movies to 127.0.0.1:8000
npm run build    # tsc -b && vite build -> web/dist
npx tsc -b       # type-check only
```

## Smoke tests
```powershell
Invoke-WebRequest "http://127.0.0.1:8000/health" -UseBasicParsing
Invoke-WebRequest "http://127.0.0.1:8000/search?q=Avatar" -UseBasicParsing
```

## Gotchas
- Measure-Command { Invoke-WebRequest ... } to time cache cold vs warm.
- .NET HttpWebRequest throws on 4xx/5xx - wrap in try/catch and read $_.Exception.Response.StatusCode.
- StreamReader.ReadToEnd() on a video stream errors - the payload is binary.

## Tests + docs checks (run before claiming done)
```powershell
# 129 tests, no network, ~1.5s
.\.venv\Scripts\pip.exe install -r requirements-dev.txt   # once
.\.venv\Scripts\pytest.exe tests -q

# documentation link integrity (wikilinks + relative links)
python scripts\check_wikilinks.py
python scripts\check_wikilinks.py --verbose    # list every link

# the same three things CI runs (.github/workflows/ci.yml)
pytest tests -q ; cd web ; npx tsc -b ; npm run build ; cd .. ; python scripts\check_wikilinks.py
```

## CI
.github/workflows/ci.yml = 3 jobs: backend (import check + pytest),
frontend (npm ci + tsc + vite build), docs (link check). Python 3.14, Node 24.
No Tauri job (needs Rust) and NO Android job yet (never compiled - would go red).