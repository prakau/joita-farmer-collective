package ai.joita.biosoil.sensor

import ai.joita.biosoil.model.SoilReading

object SoilProbeProtocol {
    const val FRAME_LENGTH = 19

    fun pollCommand(nonce: Byte): ByteArray = byteArrayOf(
        0x00, 0x04, 0x00, 0x00, 0x00, 0x08, 0xF0.toByte(), nonce,
    )

    fun parseFrame(frame: ByteArray): SoilReading? {
        if (frame.size < FRAME_LENGTH) return null
        return SoilReading(
            moisturePercent = unsigned16(frame, 3) / 10.0,
            temperatureCelsius = signed16(frame, 5) / 10.0,
            ecUsCm = unsigned16(frame, 7),
            ph = unsigned16(frame, 9) / 10.0,
            nitrogenMgKg = unsigned16(frame, 11),
            phosphorusMgKg = unsigned16(frame, 13),
            potassiumMgKg = unsigned16(frame, 15),
            fertilityMgKg = unsigned16(frame, 17),
        )
    }

    private fun unsigned16(bytes: ByteArray, index: Int): Int =
        (probeByte(bytes[index]) shl 8) or probeByte(bytes[index + 1])

    private fun signed16(bytes: ByteArray, index: Int): Int {
        val raw = unsigned16(bytes, index)
        return if (raw and 0x8000 == 0) raw else raw - 0x1_0000
    }

    /** SoilDetector 3.2.0 treats the probe's 0x7F sentinel byte as zero. */
    private fun probeByte(byte: Byte): Int =
        (byte.toInt() and 0xFF).let { if (it == 0x7F) 0 else it }
}

class SoilProbeFrameBuffer {
    private val buffer = ArrayList<Byte>()

    @Synchronized
    fun append(bytes: ByteArray): List<SoilReading> {
        if (bytes.size >= SoilProbeProtocol.FRAME_LENGTH) {
            buffer.clear()
            return listOfNotNull(SoilProbeProtocol.parseFrame(bytes))
        }
        bytes.forEach(buffer::add)
        if (buffer.size < SoilProbeProtocol.FRAME_LENGTH) return emptyList()

        val frame = ByteArray(SoilProbeProtocol.FRAME_LENGTH) { buffer[it] }
        buffer.clear()
        return listOfNotNull(SoilProbeProtocol.parseFrame(frame))
    }
}
