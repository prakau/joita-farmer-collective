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
    fun parser_rejectsShortAndImpossibleFrames() {
        assertNull(SoilProbeProtocol.parseFrame(ByteArray(18)))
        val impossiblePh = ByteArray(19).apply {
            this[9] = 0x00
            this[10] = 0xFF.toByte()
        }
        assertNull(SoilProbeProtocol.parseFrame(impossiblePh))
    }

    @Test
    fun buffer_waitsForACompleteFrame() {
        val frame = byteArrayOf(1, 3, 16, 0, 200.toByte(), 1, 14, 1, 44, 0, 65, 0, 140.toByte(), 0, 25, 0, 180.toByte(), 1, 244.toByte())
        val buffer = SoilProbeFrameBuffer()
        assertTrue(buffer.append(frame.copyOfRange(0, 8)).isEmpty())
        assertEquals(1, buffer.append(frame.copyOfRange(8, frame.size)).size)
    }
}

