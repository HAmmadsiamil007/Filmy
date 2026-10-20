---
tags: [guides, verification, checklist]
status: active
---

# Verification Checklist

> [!abstract] Rule
> **Evidence before assertions, always.** Every "it works" claim below was earned by running the command, not by reading the code.

## 0. The gate — run all three, all must exit 0

```powershell
.\.venv\Scripts\pytest.exe tests -q          # 129 tests, no network, ~1.5s

cd web; npx tsc -b; npm run build; cd ..     # type-check + production build

python scripts\check_wikilinks.py            # 53 files, 219 links, 0 broken
```

CI runs exactly these three on every push and PR
(`.github/workflows/ci.yml`). **Local green means CI green.**

If you could not run a check, say which one and why — do not round it up to "passing".

## 1. Backend change

```powershell
.venv\Scripts\python.exe -c "import api; print('IMPORT OK')"
```

Then **restart** (uvicorn does not hot-reload unless `--reload`) and hit:

- [ ] `/health` → `{"status":"ok"…}`
- [ ] `/search?q=Avatar` → `200`, results present
- [ ] `/api/stream/<known id>` → `200`, streams present

## 2. Proxy change — assert Range, not just 200

> [!danger] A bare `200` does NOT prove seeking works
> Request a byte range and expect **`206` + `Content-Range`**. See [[Architecture/Stream Proxy]].

```powershell
$req = [System.Net.HttpWebRequest]::Create("<proxy url>")
$req.AddRange(0, 2047)
$resp = $req.GetResponse()
[int]$resp.StatusCode            # expect 206
$resp.Headers['Content-Range']   # expect bytes 0-2047/<total>
```

## 3. Frontend change

```powershell
Set-Location web; npx tsc -b; npm run build
```

- [ ] `tsc` exits 0 — catches unused-constant regressions (it flagged `GOOD_BUFFER_SEC`)
- [ ] `vite build` exits 0 — currently 33 modules, ~1.4s, 150 kB / 48 kB gz

## 4. Full-stack check

Both servers up, then fetch video bytes **through the Vite proxy chain** (`http://127.0.0.1:5173/proxy/stream?…`) and expect `206`. Reference script: [[Reference/Commands]] § Full-stack video check.

## 5. Player change — behavioural

- [ ] Switching quality **preserves the playhead**
- [ ] Quality picker shows **no duplicate labels**
- [ ] Manual pick **pins** until Auto is reselected
- [ ] No `loadedmetadata` re-fire stall when handlers update

Logic under test: [[Architecture/Adaptive Quality]].

## 6. Docs change — link integrity

> [!tip] Now automated
> `scripts/check_wikilinks.py` validates every `[[wikilink]]` **and** every
> relative markdown link across the whole repo, ignoring prose mentions in
> backticks. CI runs it.

```powershell
python scripts\check_wikilinks.py
python scripts\check_wikilinks.py --verbose   # list every link found
```

- [ ] exits `0`
- [ ] any new note is **reachable** from `Home.md` or another index note
- [ ] both stores updated if behaviour changed (§ Memory sync below)

Adding a root file (`rules`, `design`, `architecture`, `AGENTS`)? It is checked
too — the script is repo-wide, not `docs/`-only.

## 7. Android change — static checks only

No Android SDK here, so Gradle cannot run. Do the checks that *can* run, and
state the result as **uncompiled**:

- [ ] every `.kt` package declaration matches its file path
- [ ] import sweep: each used symbol (`.background(`, `mutableLongStateOf`, …) has an import in that file
- [ ] third-party APIs looked up, not recalled
- [ ] when a toolchain exists: `.\gradlew.bat assembleDebug` + `testDebugUnitTest`

## Memory sync

> [!tip] Two stores, must stay in sync
> After any backend or player change, update **both**:
>
> 1. **Serena** — agent-facing memory graph, root `mem:core`. Edit with `write_memory` / `edit_memory`.
> 2. **This vault** — human-facing mirror (`docs/`).
>
> Cross-check references: `serena memories check` from the project root.

## Related

→ [[Guides/Debugging Upstream]] · [[Reference/Commands]] · [[Project/Status]]
