package ai.joita.biosoil.sensor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoilProbeProtocolTest {
    @Test
    fun pollCommand_matchesOriginalProbeProtocol() {
        assertArrayEquals(
            byteArrayOf(0, 4, 0, 0, 0, 8, 0xF0.toByte(), 0x2A),
            SoilProbeProtocol.pollCommand(0x2A),
        )
    }

    @Test
    fun parser_decodesAllEightMetricsAndSignedTemperature() {
        val frame = byteArrayOf(
            1, 3, 16,
            0x01, 0x82.toByte(), // moisture 38.6
            0xFF.toByte(), 0x85.toByte(), // temperature -12.3
            0x02, 0x6C, // EC 620
            0x00, 0x44, // pH 6.8
            0x00, 0xA8.toByte(), // N 168
            0x00, 0x1F, // P 31
            0x00, 0xC0.toByte(), // K 192
            0x02, 0x03, // fertility 515
        )

        val reading = requireNotNull(SoilProbeProtocol.parseFrame(frame))
        assertEquals(38.6, reading.moisturePercent, 0.01)
        assertEquals(-12.3, reading.temperatureCelsius, 0.01)
        assertEquals(620, reading.ecUsCm)
        assertEquals(6.8, reading.ph, 0.01)
        assertEquals(168, reading.nitrogenMgKg)
        assertEquals(31, reading.phosphorusMgKg)
        assertEquals(192, reading.potassiumMgKg)
        assertEquals(515, reading.fertilityMgKg)
    }

    @Test
    fun parser_rejectsOnlyShortFramesLikeOriginalApp() {
        assertNull(SoilProbeProtocol.parseFrame(ByteArray(18)))
        val outOfRangePh = ByteArray(19).apply {
            this[9] = 0x00
            this[10] = 0xFF.toByte()
        }
        assertEquals(25.5, requireNotNull(SoilProbeProtocol.parseFrame(outOfRangePh)).ph, 0.01)
    }

    @Test
    fun parser_normalizesProbeSentinelBytesLikeOriginalApp() {
        val sentinelFrame = byteArrayOf(
            1, 3, 16,
            0x7F, 0x7F,
            0x7F, 0x7F,
            0x7F, 0x7F,
            0x7F, 0x7F,
            0x7F, 0x7F,
            0x7F, 0x7F,
            0x7F, 0x7F,
            0x7F, 0x7F,
        )

        val reading = requireNotNull(SoilProbeProtocol.parseFrame(sentinelFrame))
        assertEquals(0.0, reading.moisturePercent, 0.01)
        assertEquals(0.0, reading.temperatureCelsius, 0.01)
        assertEquals(0, reading.ecUsCm)
        assertEquals(0.0, reading.ph, 0.01)
        assertEquals(0, reading.nitrogenMgKg)
        assertEquals(0, reading.phosphorusMgKg)
        assertEquals(0, reading.potassiumMgKg)
        assertEquals(0, reading.fertilityMgKg)
    }

    @Test
    fun buffer_waitsForACompleteFrame() {
        val frame = byteArrayOf(1, 3, 16, 0, 200.toByte(), 1, 14, 1, 44, 0, 65, 0, 140.toByte(), 0, 25, 0, 180.toByte(), 1, 244.toByte())
        val buffer = SoilProbeFrameBuffer()
        assertTrue(buffer.append(frame.copyOfRange(0, 8)).isEmpty())
        assertEquals(1, buffer.append(frame.copyOfRange(8, frame.size)).size)
    }

    @Test
    fun buffer_discardsTrailingPacketBytesBeforeNextCallback() {
        val first = byteArrayOf(1, 3, 16, 0, 200.toByte(), 1, 14, 0, 100, 0, 65, 0, 140.toByte(), 0, 25, 0, 180.toByte(), 1, 244.toByte())
        val second = byteArrayOf(1, 3, 16, 1, 130.toByte(), 0, 245.toByte(), 0, 100, 0, 68, 0, 168.toByte(), 0, 31, 0, 192.toByte(), 2, 3)
        val buffer = SoilProbeFrameBuffer()

        assertEquals(1, buffer.append(first + byteArrayOf(0, 0)).size)
        val secondReading = buffer.append(second).single()

        assertEquals(38.6, secondReading.moisturePercent, 0.01)
        assertEquals(24.5, secondReading.temperatureCelsius, 0.01)
        assertEquals(100, secondReading.ecUsCm)
        assertEquals(6.8, secondReading.ph, 0.01)
    }
}
