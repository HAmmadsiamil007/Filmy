package com.moviebox.app.player

import com.moviebox.app.data.Stream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the adaptive/manual quality logic.
 *
 * This is the highest-value test target in the app: QualityLogic is pure
 * (no Android imports), it is a direct port of web/src/Player.tsx, and it
 * encodes the two bugs found during web development — the missing step-up
 * gate and the mislabelled hevc "MP4" fragment. Both are pinned here so a
 * refactor cannot silently reintroduce them.
 *
 * Run:  cd android && ./gradlew :app:testDebugUnitTest
 */
class QualityLogicTest {

    // ---- Fixtures --------------------------------------------------------

    private fun stream(
        qualityValue: Int,
        sizeMb: Double,
        codec: String = "h264",
        format: String = "MP4",
        url: String = "https://cdn.example/$qualityValue-$sizeMb.mp4",
    ) = Stream(
        source = "web",
        format = format,
        quality = "${qualityValue}p",
        qualityValue = qualityValue,
        resolutionTag = "${qualityValue}p",
        url = url,
        sizeMb = sizeMb,
        codec = codec,
        headers = mapOf("Referer" to "https://example.com/"),
    )

    /** Best-quality-first, as produced by dedupe(). */
    private fun ladder() = listOf(
        stream(1080, 4900.0),
        stream(720, 1800.0),
        stream(480, 700.0),
        stream(360, 300.0),
    )

    // ---- dedupe: the mislabelled-fragment guard --------------------------

    @Test
    fun `dedupe removes hevc fragment masquerading as MP4 at same quality`() {
        // The exact upstream bug: 0.8MB hevc DASH fragment labelled format=MP4
        // sitting beside the real 4.9GB h264 file, both quality_value=1080.
        val raw = listOf(
            stream(1080, 4900.0, codec = "h264"),
            stream(1080, 0.8, codec = "hevc", format = "MP4"), // the impostor
            stream(720, 1800.0, codec = "h264"),
        )

        val result = QualityLogic.dedupe(raw)

        val at1080 = result.filter { it.qualityValue == 1080 }
        assertEquals("1080p must be deduped to exactly one entry", 1, at1080.size)
        assertEquals("h264 full file must win over the hevc fragment",
            "h264", at1080.first().codec)
        assertEquals("full-size file must win", 4900.0, at1080.first().sizeMb, 0.001)
    }

    @Test
    fun `dedupe keeps one entry per distinct quality rung`() {
        val raw = listOf(
            stream(1080, 4900.0),
            stream(720, 1800.0),
            stream(480, 700.0),
        )
        val result = QualityLogic.dedupe(raw)
        assertEquals(3, result.size)
        // Sorted best-first.
        assertEquals(1080, result.first().qualityValue)
        assertEquals(480, result.last().qualityValue)
    }

    @Test
    fun `dedupe drops entries with blank url`() {
        val raw = listOf(
            stream(1080, 4900.0),
            stream(720, 1800.0, url = ""),
        )
        val result = QualityLogic.dedupe(raw)
        assertEquals(1, result.size)
        assertTrue(result.none { it.url.isBlank() })
    }

    @Test
    fun `dedupe on empty input returns empty`() {
        assertTrue(QualityLogic.dedupe(emptyList()).isEmpty())
    }

    // ---- pickForBandwidth: low buffer -> step DOWN -----------------------

    @Test
    fun `critically low buffer sheds one rung even when bandwidth looks fine`() {
        val r = ladder()
        // mbps says 1080 is affordable, but buffer is starving.
        val target = QualityLogic.pickForBandwidth(
            renditions = r, mbps = 50.0, bufferSec = 5.0, currentIndex = 0,
        )
        assertEquals("must drop from 1080 -> 720", 1, target)
    }

    @Test
    fun `low buffer at the lowest rung does not run off the end`() {
        val r = ladder()
        val target = QualityLogic.pickForBandwidth(
            renditions = r, mbps = 0.5, bufferSec = 2.0, currentIndex = r.lastIndex,
        )
        assertEquals("stays at last index, never out of bounds", r.lastIndex, target)
    }

    // ---- pickForBandwidth: step-UP gate (the web bug) --------------------

