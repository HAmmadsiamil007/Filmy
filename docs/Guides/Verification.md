---
tags: [guides, verification, checklist]
status: active
---

# Verification Checklist

> [!abstract] Rule
> **Evidence before assertions, always.** Every "it works" claim below was earned by running the command, not by reading the code.

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
