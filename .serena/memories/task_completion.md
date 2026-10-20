# Task Completion

Run before calling any task done.

## Backend change - RUN THE SUITE FIRST (129 tests, ~1.6s, no network)
```powershell
.\.venv\Scripts\pytest.exe tests -q     # must exit 0
```
See mem:backend/tests for what is covered vs what still needs the live server.

## Backend change - manual checks
```powershell
.\.venv\Scripts\python.exe -c "import api; print('IMPORT OK')"
```
Then restart the server (uvicorn does not hot-reload unless --reload) and hit:
- /health returns {"status":"ok"...}
- /search?q=Avatar returns 200 with results
- /api/stream/<known id> returns 200 with streams

## Proxy change - must verify Range, not just 200
A bare 200 does not prove seeking works. Request a byte range and expect 206 + Content-Range:
```powershell
$req = [System.Net.HttpWebRequest]::Create("<proxy url>")
$req.AddRange(0, 2047); $resp = $req.GetResponse()
# expect [int]$resp.StatusCode -eq 206
```

## Frontend change
```powershell
Set-Location web; npx tsc -b; npm run build
```
Both must exit 0. tsc catches unused-constant regressions (it flagged GOOD_BUFFER_SEC going unused).

## Full-stack check
Dev server and backend both up, then fetch video bytes through the Vite proxy chain (http://127.0.0.1:5173/proxy/stream?...) and expect 206.

## Android change (no toolchain in this env - see mem:architecture/android_build)
This env has NO Android SDK/Gradle/ANDROID_HOME. Android sources are hand-reviewed only.
Before claiming an Android change works, be explicit that it is UNCOMPILED.

Static checks that DO run here (they catch real defects):
- package declaration must match file path for every .kt
  (expected = path after src/main/java|test/java, dots for separators)
- import sweep: grep each .kt for symbol usage (.background(, .clip(,
  mutableLongStateOf, AsyncImage, LazyColumn...) and confirm a matching
  import line exists in that same file.

When a toolchain IS available:
    cd android
    .\gradlew.bat assembleDebug      # first run WILL surface compile errors
    .\gradlew.bat testDebugUnitTest  # QualityLogicTest, pure JVM, no device

## Third-party API usage - verify, do not guess
Rendered in this project already: a guessed PlaybackException.errorMessage (does not exist),
a guessed AnalyticsListener signature, and invalid CIDR in network_security_config.xml.
Look up library signatures (context7) before using an API not in the source tree.