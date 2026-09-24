package ai.joita.biosoil.collective

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A user-exported evidence copy, not cloud sync or a restore format. */
object CollectiveEvidence {
    fun create(context: Context, farmer: Farmer, fields: List<FarmField>, visits: Map<Long, List<FieldVisit>>, pdf: File, impacts: List<ImpactAssessment> = emptyList()): File {
        val file = File(pdf.parentFile, "JOITA-evidence-${farmer.id}-${UUID.randomUUID().toString().take(8)}.zip")
        fun photoName(path: String) = if (path.isBlank()) "" else "photos/${File(path).name}"
        val profile = JSONObject(mapOf(
            "id" to farmer.id, "name" to farmer.name, "phone" to farmer.phone, "village" to farmer.village,
            "latitude" to farmer.latitude, "longitude" to farmer.longitude, "leadFarmer" to farmer.leadFarmer,
            "notes" to farmer.notes, "tenure" to farmer.tenure, "createdAtDeviceTime" to farmer.createdAt,
            "photo" to photoName(farmer.photoPath), "recordedBy" to farmer.recordedBy,
            "acknowledgedAtDeviceTime" to farmer.acknowledgedAt,
        ))
        val rows = JSONArray()
        fields.forEach { field ->
            val entries = JSONArray()
            visits[field.id].orEmpty().forEach { v -> entries.put(JSONObject(mapOf(
                "id" to v.id, "fieldId" to v.fieldId, "date" to v.date, "officer" to v.officer, "cropStage" to v.cropStage,
                "observations" to v.observations, "issues" to v.issues, "recommendations" to v.recommendations,
                "yieldData" to v.yieldData, "notes" to v.notes, "photo" to photoName(v.photoPath), "createdAtDeviceTime" to v.createdAt,
            ))) }
            rows.put(JSONObject(mapOf(
                "id" to field.id, "farmerId" to field.farmerId, "acreage" to field.acreage, "crop" to field.crop,
                "variety" to field.variety, "season" to field.season, "sowingDate" to field.sowingDate,
                "soilType" to field.soilType, "irrigation" to field.irrigation, "inputs" to field.inputs,
                "boundaryNotes" to field.boundaryNotes, "createdAtDeviceTime" to field.createdAt, "visits" to entries,
            )))
        }
        val missing = JSONArray()
        val root = JSONObject().put("formatVersion", 1).put("appVersion", "1.1.0")
            .put("exportedAtDeviceTime", System.currentTimeMillis()).put("farmer", profile).put("fields", rows).put("missingPhotos", missing)
        root.put("impactAssessments", JSONArray().apply { impacts.forEach { a -> put(JSONObject(mapOf(
            "id" to a.id, "farmerId" to a.farmerId, "date" to a.date, "officer" to a.officer,
            "answers" to JSONObject(a.answers), "photos" to JSONObject(a.photos.mapValues { photoName(it.value) }),
            "consentedAtDeviceTime" to a.consentedAt, "createdAtDeviceTime" to a.createdAt,
        ))) } })
        root.put("impactQuestionLabels", JSONObject(ImpactSchema.sections.flatMap { it.questions }.associate { it.key to it.label }))
        root.put("impactConsentStatement", ImpactSchema.consentText)
        try {
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                fun entry(name: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
                val checksums = StringBuilder()
                fun copy(name: String, source: File) {
                    zip.putNextEntry(ZipEntry(name))
                    source.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    checksums.append(CollectivePhotos.sha256(source)).append("  ").append(name).append('\n')
                }
                copy("farmer-report.pdf", pdf)
                (listOf(farmer.photoPath) + visits.values.flatten().map { it.photoPath } + impacts.flatMap { it.photos.values }).filter { it.isNotBlank() }.distinct().forEach { path ->
                    val photo = File(path)
                    val dir = File(context.filesDir, "field-photos").canonicalFile
                    if (photo.canonicalFile.parentFile == dir && photo.isFile) {
                        copy(photoName(path), photo)
                        listOf(".original", ".json").forEach { suffix ->
                            val extra = File(path + suffix)
                            if (extra.isFile) copy(photoName(path) + suffix, extra)
                        }
                    } else missing.put(photoName(path))
                }
                val recordsBytes = root.toString(2).toByteArray(Charsets.UTF_8)
                entry("records.json", recordsBytes)
                checksums.append(java.security.MessageDigest.getInstance("SHA-256").digest(recordsBytes).joinToString("") { "%02x".format(it) }).append("  records.json\n")
                entry("SHA256SUMS.txt", checksums.toString().toByteArray())
                entry("READ-ME.txt", """
                    JOITA FARMER EVIDENCE EXPORT
                    Contains the current profile, linked fields, visits, PDF and available photos.
                    records.json holds entered details and device timestamps.
                    A .jpg is a resized copy. A .jpg.original preserves the original photo bytes
                    received from the camera or picker. Consult the .jpg.json sidecar for source,
                    saved time and SHA-256 hashes. Older photos may not have originals or
                    sidecars. Missing photos are listed in records.json.
                    Hashes detect later file changes; they do not establish authenticity.
                    Acknowledgement is recorded by the officer, not an electronic signature.
                    Device clocks and entered GPS may be incorrect. Records are editable.
                    This archive is not independent verification or carbon certification.
                    Keep it securely and confirm your program's required evidence.
                    Import/restore into the app is not available in this version.
                """.trimIndent().toByteArray())
            }
            return file
        } catch (e: Exception) { file.delete(); throw e }
    }
}
