package ai.joita.biosoil.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import ai.joita.biosoil.R
import ai.joita.biosoil.domain.SoilAdvisor
import ai.joita.biosoil.model.AdvisoryResult
import ai.joita.biosoil.model.MeasurementConfidence
import ai.joita.biosoil.model.ParameterStatus
import ai.joita.biosoil.model.ReadingSource
import ai.joita.biosoil.model.SoilStatus
import ai.joita.biosoil.model.SoilTestRecord
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

object ReportService {
    fun createPdf(context: Context, test: SoilTestRecord): File {
        val reports = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reports, "JOITA-Soil-${test.id.take(8)}.pdf")
        val document = PdfDocument()
        val advisory = SoilAdvisor.assess(test.reading)
        val confidence = SoilAdvisor.measurementConfidence(test.reading, test.source, test.sampleCount)
        val renderer = FieldReportRenderer(context, document, test)
        renderer.drawReadingPage(advisory, confidence)
        renderer.drawAdvisoryPage(advisory, confidence)
        renderer.finish()
        FileOutputStream(file).use(document::writeTo)
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File, test: SoilTestRecord) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val resultStatus = statusLabel(context, test.status)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_report_subject))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_report_message, test.score, resultStatus))
            clipData = ClipData.newRawUri(context.getString(R.string.report_title), uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_report)).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    private class FieldReportRenderer(
        private val context: Context,
        private val document: PdfDocument,
        private val test: SoilTestRecord,
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var pageNumber = 0
        private var y = ContentTop

        fun drawReadingPage(advisory: AdvisoryResult, confidence: MeasurementConfidence) {
            startPage()
            y = drawParagraph(
                context.getString(R.string.report_title),
                Margin,
                y,
                ContentWidth,
                25f,
                Ink,
                bold = true,
                maxLines = 2,
            ) + 5f
            val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(test.createdAtEpochMs))
            val source = if (test.source == ReadingSource.USB) {
                "${sourceLabel(context, test.source)} - ${context.getString(R.string.report_samples_format, test.sampleCount)}"
            } else {
                sourceLabel(context, test.source)
            }
            y = drawParagraph(
                "${context.getString(R.string.report_generated_on)}: $date   |   ${context.getString(R.string.reading_source)}: $source",
                Margin,
                y,
                ContentWidth,
                10.5f,
                Muted,
            ) + 13f

            drawSectionTitle(context.getString(R.string.report_farmer_section))
            drawFarmerCard()
            y += 13f

            drawSectionTitle(context.getString(R.string.report_quick_summary))
            drawSummaryCard(advisory, confidence)
            y += 13f

            drawSectionTitle(context.getString(R.string.report_values_section))
            drawMetricGrid(advisory)
            drawReadingInterpretation(confidence)
        }

        fun drawAdvisoryPage(advisory: AdvisoryResult, confidence: MeasurementConfidence) {
            startPage()
            y = drawParagraph(
                context.getString(R.string.smart_advisory_title),
                Margin,
                y,
                ContentWidth,
                25f,
                Ink,
                bold = true,
            ) + 5f
            y = drawParagraph(
                context.getString(R.string.report_advisory_intro),
                Margin,
                y,
                ContentWidth,
                10.5f,
                Muted,
            ) + 13f

            drawConfidenceBanner(confidence)
            y += 14f

            drawSectionTitle(context.getString(R.string.report_priority_actions))
            advisory.immediateActions.take(4).forEachIndexed { index, action ->
                drawActionCard(index + 1, actionLabel(context, action))
                y += 7f
            }

            drawSectionTitle(context.getString(R.string.report_next_check))
            drawNumberedListCard(
                listOf(
                    context.getString(R.string.report_retest_step),
                    context.getString(R.string.report_lab_step),
                    context.getString(R.string.report_record_step),
                ),
            )
            y += 14f

            drawSectionTitle(context.getString(R.string.report_how_to_read))
            drawNotesCard(
                listOf(
                    context.getString(R.string.report_ph_note),
                    context.getString(R.string.report_ec_note),
                    context.getString(R.string.report_npk_note),
                ),
            )
            y += 14f

            drawLimitCard()
        }

        fun finish() {
            finishCurrentPage()
        }

        private fun drawFarmerCard() {
            val identity = test.reportIdentity
            val height = if (identity.isEmpty) 54f else 104f
            ensureSpace(height)
            val top = y
            fillRoundRect(Margin, y, PageWidth - Margin, y + height, 13f, Surface)
            if (identity.isEmpty) {
                drawParagraph(
                    context.getString(R.string.report_not_added),
                    Margin + 16f,
                    y + 18f,
                    ContentWidth - 32f,
                    12f,
                    Muted,
                    bold = true,
                )
            } else {
                val gap = 14f
                val columnWidth = (ContentWidth - 32f - gap) / 2f
                val left = Margin + 16f
                val right = left + columnWidth + gap
                drawInfoCell(context.getString(R.string.report_farmer_name), identity.farmerName, left, y + 13f, columnWidth)
                drawInfoCell(context.getString(R.string.report_father_name), identity.fatherName, right, y + 13f, columnWidth)
                drawInfoCell(context.getString(R.string.report_village), identity.village, left, y + 58f, columnWidth)
                drawInfoCell(context.getString(R.string.report_mobile), identity.mobileNumber, right, y + 58f, columnWidth)
            }
            y = top + height
        }

        private fun drawInfoCell(label: String, value: String, x: Float, top: Float, width: Float) {
            drawParagraph(label, x, top, width, 8.5f, Muted, bold = true, maxLines = 1)
            drawParagraph(
                value.ifBlank { context.getString(R.string.report_not_added) },
                x,
                top + 15f,
                width,
                11.5f,
                Ink,
                bold = true,
                maxLines = 2,
            )
        }

        private fun drawSummaryCard(advisory: AdvisoryResult, confidence: MeasurementConfidence) {
            val height = 102f
            ensureSpace(height)
            val top = y
            val accent = confidenceColor(confidence)
            fillRoundRect(Margin, top, PageWidth - Margin, top + height, 15f, PaleGreen)
            paint.color = accent
            canvas.drawRoundRect(RectF(Margin, top, Margin + 7f, top + height), 7f, 7f, paint)

            val left = Margin + 22f
            drawParagraph(context.getString(R.string.report_indicator_label), left, top + 16f, 170f, 9f, Muted, bold = true)
            drawParagraph("${advisory.score}/100", left, top + 35f, 170f, 25f, JoitaGreenDark, bold = true, maxLines = 1)
            drawParagraph(statusLabel(context, advisory.status), left, top + 69f, 170f, 11f, JoitaGreenDark, bold = true, maxLines = 1)

            val dividerX = Margin + 205f
            paint.color = GreenLine
            paint.strokeWidth = 1f
            canvas.drawLine(dividerX, top + 15f, dividerX, top + height - 15f, paint)
            val right = dividerX + 18f
            drawParagraph(context.getString(R.string.report_quality_label), right, top + 16f, 318f, 9f, Muted, bold = true)
            drawParagraph(confidenceTitle(context, confidence), right, top + 35f, 318f, 13f, accent, bold = true, maxLines = 2)
            drawParagraph(confidenceBody(context, confidence), right, top + 65f, 318f, 9.5f, Ink, maxLines = 3)
            y = top + height
        }

        private fun drawMetricGrid(advisory: AdvisoryResult) {
            val assessmentByKey = advisory.assessments.associateBy { it.key }
            val metrics = listOf(
                Metric("moisture", context.getString(R.string.metric_moisture), formatOne(test.reading.moisturePercent), "%"),
                Metric("temperature", context.getString(R.string.metric_temperature), formatOne(test.reading.temperatureCelsius), "°C"),
                Metric("ec", context.getString(R.string.metric_ec), test.reading.ecUsCm.toString(), "µS/cm"),
                Metric("ph", context.getString(R.string.metric_ph), formatOne(test.reading.ph), ""),
                Metric("nitrogen", context.getString(R.string.metric_nitrogen), test.reading.nitrogenMgKg.toString(), "mg/kg"),
                Metric("phosphorus", context.getString(R.string.metric_phosphorus), test.reading.phosphorusMgKg.toString(), "mg/kg"),
                Metric("potassium", context.getString(R.string.metric_potassium), test.reading.potassiumMgKg.toString(), "mg/kg"),
                Metric("fertility", context.getString(R.string.metric_fertility), test.reading.fertilityMgKg.toString(), "mg/kg"),
            )
            val gap = 9f
            val columnWidth = (ContentWidth - gap) / 2f
            val rowHeight = 63f
            metrics.chunked(2).forEach { row ->
                ensureSpace(rowHeight)
                row.forEachIndexed { column, metric ->
                    val left = Margin + column * (columnWidth + gap)
                    val assessment = assessmentByKey.getValue(metric.key)
                    drawMetricCard(left, y, columnWidth, rowHeight, metric, assessment.status)
                }
                y += rowHeight + gap
            }
            y -= gap
        }

        private fun drawMetricCard(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            metric: Metric,
            status: ParameterStatus,
        ) {
            fillRoundRect(left, top, left + width, top + height, 11f, statusBackground(status))
            drawParagraph(metric.label, left + 13f, top + 10f, width - 104f, 9f, Muted, bold = true, maxLines = 2)
            val value = listOf(metric.value, metric.unit).filter { it.isNotBlank() }.joinToString(" ")
            drawParagraph(value, left + 13f, top + 34f, width - 26f, 15f, Ink, bold = true, maxLines = 1)
            drawParagraph(
                parameterStatusLabel(context, status),
                left + width - 86f,
                top + 12f,
                72f,
                8.5f,
                statusColor(status),
                bold = true,
                maxLines = 1,
                alignment = Layout.Alignment.ALIGN_OPPOSITE,
            )
        }

        private fun drawReadingInterpretation(confidence: MeasurementConfidence) {
            val gap = 8f
            val height = 45f
            if (y + gap + height > ContentBottom) return
            y += gap
            val top = y
            fillRoundRect(Margin, top, PageWidth - Margin, top + height, 10f, WarmSurface)
            drawParagraph(context.getString(R.string.report_interpretation), Margin + 13f, top + 8f, 112f, 8.5f, Muted, bold = true, maxLines = 1)
            drawParagraph(confidenceBody(context, confidence), Margin + 125f, top + 8f, ContentWidth - 140f, 9f, Ink, maxLines = 2)
            y = top + height
        }

        private fun drawConfidenceBanner(confidence: MeasurementConfidence) {
            val title = confidenceTitle(context, confidence)
            val body = confidenceBody(context, confidence)
            val bodyHeight = measureParagraph(body, ContentWidth - 46f, 10f, maxLines = 4)
            val height = 60f + bodyHeight
            ensureSpace(height)
            val top = y
            fillRoundRect(Margin, top, PageWidth - Margin, top + height, 14f, confidenceBackground(confidence))
            paint.color = confidenceColor(confidence)
            canvas.drawCircle(Margin + 23f, top + 24f, 7f, paint)
            drawParagraph(context.getString(R.string.measurement_quality), Margin + 44f, top + 12f, ContentWidth - 60f, 8.5f, Muted, bold = true)
            drawParagraph(title, Margin + 44f, top + 29f, ContentWidth - 60f, 13f, confidenceColor(confidence), bold = true, maxLines = 2)
            drawParagraph(body, Margin + 18f, top + 55f, ContentWidth - 36f, 10f, Ink, maxLines = 4)
            y = top + height
        }

        private fun drawActionCard(number: Int, value: String) {
            val textWidth = ContentWidth - 76f
            val textHeight = measureParagraph(value, textWidth, 10.5f, bold = true, maxLines = 5)
            val height = maxOf(54f, textHeight + 24f)
            ensureSpace(height)
            val top = y
            fillRoundRect(Margin, top, PageWidth - Margin, top + height, 12f, WarmSurface)
            paint.color = Turmeric
            canvas.drawCircle(Margin + 23f, top + 27f, 14f, paint)
            drawParagraph(number.toString(), Margin + 15f, top + 18f, 16f, 11f, Ink, bold = true, maxLines = 1, alignment = Layout.Alignment.ALIGN_CENTER)
            drawParagraph(value, Margin + 52f, top + 12f, textWidth, 10.5f, Ink, bold = true, maxLines = 5)
            y = top + height
        }

        private fun drawNumberedListCard(items: List<String>) {
            val contentWidth = ContentWidth - 68f
            val itemHeights = items.map { maxOf(25f, measureParagraph(it, contentWidth, 9.8f, maxLines = 4) + 6f) }
            val height = itemHeights.sum() + 24f
            ensureSpace(height)
            val top = y
            fillRoundRect(Margin, top, PageWidth - Margin, top + height, 12f, Surface)
            var itemY = top + 14f
            items.forEachIndexed { index, item ->
                paint.color = JoitaGreenDark
                canvas.drawCircle(Margin + 23f, itemY + 10f, 9f, paint)
                drawParagraph((index + 1).toString(), Margin + 17f, itemY + 4f, 12f, 8.5f, Color.WHITE, bold = true, maxLines = 1, alignment = Layout.Alignment.ALIGN_CENTER)
                drawParagraph(item, Margin + 45f, itemY, contentWidth, 9.8f, Ink, maxLines = 4)
                itemY += itemHeights[index]
            }
            y = top + height
        }

        private fun drawNotesCard(items: List<String>) {
            val width = ContentWidth - 38f
            val itemHeights = items.map { measureParagraph(it, width, 9.4f, maxLines = 4) + 12f }
            val height = itemHeights.sum() + 22f
            ensureSpace(height)
            val top = y
            fillRoundRect(Margin, top, PageWidth - Margin, top + height, 12f, PaleBlue)
            var itemY = top + 13f
            items.forEachIndexed { index, item ->
                paint.color = InfoBlue
                canvas.drawCircle(Margin + 18f, itemY + 7f, 3f, paint)
                drawParagraph(item, Margin + 30f, itemY, width, 9.4f, Ink, maxLines = 4)
                itemY += itemHeights[index]
            }
            y = top + height
        }

        private fun drawLimitCard() {
            val text = context.getString(R.string.advisory_disclaimer) + " " + context.getString(R.string.sensor_advice_limit)
            val textHeight = measureParagraph(text, ContentWidth - 34f, 9.2f, bold = true, maxLines = 6)
            val height = textHeight + 49f
            ensureSpace(height)
            val top = y
            fillRoundRect(Margin, top, PageWidth - Margin, top + height, 12f, PaleRed)
            drawParagraph(context.getString(R.string.report_limit_title), Margin + 17f, top + 13f, ContentWidth - 34f, 10f, Red, bold = true)
            drawParagraph(text, Margin + 17f, top + 33f, ContentWidth - 34f, 9.2f, Ink, bold = true, maxLines = 6)
            y = top + height
        }

        private fun drawSectionTitle(value: String) {
            ensureSpace(27f)
            y = drawParagraph(value, Margin, y, ContentWidth, 14f, Ink, bold = true, maxLines = 2) + 8f
        }

        private fun startPage() {
            finishCurrentPage()
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PageWidth.toInt(), PageHeight.toInt(), pageNumber).create())
            canvas = requireNotNull(page).canvas
            drawHeader()
            y = ContentTop
        }

        private fun finishCurrentPage() {
            val current = page ?: return
            drawFooter()
            document.finishPage(current)
            page = null
        }

        private fun ensureSpace(required: Float) {
            if (y + required <= ContentBottom) return
            startPage()
        }

        private fun drawHeader() {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.joita_bioseed_logo)
            val logoHeight = 38f
            val naturalWidth = logo.width * logoHeight / logo.height
            val logoWidth = minOf(naturalWidth, 158f)
            canvas.drawBitmap(logo, null, RectF(Margin, 24f, Margin + logoWidth, 24f + logoHeight), paint)
            drawParagraph(
                context.getString(R.string.report_subtitle),
                315f,
                31f,
                PageWidth - Margin - 315f,
                9f,
                JoitaGreenDark,
                bold = true,
                maxLines = 2,
                alignment = Layout.Alignment.ALIGN_OPPOSITE,
            )
            paint.color = GreenLine
            paint.strokeWidth = 1f
            canvas.drawLine(Margin, 76f, PageWidth - Margin, 76f, paint)
        }

        private fun drawFooter() {
            paint.color = Line
            paint.strokeWidth = 1f
            canvas.drawLine(Margin, 804f, PageWidth - Margin, 804f, paint)
            drawParagraph(context.getString(R.string.publisher), Margin, 813f, 220f, 8.5f, JoitaGreenDark, bold = true, maxLines = 1)
            drawParagraph(
                "${context.getString(R.string.report_id)}: ${test.id.take(8).uppercase()}",
                225f,
                813f,
                230f,
                8f,
                Muted,
                maxLines = 1,
                alignment = Layout.Alignment.ALIGN_CENTER,
            )
            drawParagraph(
                context.getString(R.string.report_page_format, pageNumber),
                470f,
                813f,
                PageWidth - Margin - 470f,
                8f,
                Muted,
                bold = true,
                maxLines = 1,
                alignment = Layout.Alignment.ALIGN_OPPOSITE,
            )
        }

        private fun fillRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) {
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
        }

        private fun drawParagraph(
            value: String,
            x: Float,
            top: Float,
            width: Float,
            size: Float,
            color: Int,
            bold: Boolean = false,
            maxLines: Int = Int.MAX_VALUE,
            alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        ): Float {
            val layout = paragraphLayout(value, width, size, color, bold, maxLines, alignment)
            canvas.save()
            canvas.translate(x, top)
            layout.draw(canvas)
            canvas.restore()
            return top + layout.height
        }

        private fun measureParagraph(
            value: String,
            width: Float,
            size: Float,
            bold: Boolean = false,
            maxLines: Int = Int.MAX_VALUE,
        ): Float = paragraphLayout(value, width, size, Ink, bold, maxLines, Layout.Alignment.ALIGN_NORMAL).height.toFloat()

        private fun paragraphLayout(
            value: String,
            width: Float,
            size: Float,
            color: Int,
            bold: Boolean,
            maxLines: Int,
            alignment: Layout.Alignment,
        ): StaticLayout {
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            return StaticLayout.Builder.obtain(value, 0, value.length, textPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(alignment)
                .setIncludePad(false)
                .setLineSpacing(1.5f, 1.04f)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        }

        private data class Metric(val key: String, val label: String, val value: String, val unit: String)

        companion object {
            private const val PageWidth = 595f
            private const val PageHeight = 842f
            private const val Margin = 38f
            private const val ContentWidth = PageWidth - Margin * 2f
            private const val ContentTop = 94f
            private const val ContentBottom = 792f
            private val Ink = Color.rgb(18, 27, 24)
            private val Muted = Color.rgb(91, 103, 96)
            private val JoitaGreenDark = Color.rgb(18, 92, 42)
            private val GreenLine = Color.rgb(194, 222, 198)
            private val Surface = Color.rgb(246, 248, 245)
            private val PaleGreen = Color.rgb(226, 244, 226)
            private val WarmSurface = Color.rgb(255, 246, 225)
            private val PaleBlue = Color.rgb(234, 242, 255)
            private val PaleRed = Color.rgb(255, 235, 230)
            private val Turmeric = Color.rgb(244, 166, 34)
            private val InfoBlue = Color.rgb(33, 105, 179)
            private val Red = Color.rgb(164, 45, 38)
            private val Line = Color.rgb(216, 225, 217)
        }
    }

    private fun sourceLabel(context: Context, source: ReadingSource) = when (source) {
        ReadingSource.USB -> context.getString(R.string.source_usb)
        ReadingSource.MANUAL -> context.getString(R.string.source_manual)
        ReadingSource.SAMPLE -> context.getString(R.string.source_sample)
    }

    private fun statusLabel(context: Context, status: SoilStatus) = when (status) {
        SoilStatus.GOOD -> context.getString(R.string.status_good)
        SoilStatus.NEEDS_ATTENTION -> context.getString(R.string.status_attention)
        SoilStatus.URGENT -> context.getString(R.string.status_urgent)
    }

    private fun parameterStatusLabel(context: Context, status: ParameterStatus) = when (status) {
        ParameterStatus.GOOD -> context.getString(R.string.status_good)
        ParameterStatus.LOW -> context.getString(R.string.status_low)
        ParameterStatus.HIGH -> context.getString(R.string.status_high)
    }

    private fun actionLabel(context: Context, action: String) = context.getString(
        when (action) {
            "prepare_moist_soil" -> R.string.advice_prepare_moist_soil
            "irrigate" -> R.string.advice_irrigate
            "improve_drainage" -> R.string.advice_drainage
            "acidic_ph" -> R.string.advice_acidic_ph
            "alkaline_ph" -> R.string.advice_alkaline_ph
            "high_salinity" -> R.string.advice_high_salinity
            "low_nutrients" -> R.string.advice_low_nutrients
            "high_nutrients" -> R.string.advice_high_nutrients
            else -> R.string.advice_maintain
        },
    )

    private fun confidenceTitle(context: Context, confidence: MeasurementConfidence) = context.getString(
        when (confidence) {
            MeasurementConfidence.RETEST_REQUIRED -> R.string.confidence_retest_title
            MeasurementConfidence.PRELIMINARY -> R.string.confidence_preliminary_title
            MeasurementConfidence.FIELD_INDICATOR -> R.string.confidence_field_title
            MeasurementConfidence.MANUAL_ENTRY -> R.string.confidence_manual_title
            MeasurementConfidence.SAMPLE_DATA -> R.string.confidence_sample_title
        },
    )

    private fun confidenceBody(context: Context, confidence: MeasurementConfidence) = context.getString(
        when (confidence) {
            MeasurementConfidence.RETEST_REQUIRED -> R.string.confidence_retest_body
            MeasurementConfidence.PRELIMINARY -> R.string.confidence_preliminary_body
            MeasurementConfidence.FIELD_INDICATOR -> R.string.confidence_field_body
            MeasurementConfidence.MANUAL_ENTRY -> R.string.confidence_manual_body
            MeasurementConfidence.SAMPLE_DATA -> R.string.confidence_sample_body
        },
    )

    private fun confidenceColor(confidence: MeasurementConfidence) = when (confidence) {
        MeasurementConfidence.RETEST_REQUIRED -> Color.rgb(164, 45, 38)
        MeasurementConfidence.PRELIMINARY, MeasurementConfidence.MANUAL_ENTRY -> Color.rgb(125, 80, 35)
        MeasurementConfidence.FIELD_INDICATOR -> Color.rgb(18, 92, 42)
        MeasurementConfidence.SAMPLE_DATA -> Color.rgb(33, 105, 179)
    }

    private fun confidenceBackground(confidence: MeasurementConfidence) = when (confidence) {
        MeasurementConfidence.RETEST_REQUIRED -> Color.rgb(255, 235, 230)
        MeasurementConfidence.PRELIMINARY, MeasurementConfidence.MANUAL_ENTRY -> Color.rgb(255, 246, 225)
        MeasurementConfidence.FIELD_INDICATOR -> Color.rgb(226, 244, 226)
        MeasurementConfidence.SAMPLE_DATA -> Color.rgb(234, 242, 255)
    }

    private fun statusColor(status: ParameterStatus) = when (status) {
        ParameterStatus.GOOD -> Color.rgb(18, 92, 42)
        ParameterStatus.LOW -> Color.rgb(33, 105, 179)
        ParameterStatus.HIGH -> Color.rgb(164, 45, 38)
    }

    private fun statusBackground(status: ParameterStatus) = when (status) {
        ParameterStatus.GOOD -> Color.rgb(235, 247, 235)
        ParameterStatus.LOW -> Color.rgb(235, 243, 255)
        ParameterStatus.HIGH -> Color.rgb(255, 237, 233)
    }

    private fun formatOne(value: Double): String = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", value)
    }
}
