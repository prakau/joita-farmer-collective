package ai.joita.biosoil.collective

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

object CollectivePdfService {
    fun createFarmerReport(context: Context, farmer: Farmer, fields: List<FarmField>, visits: Map<Long, List<FieldVisit>>): File {
        val reports = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = farmer.name.replace(Regex("[^A-Za-z0-9-]+"), "-").trim('-').take(40).ifBlank { "Farmer" }
        val output = File(reports, "JOITA-$safeName-${farmer.id}-${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()
        val renderer = Renderer(document, farmer)
        renderer.title("Farmer profile")
        renderer.pair("Name", farmer.name)
        renderer.pair("Registration reference", "JOITA-${farmer.id}")
        renderer.pair("Registered on (device time)", DateFormat.getDateTimeInstance().format(Date(farmer.createdAt)))
        renderer.pair("Recorded by", farmer.recordedBy.ifBlank { "Not recorded" })
        renderer.pair("Acknowledgement", if (farmer.acknowledgedAt > 0) "Officer recorded farmer agreement to profile and photo storage on ${DateFormat.getDateTimeInstance().format(Date(farmer.acknowledgedAt))}. Not an electronic signature." else "Not recorded")
        renderer.pair("Village", farmer.village)
        renderer.pair("Phone", farmer.phone.ifBlank { "Not recorded" })
        renderer.pair("Program role", if (farmer.leadFarmer) "Lead farmer" else "Farmer")
        renderer.pair("Land arrangement", farmer.tenure)
        if (farmer.latitude.isNotBlank()) renderer.pair("GPS", "${farmer.latitude}, ${farmer.longitude}")
        if (farmer.notes.isNotBlank()) renderer.paragraph("Family and farming notes", farmer.notes)
        renderer.section("Collective summary")
        renderer.pair("Fields", fields.size.toString())
        renderer.pair("Total acreage", "${formatAcres(fields.sumOf { it.acreage })} acres")
        renderer.pair("Recorded visits", visits.values.sumOf { it.size }.toString())
        renderer.section("Fields (${fields.size})")
        if (fields.isEmpty()) renderer.body("No fields recorded.")
        fields.forEachIndexed { index, field ->
            renderer.fieldHeading("${index + 1}. ${field.crop} - ${formatAcres(field.acreage)} acres")
            renderer.body(listOf(field.variety, field.season, field.sowingDate).filter { it.isNotBlank() }.joinToString(" | ").ifBlank { "Crop details not recorded" })
            renderer.pair("Soil / irrigation", listOf(field.soilType, field.irrigation).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "Not recorded" })
            if (field.inputs.isNotBlank()) renderer.paragraph("Inputs", field.inputs)
            if (field.boundaryNotes.isNotBlank()) renderer.paragraph("Boundary / location", field.boundaryNotes)
            val fieldVisits = visits[field.id].orEmpty()
            renderer.fieldHeading("Visit history (${fieldVisits.size})")
            if (fieldVisits.isEmpty()) renderer.body("No visits recorded for this field.")
            fieldVisits.forEach { visit ->
                renderer.visitHeading("${visit.date} | ${visit.officer}${visit.cropStage.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()}")
                if (visit.observations.isNotBlank()) renderer.paragraph("Observations", visit.observations)
                if (visit.issues.isNotBlank()) renderer.paragraph("Pest / disease", visit.issues)
                if (visit.recommendations.isNotBlank()) renderer.paragraph("Recommendations", visit.recommendations)
                if (visit.yieldData.isNotBlank()) renderer.paragraph("Yield / harvest", visit.yieldData)
                if (visit.notes.isNotBlank()) renderer.paragraph("Notes", visit.notes)
                renderer.pair("Photo evidence", if (visit.photoPath.isNotBlank()) "See photo appendix; availability checked there" else "Not added")
            }
        }
        if (farmer.photoPath.isNotBlank()) renderer.photo("Farmer registration - ${farmer.name}", farmer.photoPath)
        fields.forEach { field ->
            visits[field.id].orEmpty().filter { it.photoPath.isNotBlank() }.forEach { visit ->
                renderer.photo("${field.crop} - ${visit.date} - ${visit.officer}", visit.photoPath)
            }
        }
        renderer.finish()
        FileOutputStream(output).use(document::writeTo)
        document.close()
        return output
    }

