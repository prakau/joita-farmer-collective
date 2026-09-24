package ai.joita.biosoil.collective

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CollectiveStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun editingRetainsLinkedRecordsAndSurvivesReopening() {
        val name = "test-${UUID.randomUUID()}.db"
        try {
            CollectiveRepository(context, name).use { repo ->
                val farmer = Farmer(name = "Test farmer", village = "Village", recordedBy = "Officer", acknowledgedAt = 123456L)
                val id = repo.addFarmer(farmer)
                val field = repo.addField(FarmField(farmerId = id, acreage = 1.5, crop = "Rice"))
                repo.addVisit(FieldVisit(fieldId = field, date = "31-12-2026", officer = "Officer"))
                repo.addVisit(FieldVisit(fieldId = field, date = "01-01-2027", officer = "Officer"))
                repo.addFarmer(farmer.copy(id = id, name = "Edited farmer"))
                assertEquals(1, repo.fields(id).size)
                assertEquals("01-01-2027", repo.visits(field).first().date)
            }
            CollectiveRepository(context, name).use { repo ->
                assertEquals("Edited farmer", repo.farmers().single().name)
                assertEquals(123456L, repo.farmers().single().acknowledgedAt)
                assertEquals(2, repo.visits().size)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun versionOneFarmerSurvivesRegistrationMigration() {
        val name = "test-upgrade-${UUID.randomUUID()}.db"
        try {
            val dbFile = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
            SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
                db.execSQL("CREATE TABLE farmers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,phone TEXT,village TEXT,latitude TEXT,longitude TEXT,lead INTEGER,notes TEXT,tenure TEXT,created_at INTEGER)")
                db.execSQL("INSERT INTO farmers VALUES(1,'Existing farmer','','Village','','',0,'','Owned',1234)")
                db.version = 1
            }
            CollectiveRepository(context, name).use { repo ->
                val farmer = repo.farmers().single()
                assertEquals("Existing farmer", farmer.name)
                assertEquals(1234L, farmer.createdAt)
                assertEquals("", farmer.photoPath)
                assertEquals(0L, farmer.acknowledgedAt)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun photoImportPreservesOriginalAndChecksums() {
        val source = File(context.cacheDir, "source-test.jpg")
        val bitmap = android.graphics.Bitmap.createBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val path = CollectivePhotos.import(context, android.net.Uri.fromFile(source), "Camera capture")
        try {
            assertEquals(CollectivePhotos.sha256(source), CollectivePhotos.sha256(File(path + ".original")))
            val json = org.json.JSONObject(File(path + ".json").readText())
            assertEquals("Camera capture", json.getString("source"))
            assertEquals(CollectivePhotos.sha256(source), json.getString("originalSha256"))
        } finally { source.delete(); listOf(path, path + ".original", path + ".json").forEach { File(it).delete() } }
    }

    @Test fun impactHistoryIsAppendOnlyAndSurvivesUpgradeFromVersionTwo() {
        val name = "test-impact-${UUID.randomUUID()}.db"
        try {
            var farmerId: Long
            CollectiveRepository(context, name).use { repo ->
                farmerId = repo.addFarmer(Farmer(name = "आशा", village = "रामपुर"))
                repo.writableDatabase.execSQL("DROP TABLE impact_assessments")
                repo.writableDatabase.version = 2
            }
            CollectiveRepository(context, name).use { repo ->
                val anchors = mapOf("stage" to "बेसलाइन", "farmerRef" to "CCF-001", "plotRef" to "P-001", "projectAcres" to "1.5", "cropStage" to "गेहूँ / बढ़वार")
                val baseline = ImpactAssessment(farmerId = farmerId, date = "01-06-2026", officer = "मीरा", answers = anchors + ("ph" to "7.2"))
                repo.addImpact(baseline)
                repo.addImpact(baseline.copy(date = "24-09-2026", answers = anchors + mapOf("stage" to "फॉलो-अप", "ph" to "7.0")))
            }
            CollectiveRepository(context, name).use { repo ->
                assertEquals("आशा", repo.farmers().single().name)
                assertEquals(2, repo.snapshot().impactsFor(farmerId).size)
                assertEquals(setOf("7.2", "7.0"), repo.impacts().map { it.answers["ph"] }.toSet())
                assertTrue(repo.impacts().all { it.consentedAt == 0L })
            }
        } finally { context.deleteDatabase(name) }
    }
}
