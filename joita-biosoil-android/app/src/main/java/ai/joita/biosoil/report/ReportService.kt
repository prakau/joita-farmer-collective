package ai.joita.biosoil.report

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import ai.joita.biosoil.R
import ai.joita.biosoil.domain.SoilAdvisor
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
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 42f

        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.joita_bioseed_logo)
        val logoHeight = 58f
        val logoWidth = logo.width * logoHeight / logo.height
        canvas.drawBitmap(
            logo,
            null,
            android.graphics.RectF(margin, 30f, margin + logoWidth, 30f + logoHeight),
            paint,
        )

        fun text(value: String, y: Float, size: Float = 13f, bold: Boolean = false, color: Int = Color.rgb(17, 24, 23)) {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            canvas.drawText(value, margin, y, paint)
        }

        text(context.getString(R.string.report_title), 118f, 22f, true)
        text("${context.getString(R.string.report_id)}: ${test.id}", 143f, 10f)
        text(test.fieldLabel, 177f, 17f, true)
        text(test.crop, 198f)
        text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(test.createdAtEpochMs)), 219f)
        text("${context.getString(R.string.reading_source)}: ${sourceLabel(context, test.source)}", 240f)

        paint.color = Color.rgb(221, 243, 221)
        canvas.drawRoundRect(margin, 264f, 553f, 344f, 16f, 16f, paint)
        text("${context.getString(R.string.soil_health_score)}: ${test.score}/100", 302f, 24f, true, Color.rgb(18, 92, 42))
        text(statusLabel(context, test.status), 328f, 13f, true, Color.rgb(18, 92, 42))

        text(context.getString(R.string.observed_values), 383f, 17f, true)
        val values = listOf(
            context.getString(R.string.metric_moisture) to "${test.reading.moisturePercent} %",
            context.getString(R.string.metric_temperature) to "${test.reading.temperatureCelsius} °C",
            context.getString(R.string.metric_ec) to "${test.reading.ecUsCm} µS/cm",
            context.getString(R.string.metric_ph) to test.reading.ph.toString(),
            context.getString(R.string.metric_nitrogen) to "${test.reading.nitrogenMgKg} mg/kg",
            context.getString(R.string.metric_phosphorus) to "${test.reading.phosphorusMgKg} mg/kg",
            context.getString(R.string.metric_potassium) to "${test.reading.potassiumMgKg} mg/kg",
            context.getString(R.string.metric_fertility) to "${test.reading.fertilityMgKg} mg/kg",
        )
        values.forEachIndexed { index, (label, value) ->
            val columnX = if (index % 2 == 0) margin else 308f
            val rowY = 414f + (index / 2) * 38f
            paint.textSize = 12f
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.color = Color.rgb(89, 100, 94)
            canvas.drawText(label, columnX, rowY, paint)
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.color = Color.rgb(17, 24, 23)
            canvas.drawText(value, columnX, rowY + 17f, paint)
        }

        paint.color = Color.rgb(255, 226, 168)
        canvas.drawRoundRect(margin, 570f, 553f, 773f, 12f, 12f, paint)
        text(context.getString(R.string.farmer_advice_title), 598f, 17f, true)
        val advisory = SoilAdvisor.assess(test.reading)
        paint.color = Color.rgb(17, 24, 23)
        paint.textSize = 11f
        paint.typeface = android.graphics.Typeface.DEFAULT
        var adviceY = 622f
        advisory.immediateActions.take(2).forEach { action ->
            adviceY = drawWrapped(canvas, paint, "• ${actionLabel(context, action)}", margin + 14f, adviceY, 475f, 15f) + 5f
        }
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        drawWrapped(canvas, paint, context.getString(R.string.advisory_disclaimer), margin + 14f, 741f, 475f, 14f)

        text(context.getString(R.string.publisher), 812f, 11f, true, Color.rgb(18, 92, 42))
        document.finishPage(page)
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

    private fun drawWrapped(canvas: android.graphics.Canvas, paint: Paint, value: String, x: Float, y: Float, width: Float, lineHeight: Float): Float {
        var line = ""
        var currentY = y
        value.split(" ").forEach { word ->
            val next = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(next) > width && line.isNotEmpty()) {
                canvas.drawText(line, x, currentY, paint)
                currentY += lineHeight
                line = word
            } else line = next
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, currentY, paint)
        return currentY
    }
}
