# Player Quality Logic (web/src/Player.tsx)

Adaptive AND manual, per product decision: auto-adjust to connection, but let the user pin a rung.

## Constants
- MIN_BUFFER_SEC 12 - below this, step DOWN.
- GOOD_BUFFER_SEC 30 - step UP is gated on reaching this.
- SWITCH_COOLDOWN_MS 8000 - anti-oscillation.
- HEADROOM 1.35 - need 35% spare bandwidth above the rendition cost.

## Algorithm (Auto)
renditions sorted by quality_value desc. Index 0 = highest.
pickForBandwidth(mbps, bufferSec, currentIdx):
  - neededMbps = quality_value / 180 (rough pixel->bitrate proxy).
  - choose the HIGHEST rendition where neededMbps * HEADROOM <= mbps.
  - if bufferSec < MIN_BUFFER_SEC, drop one rung.
  - if the choice is a step UP but bufferSec < GOOD_BUFFER_SEC, hold at currentIdx.
Manual pick overrides Auto (modeRef) and pins until the user returns to Auto. Playhead is preserved across switches via loadRendition(idx, resumeAt).

## Two bugs found here - both instructive
1. GOOD_BUFFER_SEC was declared but unused: the step-UP gate was never wired, so the player oscillated (climb on a good sample -> buffer drains -> drop -> repeat). tsc flagged it via noUnusedLocals. FIX: wire it in, do not delete the constant.
2. Rendition list contained a duplicate 1080p: upstream returns a 0.8 MB hevc DASH fragment MISLABELLED format="MP4" next to the real 4.9 GB h264 file, both quality_value=1080. Symptoms: duplicate picker labels, duplicate React keys, and a stream that dies after ~1s. FIX: dedupe by quality_value keeping the largest size_mb, and prefer non-hevc (hevc will not decode in Chrome/Firefox).

## Invariants
- Media event handlers MUST be useCallback-stable; re-creating them re-fires loadedmetadata and stalls playback.
- modeRef/lastSwitchRef/bytesRef are refs, not state - state would give stale closures inside onProgress/onWaiting.
- Always verify a rendition is actually a full MP4 before exposing it; never trust the format field alone.