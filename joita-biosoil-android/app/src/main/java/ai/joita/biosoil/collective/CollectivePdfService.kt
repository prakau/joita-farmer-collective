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
    fun createFarmerReport(context: Context, farmer: Farmer, fields: List<FarmField>, visits: Map<Long, List<FieldVisit>>, impacts: List<ImpactAssessment> = emptyList()): File {
        val reports = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = farmer.name.replace(Regex("[^A-Za-z0-9-]+"), "-").trim('-').take(40).ifBlank { "किसान" }
        val output = File(reports, "JOITA-$safeName-${farmer.id}-${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()
        val renderer = Renderer(document, farmer)
        renderer.title("किसान प्रोफाइल")
        renderer.pair("नाम", farmer.name)
        renderer.pair("पंजीकरण संदर्भ", "JOITA-${farmer.id}")
        renderer.pair("पंजीकरण (फोन का समय)", DateFormat.getDateTimeInstance().format(Date(farmer.createdAt)))
        renderer.pair("दर्ज करने वाले कर्मी", farmer.recordedBy.ifBlank { "दर्ज नहीं" })
        renderer.pair("सहमति रिकॉर्ड", if (farmer.acknowledgedAt > 0) "कर्मी ने प्रोफाइल और फोटो रखने की किसान सहमति दर्ज की: ${DateFormat.getDateTimeInstance().format(Date(farmer.acknowledgedAt))}। यह डिजिटल हस्ताक्षर नहीं है।" else "दर्ज नहीं")
        renderer.pair("गाँव", farmer.village)
        renderer.pair("मोबाइल", farmer.phone.ifBlank { "दर्ज नहीं" })
        renderer.pair("परियोजना भूमिका", if (farmer.leadFarmer) "किसान साथी" else "किसान")
        renderer.pair("भूमि व्यवस्था", farmer.tenure)
        if (farmer.latitude.isNotBlank()) renderer.pair("GPS निर्देशांक", "${farmer.latitude}, ${farmer.longitude}")
        if (farmer.notes.isNotBlank()) renderer.paragraph("परिवार और खेती की जानकारी", farmer.notes)
        renderer.section("समूह विवरण")
        renderer.pair("खेत", fields.size.toString())
        renderer.pair("कुल क्षेत्र", "${formatAcres(fields.sumOf { it.acreage })} एकड़")
        renderer.pair("दर्ज विजिट", visits.values.sumOf { it.size }.toString())
        renderer.section("Data completeness / अगला कदम")
        val missing = buildList {
            if (farmer.phone.isBlank()) add("मोबाइल / Phone भरें")
            if (farmer.photoPath.isBlank()) add("किसान का पहचान फोटो जोड़ें")
            if (farmer.latitude.isBlank() || farmer.longitude.isBlank()) add("GPS या Plot ID सुरक्षित करें")
            if (fields.isEmpty()) add("कम-से-कम एक field: area, crop, season, irrigation भरें")
            if (impacts.isEmpty()) add("Hindi impact assessment: baseline / follow-up भरें")
            if (fields.isNotEmpty() && visits.values.flatten().isEmpty()) add("पहली dated field visit दर्ज करें")
        }
        if (missing.isEmpty()) renderer.body("पूरा रिकॉर्ड: profile, field, visit और impact assessment उपलब्ध हैं।")
        else {
            renderer.body("यह रिपोर्ट अभी अधूरी है। नीचे के डेटा को भरकर दोबारा PDF export करें:")
            missing.forEachIndexed { index, item -> renderer.body("${index + 1}. $item") }
        }
        renderer.section("खेत (${fields.size})")
        if (fields.isEmpty()) renderer.body("खेत दर्ज नहीं है।")
        fields.forEachIndexed { index, field ->
            renderer.fieldHeading("${index + 1}. ${field.crop} - ${formatAcres(field.acreage)} एकड़")
            renderer.body(listOf(field.variety, field.season, field.sowingDate).filter { it.isNotBlank() }.joinToString(" | ").ifBlank { "फसल विवरण दर्ज नहीं" })
            renderer.pair("मिट्टी / सिंचाई", listOf(field.soilType, field.irrigation).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "दर्ज नहीं" })
            if (field.inputs.isNotBlank()) renderer.paragraph("खाद / इनपुट", field.inputs)
            if (field.boundaryNotes.isNotBlank()) renderer.paragraph("सीमा / स्थान", field.boundaryNotes)
            val fieldVisits = visits[field.id].orEmpty()
            renderer.fieldHeading("विजिट इतिहास (${fieldVisits.size})")
            if (fieldVisits.isEmpty()) renderer.body("इस खेत की कोई विजिट दर्ज नहीं।")
            fieldVisits.forEach { visit ->
                renderer.visitHeading("${visit.date} | ${visit.officer}${visit.cropStage.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()}")
                if (visit.observations.isNotBlank()) renderer.paragraph("अवलोकन", visit.observations)
                if (visit.issues.isNotBlank()) renderer.paragraph("कीट / रोग", visit.issues)
                if (visit.recommendations.isNotBlank()) renderer.paragraph("सलाह", visit.recommendations)
                if (visit.yieldData.isNotBlank()) renderer.paragraph("उपज / कटाई", visit.yieldData)
                if (visit.notes.isNotBlank()) renderer.paragraph("नोट", visit.notes)
                renderer.pair("फोटो प्रमाण", if (visit.photoPath.isNotBlank()) "अंत में फोटो देखें; अनुपलब्ध होने पर वहाँ बताया गया है" else "नहीं जोड़ा")
            }
        }
        impacts.forEach { assessment ->
            renderer.assessmentPage()
            renderer.fieldHeading("किसान तकनीकी उपयोग एवं प्रभाव आकलन • #${assessment.id}")
            renderer.pair("दिनांक / चरण", "${assessment.date} / ${assessment.answers["stage"].orEmpty()}")
            renderer.pair("फील्ड कर्मी", assessment.officer)
            renderer.pair("फोन पर सहेजने का समय", DateFormat.getDateTimeInstance().format(Date(assessment.createdAt)))
            ImpactSchema.sections.forEach { section ->
                renderer.section(section.title)
                section.questions.forEach { question -> renderer.pair(question.label, assessment.answers[question.key].orEmpty().ifBlank { "दर्ज नहीं" }) }
            }
            renderer.section("किसान सहमति")
            renderer.body(ImpactSchema.consentText)
            renderer.body(if (assessment.consentedAt > 0) "फील्ड कर्मी ने किसान की सहमति दर्ज की: ${DateFormat.getDateTimeInstance().format(Date(assessment.consentedAt))} (फोन का समय)। यह डिजिटल हस्ताक्षर नहीं है।" else "सहमति दर्ज नहीं। यह वाक्य अपने-आप किसान की सहमति नहीं दर्शाता।")
            renderer.body("किसान हस्ताक्षर / अंगूठा (कागजी प्रति पर): __________________")
            renderer.body("फील्ड कर्मी हस्ताक्षर (कागजी प्रति पर): __________________")
            renderer.body("दर्ज प्रभाव स्वयं बताया / फील्ड में दर्ज डेटा है; स्वतंत्र सत्यापन या कार्बन प्रमाणन नहीं। अंतिम CO₂e गणना JOITA द्वारा अलग से की जाएगी।")
            assessment.photos.forEach { (key, path) -> renderer.photo("आकलन #${assessment.id} • ${ImpactSchema.photoLabels[key].orEmpty()}", path) }
        }
        if (farmer.photoPath.isNotBlank()) renderer.photo("किसान पंजीकरण - ${farmer.name}", farmer.photoPath)
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
            putExtra(Intent.EXTRA_TEXT, "Joita किसान समूह से निर्यात किया गया ऑफलाइन फील्ड रिकॉर्ड।")
            clipData = ClipData.newRawUri("JOITA farmer report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "PDF सहेजें / साझा करें").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    private class Renderer(private val document: PdfDocument, private val farmer: Farmer) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var pageNumber = 0
        private var y = 0f

        init { newPage() }

        fun title(text: String) { wrapped(text, 25f, Color.rgb(18, 92, 42), true) }
        fun section(text: String) { ensure(48f); y += 13f; wrapped(text, 17f, Color.rgb(18, 92, 42), true) }
        fun assessmentPage() = newPage()
        fun fieldHeading(text: String) { ensure(55f); y += 8f; wrapped(text, 14f, Color.rgb(35, 139, 42), true) }
        fun visitHeading(text: String) { ensure(50f); y += 6f; wrapped(text, 12f, Color.rgb(122, 78, 45), true) }
        fun body(text: String) { wrapped(text, 10.5f, Color.rgb(35, 42, 38), false) }
        fun pair(label: String, value: String) { wrapped("$label: $value", 10.5f, Color.rgb(35, 42, 38), false) }
        fun paragraph(label: String, value: String) { wrapped("$label: $value", 10.5f, Color.rgb(35, 42, 38), false) }

        fun photo(title: String, path: String) {
            val bitmap = CollectivePhotos.decode(path, 1600) ?: run { section("फोटो उपलब्ध नहीं"); body(title); return }
            newPage()
            wrapped("फोटो रिकॉर्ड", 10f, Color.rgb(35, 139, 42), true)
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
