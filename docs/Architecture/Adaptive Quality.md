---
tags: [architecture, player, quality]
status: active
---

# Adaptive Quality — `web/src/Player.tsx`

> [!info] Product decision
> Do **both**: auto-adjust to the user's connection, **and** let the user pin a rung manually.

## Constants

| Constant | Value | Role |
| --- | --- | --- |
| `MIN_BUFFER_SEC` | 12 | below → step **down** |
| `GOOD_BUFFER_SEC` | 30 | step **up** is gated on reaching this |
| `SWITCH_COOLDOWN_MS` | 8000 | anti-oscillation |
| `HEADROOM` | 1.35 | need 35% spare bandwidth above rendition cost |

## Algorithm (Auto)

Renditions sorted `quality_value` desc — index `0` = highest.

```
pickForBandwidth(mbps, bufferSec, currentIdx):
  neededMbps = quality_value / 180          # rough pixel→bitrate proxy
  choose highest rendition where neededMbps * HEADROOM <= mbps
  if bufferSec < MIN_BUFFER_SEC:      drop one rung
  if choice is a step UP
     and bufferSec < GOOD_BUFFER_SEC: hold at currentIdx
```

Manual pick overrides Auto (`modeRef`) and pins until the user returns to Auto. Playhead preserved across switches via `loadRendition(idx, resumeAt)`.

## Two instructive bugs

> [!bug] 1 — unused constant meant an unwired gate
> `GOOD_BUFFER_SEC` was **declared but never used**: the step-**up** gate was never wired, so the player oscillated — climb on a good sample → buffer drains → drop → repeat. `tsc` caught it via `noUnusedLocals`.
>
> **Fix:** wire it in. Do **not** delete the constant.

> [!bug] 2 — upstream mislabels a DASH fragment as MP4
> The list contained a **duplicate `1080p`**: a `0.8 MB` **hevc** DASH fragment with `format:"MP4"`, sitting beside the real `4.9 GB` **h264** file, both `quality_value=1080`.
>
> Symptoms: duplicate picker labels · duplicate React keys · a stream that **dies after ~1s**.
>
> **Fix:** dedupe by `quality_value` keeping largest `size_mb`, and prefer non-hevc (hevc won't decode in Chrome/Firefox).

## Invariants

> [!danger] Never regress these
> - Media event handlers **must** be `useCallback`-stable. Re-creating them re-fires `loadedmetadata` and **stalls playback**.
> - `modeRef` / `lastSwitchRef` / `bytesRef` are **refs, not state** — state gives stale closures inside `onProgress` / `onWaiting`.
> - Always sanity-check a rendition is a real full MP4. **Never trust the `format` field alone** — see [[Guides/Debugging Upstream]] § mislabelling.

## Known gap

`hls.js` is installed but **unwired** — the player is MP4-only today. DASH manifests are ignored. Tracked in [[Roadmap/Backlog]].

## Related

→ [[Architecture/Stream Proxy]] · [[Architecture/Client Strategy]] · [[Guides/Verification]]
