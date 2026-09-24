package ai.joita.biosoil.collective

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CollectivePdfServiceTest {
    @Test fun completeFarmerReportRendersEveryPage() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val photo = File(context.filesDir, "field-photos/test-evidence.jpg").apply { parentFile?.mkdirs() }
        val bitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(198, 218, 177))
            drawRect(0f, 360f, 900f, 600f, Paint().apply { color = Color.rgb(78, 115, 55) })
        }
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()

        val farmer = Farmer(id = 7, name = "Asha Devi", phone = "+91 98765 43210", village = "Rampur", leadFarmer = true, notes = "Smallholder demonstration farmer")
        val field = FarmField(id = 11, farmerId = 7, acreage = 1.75, crop = "Mustard", variety = "Pusa Bold", season = "Rabi", sowingDate = "15-11-2026", soilType = "Loam", irrigation = "Tube well")
        val visit = FieldVisit(id = 13, fieldId = 11, date = "10-01-2027", officer = "Meera", cropStage = "Flowering", observations = "Good uniform stand and healthy flowering across the plot.", recommendations = "Continue scouting weekly.", photoPath = photo.absolutePath)
        val assessment = ImpactAssessment(id = 1, farmerId = farmer.id, date = "24-09-2026", officer = "मीरा", answers = mapOf(
            "stage" to "बेसलाइन", "farmerName" to "आशा देवी", "village" to "रामपुर", "projectAcres" to "1.75",
            "farmAssistUse" to "हाँ", "advice" to "सिंचाई | पोषण", "ph" to "7.2", "ec" to "0.45", "ecUnit" to "dS/m",
            "moisture" to "22.5", "feedback" to "मिट्टी की स्थिति समझने और सिंचाई का समय चुनने में मदद मिली।",
            "trialType" to "लागू नहीं", "satisfaction" to "संतुष्ट", "noBurning" to "हाँ",
        ), photos = mapOf("baseline" to photo.absolutePath))
        val pdf = CollectivePdfService.createFarmerReport(context, farmer, listOf(field), mapOf(field.id to listOf(visit)), listOf(assessment))

        assertTrue(pdf.length() > 1_000)
        PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertTrue(renderer.pageCount >= 2)
            repeat(renderer.pageCount) { index -> renderer.openPage(index).use { page ->
                val rendered = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                assertTrue(rendered.getPixel(5, 5) != Color.TRANSPARENT)
                rendered.recycle()
            } }
        }
        val qa = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        pdf.copyTo(File(qa, "farmer-report.pdf"), overwrite = true)
        val archive = CollectiveEvidence.create(context, farmer, listOf(field), mapOf(field.id to listOf(visit)), pdf, listOf(assessment))
        java.util.zip.ZipFile(archive).use { zip ->
            assertTrue(zip.getEntry("records.json") != null)
            assertTrue(zip.getEntry("farmer-report.pdf") != null)
            assertTrue(zip.getEntry("photos/${photo.name}") != null)
            val records = org.json.JSONObject(zip.getInputStream(zip.getEntry("records.json")).bufferedReader().use { it.readText() })
            assertEquals("7.2", records.getJSONArray("impactAssessments").getJSONObject(0).getJSONObject("answers").getString("ph"))
        }
        val longReport = CollectivePdfService.createFarmerReport(context, farmer.copy(name = "आशा देवी", notes = "बहुत अच्छी फसल। ".repeat(500) + "x".repeat(600)), listOf(field), mapOf(field.id to listOf(visit)))
        longReport.copyTo(File(qa, "long-report.pdf"), overwrite = true)
    }
}
