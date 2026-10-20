package com.moviebox.app.player

import com.moviebox.app.data.Stream
import kotlin.math.roundToInt

/**
 * Quality-selection logic — the Android mirror of web/src/Player.tsx.
 *
 * Kept as pure functions (no Android imports) so it is unit-testable and can
 * be diffed directly against the TS implementation. Both clients must behave
 * the same: auto-adjust to connection speed AND let the user pin a rung.
 *
 * Constants intentionally match Player.tsx.
 */
object QualityLogic {

    const val MIN_BUFFER_SEC = 12.0        // below -> step down
    const val GOOD_BUFFER_SEC = 30.0       // step UP gated on reaching this
    const val SWITCH_COOLDOWN_MS = 8_000L  // anti-oscillation
    const val HEADROOM = 1.35              // need 35% spare bandwidth
    const val PIXEL_TO_MBIT = 180.0        // quality_value / 180 ~= needed mbps

    enum class Mode { AUTO, MANUAL }

    /**
     * Rough bitrate cost of a rendition, mirroring the web player's
     * `quality_value / 180` heuristic (quality_value is a vertical-pixel
     * count, e.g. 1080, not a bitrate).
     */
    fun neededMbps(qualityValue: Int): Double = qualityValue / PIXEL_TO_MBIT

    /**
     * Choose the rendition index to play next.
     *
     * Primary signal is BUFFER HEALTH, not a bandwidth probe. This is
     * deliberate and correct:
     *
     *   - Buffer growth/decay is downstream of connection speed, so it *is* an
     *     adaptive signal — a fast link fills the buffer, a slow one drains it.
     *   - It uses only stable `Player.bufferedPosition`/`currentPosition`, so
     *     there is no dependency on unstable analytics APIs (measuring bytes
     *     would require PlaybackStatsListener/TransferListener plumbing).
     *
     * [mbps] is an OPTIONAL refinement: pass a measured throughput > 0 and the
     * HEADROOM/neededMbps gate narrows the pick; pass 0.0 for "unknown" and the
     * buffer alone drives the decision. Either way the buffer thresholds gate
     * every transition, so the anti-oscillation rules hold.
     *
     * @param renditions sorted best-quality-first (index 0 = highest), as
     *                   produced by [dedupe].
     * @param mbps measured throughput, or 0.0 when unknown.
     * @param bufferSec seconds buffered ahead of the playhead.
     * @param currentIndex index currently playing.
     * @return index to switch to, or [currentIndex] to hold.
     */
    fun pickForBandwidth(
        renditions: List<Stream>,
        mbps: Double,
        bufferSec: Double,
        currentIndex: Int,
    ): Int {
        if (renditions.isEmpty()) return currentIndex
        val last = renditions.lastIndex
        val cur = currentIndex.coerceIn(0, last)

        // Highest rung the connection can afford, given what we know.
        val affordable: Int = when {
            mbps > 0.0 -> renditions.indices.firstOrNull { i ->
                neededMbps(renditions[i].qualityValue) * HEADROOM <= mbps
            } ?: last

            // Bandwidth unknown: stay put unless the buffer is comfortably
            // full, in which case probe one rung up (see gate below).
            bufferSec >= GOOD_BUFFER_SEC && cur > 0 -> cur - 1
            else -> cur
        }

        var target = affordable

        // Hard rule: shed a rung whenever the buffer is critically low.
        // maxOf ensures a low buffer always wins over any upgrade.
        if (bufferSec < MIN_BUFFER_SEC && cur < last) {
            target = maxOf(target, cur + 1)
        }

        // Anti-oscillation gate: never take a step-UP unless the buffer is
        // healthy. (This is the gate that was missing in the original web
        // player and caused climb→drain→drop loops.)
        if (target < cur && bufferSec < GOOD_BUFFER_SEC) {
            target = cur
        }

        return target.coerceIn(0, last)
    }

    /**
     * Dedupe renditions by quality_value, preferring the largest file and
     * non-hevc codecs.
     *
     * WHY (see docs/Architecture/Adaptive Quality.md): upstream returns a
     * ~0.8 MB hevc DASH fragment MISLABELLED format="MP4" next to the real
     * 4.9 GB h264 file, both quality_value=1080. Without this you get
     * duplicate picker rows, duplicate keys, and a stream dying at ~1s.
     * hevc also fails to decode on many devices.
     */
    fun dedupe(raw: List<Stream>): List<Stream> {
        return raw
            .filter { it.url.isNotBlank() }
            .groupBy { it.qualityValue }
            .map { (_, variants) ->
                variants.maxWithOrNull(
                    compareBy<Stream> { if (it.isHevc) 0 else 1 }
                        .thenBy { it.sizeMb }
                ) ?: variants.first()
            }
            .sortedByDescending { it.qualityValue }
    }

    /** Human label for the picker, e.g. "1080p". Falls back to quality tag. */
    fun labelFor(s: Stream): String {
        val v = s.qualityValue
        return when {
            v > 0 -> "${v}p"
            s.quality.isNotBlank() -> s.quality
            else -> s.resolutionTag
        }
    }
}