    @Test
    fun `step up is held when buffer is below GOOD_BUFFER_SEC`() {
        val r = ladder()
        // Bandwidth can afford 1080 (index 0) but buffer is only 20s — below
        // GOOD_BUFFER_SEC (30) — so we must hold at the current 720 (index 1).
        val target = QualityLogic.pickForBandwidth(
            renditions = r, mbps = 50.0, bufferSec = 20.0, currentIndex = 1,
        )
        assertEquals("must NOT climb while buffer is unhealthy", 1, target)
    }

    @Test
    fun `step up is allowed once buffer is healthy`() {
        val r = ladder()
        val target = QualityLogic.pickForBandwidth(
            renditions = r, mbps = 50.0, bufferSec = 45.0, currentIndex = 1,
        )
        assertEquals("climbs 720 -> 1080 when buffer >= GOOD_BUFFER_SEC", 0, target)
    }

    // ---- pickForBandwidth: HEADROOM respected ----------------------------

    @Test
    fun `does not climb above what headroom allows`() {
        val r = ladder()
        // 1080 needs ~6.0 mbps * 1.35 headroom = 8.1. 8.0 mbps is just under,
        // so 1080 is unaffordable but 720 (4.0*1.35=5.4) is fine.
        val mbps = 8.0
        val target = QualityLogic.pickForBandwidth(
            renditions = r, mbps = mbps, bufferSec = 60.0, currentIndex = 2,
        )
        assertTrue("target must not be index 0 (1080 unaffordable)", target > 0)
    }

    @Test
    fun `neededMbps scales with quality_value`() {
        assertEquals(1080 / QualityLogic.PIXEL_TO_MBIT,
            QualityLogic.neededMbps(1080), 1e-9)
        assertTrue(QualityLogic.neededMbps(1080) > QualityLogic.neededMbps(360))
    }

    // ---- unknown bandwidth: buffer-only path -----------------------------

    @Test
    fun `unknown bandwidth with healthy buffer probes exactly one rung up`() {
        val r = ladder()
        // mbps = 0 (unknown), buffer 45s >= GOOD_BUFFER_SEC -> probe cur-1.
        val target = QualityLogic.pickForBandwidth(
            renditions = r, mbps = 0.0, bufferSec = 45.0, currentIndex = 2,
        )
        assertEquals("moves 480 -> 720, exactly one rung", 1, target)
    }

    @Test
    fun `unknown bandwidth with critical buffer sheds one rung`() {
        val r = ladder()
        // mbps = 0 but buffer is starving (< MIN_BUFFER_SEC) -> the low-buffer
        // rule wins over any probe and we drop cur -> cur+1.
        val target = QualityLogic.pickForBandwidth(
            renditions = r, mbps = 0.0, bufferSec = 5.0, currentIndex = 1,
        )
        assertEquals("drops 720 -> 480", 2, target)
    }

    @Test
    fun `unknown bandwidth never jumps more than one rung`() {
        val r = ladder()
        // Whatever the buffer, an unknown-bandwidth probe must be adjacent.
        for (cur in r.indices) {
            for (buf in listOf(0.0, 5.0, 20.0, 45.0, 120.0)) {
                val t = QualityLogic.pickForBandwidth(r, 0.0, buf, cur)
                assertTrue(
                    "jumped from $cur to $t at buf=$buf",
                    kotlin.math.abs(t - cur) <= 1,
                )
            }
        }
    }

    // ---- invariants ------------------------------------------------------

    @Test
    fun `result always stays within rendition bounds`() {
        val r = ladder()
        val cases = listOf(
            Triple(0.0, 0.0, 0), Triple(1000.0, 0.0, 3),
            Triple(0.1, 100.0, 3), Triple(50.0, 60.0, 0),
        )
        for ((mbps, buf, cur) in cases) {
            val t = QualityLogic.pickForBandwidth(r, mbps, buf, cur)
            assertTrue("target $t out of bounds for mbps=$mbps buf=$buf cur=$cur",
                t in r.indices)
        }
    }

    @Test
    fun `empty rendition list returns current index unchanged`() {
        assertEquals(2, QualityLogic.pickForBandwidth(emptyList(), 50.0, 60.0, 2))
    }

    @Test
    fun `labelFor prefers quality_value over raw tag`() {
        assertEquals("1080p", QualityLogic.labelFor(stream(1080, 100.0)))
    }

    @Test
    fun `hevc detection matches codec spellings`() {
        assertTrue(stream(1080, 1.0, codec = "hevc").isHevc)
        assertTrue(stream(1080, 1.0, codec = "hvc1").isHevc)
        assertTrue(!stream(1080, 1.0, codec = "avc1").isHevc)
    }
}
