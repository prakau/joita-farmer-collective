package ai.joita.biosoil.collective

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID
import java.security.MessageDigest
import org.json.JSONObject

object CollectivePhotos {
    /** Bound both input size and decoded pixel memory before accepting external images. */
    fun import(context: Context, uri: Uri, origin: String = "Selected image"): String {
        val source = File.createTempFile("joita-photo-", ".image", context.cacheDir)
        try {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                source.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var size = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= 25L * 1024 * 1024) { "Choose a photo smaller than 25 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val bitmap = requireNotNull(decode(source.absolutePath, 1600)) { "This file is not a readable image" }
            val dir = File(context.filesDir, "field-photos").apply { mkdirs() }
            val result = File(dir, "${UUID.randomUUID()}.jpg")
            try {
                result.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it)) }
                source.copyTo(File(result.absolutePath + ".original"))
                val metadata = JSONObject().put("source", origin).put("savedAtDeviceTime", System.currentTimeMillis())
                    .put("originalSha256", sha256(source)).put("previewSha256", sha256(result))
                File(result.absolutePath + ".json").writeText(metadata.toString(2))
            } catch (e: Exception) { result.delete(); throw e } finally { bitmap.recycle() }
            return result.absolutePath
        } finally { source.delete() }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val size = input.read(buffer); if (size < 0) break; digest.update(buffer, 0, size) }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun decode(path: String, maxSize: Int = 900): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > maxSize) sample *= 2
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val orientation = runCatching { ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        val matrix = Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> { setRotate(180f); postScale(-1f, 1f) }
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(-90f); postScale(-1f, 1f) }
                8 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { if (it != bitmap) bitmap.recycle() }
    }
}
