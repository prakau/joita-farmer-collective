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
        ).takeIf { it.isPlausible() }
    }

    private fun unsigned16(bytes: ByteArray, index: Int): Int =
        ((bytes[index].toInt() and 0xFF) shl 8) or (bytes[index + 1].toInt() and 0xFF)

    private fun signed16(bytes: ByteArray, index: Int): Int =
        (bytes[index].toInt() shl 8) or (bytes[index + 1].toInt() and 0xFF)

    private fun SoilReading.isPlausible(): Boolean =
        moisturePercent in 0.0..100.0 &&
            temperatureCelsius in -50.0..100.0 &&
            ph in 0.0..14.0 &&
            ecUsCm in 0..65_535
}

class SoilProbeFrameBuffer {
    private val buffer = ArrayList<Byte>()

    @Synchronized
    fun append(bytes: ByteArray): List<SoilReading> {
        bytes.forEach(buffer::add)
        val readings = mutableListOf<SoilReading>()
        while (buffer.size >= SoilProbeProtocol.FRAME_LENGTH) {
            val candidate = ByteArray(SoilProbeProtocol.FRAME_LENGTH) { buffer[it] }
            val reading = SoilProbeProtocol.parseFrame(candidate)
            if (reading != null) {
                readings += reading
                repeat(SoilProbeProtocol.FRAME_LENGTH) { buffer.removeAt(0) }
            } else {
                buffer.removeAt(0)
            }
        }
        return readings
    }
}