    fun share(context: Context, file: File, farmer: Farmer) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "JOITA farmer field report - ${farmer.name}")
            putExtra(Intent.EXTRA_TEXT, "Offline field history exported from Joita Farmer Collective.")
            clipData = ClipData.newRawUri("JOITA farmer report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Save or share PDF").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    private class Renderer(private val document: PdfDocument, private val farmer: Farmer) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var pageNumber = 0
        private var y = 0f

        init { newPage() }

        fun title(text: String) { draw(text, 25f, Color.rgb(18, 92, 42), true, 34f) }
        fun section(text: String) { ensure(48f); y += 13f; draw(text, 17f, Color.rgb(18, 92, 42), true, 27f) }
        fun fieldHeading(text: String) { ensure(55f); y += 8f; wrapped(text, 14f, Color.rgb(35, 139, 42), true) }
        fun visitHeading(text: String) { ensure(50f); y += 6f; wrapped(text, 12f, Color.rgb(122, 78, 45), true) }
        fun body(text: String) { wrapped(text, 10.5f, Color.rgb(35, 42, 38), false) }
        fun pair(label: String, value: String) { wrapped("$label: $value", 10.5f, Color.rgb(35, 42, 38), false) }
        fun paragraph(label: String, value: String) { wrapped("$label: $value", 10.5f, Color.rgb(35, 42, 38), false) }

        fun photo(title: String, path: String) {
            val bitmap = CollectivePhotos.decode(path, 1600) ?: run { section("Photo evidence unavailable"); body(title); return }
            newPage()
            wrapped("FIELD PHOTO", 10f, Color.rgb(35, 139, 42), true)
            wrapped(title, 17f, Color.rgb(18, 92, 42), true)
            val availableWidth = 499f
            val availableHeight = maxOf(40f, 785f - y - 15f)
            val scale = minOf(availableWidth / bitmap.width, availableHeight / bitmap.height)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            canvas.drawBitmap(bitmap, null, RectF(48f, y + 10f, 48f + width, y + 10f + height), paint)
            y += height + 24f
            bitmap.recycle()
        }

        private fun wrapped(text: String, size: Float, color: Int, bold: Boolean) {
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size; this.color = color; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, 499)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(3f, 1f).build()
            var line = 0
            while (line < layout.lineCount) {
                val top = layout.getLineTop(line)
                ensure((layout.getLineBottom(line) - top).toFloat() + 4f)
                var end = line + 1
                while (end < layout.lineCount && layout.getLineBottom(end) - top <= 786f - y) end++
                val height = (layout.getLineBottom(end - 1) - top).toFloat()
                canvas.save()
                canvas.clipRect(48f, y, 547f, y + height)
                canvas.translate(48f, y - top)
                layout.draw(canvas)
                canvas.restore()
                y += height
                line = end
                if (line < layout.lineCount) newPage()
            }
            y += 5f
        }

        private fun draw(text: String, size: Float, color: Int, bold: Boolean, advance: Float) {
            ensure(advance); paint.color = color; paint.textSize = size; paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(text, 48f, y, paint); y += advance
        }

        private fun ensure(height: Float) { if (y + height > 792f) newPage() }
        private fun newPage() {
            finishPage(); pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()).also { canvas = it.canvas }
            paint.color = Color.rgb(255, 249, 237); canvas.drawColor(paint.color)
            paint.color = Color.rgb(35, 139, 42); canvas.drawRect(0f, 0f, 595f, 13f, paint)
            paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = 11f; canvas.drawText("JOITA FARMER COLLECTIVE", 48f, 43f, paint)
            paint.typeface = Typeface.DEFAULT; paint.textSize = 9f; paint.color = Color.DKGRAY
            val header = "JOITA-${farmer.id} | Generated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())}"
            canvas.drawText(TextUtils.ellipsize(header, TextPaint(paint), 499f, TextUtils.TruncateAt.END).toString(), 48f, 60f, paint)
            y = 91f
        }

        private fun finishPage() { page?.let { current -> paint.textSize = 8.5f; paint.color = Color.GRAY; paint.typeface = Typeface.DEFAULT; current.canvas.drawText("Local offline record | Page $pageNumber", 48f, 817f, paint); document.finishPage(current) }; page = null }
        fun finish() = finishPage()
    }

    private fun formatAcres(value: Double) = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
