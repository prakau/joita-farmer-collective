package ai.joita.biosoil.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ai.joita.biosoil.model.FieldDraft
import ai.joita.biosoil.model.FieldProfile
import ai.joita.biosoil.model.ReadingSource
import ai.joita.biosoil.model.SoilReading
import ai.joita.biosoil.model.SoilStatus
import ai.joita.biosoil.model.SoilTestRecord

class SoilRepository(context: Context) {
    private val db = SoilDatabaseHelper(context.applicationContext)

    @Synchronized
    fun addField(draft: FieldDraft): FieldProfile {
        val database = db.writableDatabase
        database.beginTransaction()
        try {
            val farmerId = database.insertOrThrow(
                "farmers",
                null,
                ContentValues().apply {
                    put("name", draft.farmerName.trim())
                    put("phone", draft.phone.trim())
                    put("village", draft.village.trim())
                    put("district", draft.district.trim())
                    put("state", draft.state.trim())
                    put("pincode", draft.pincode.trim())
                },
            )
            val fieldId = database.insertOrThrow(
                "fields",
                null,
                ContentValues().apply {
                    put("farmer_id", farmerId)
                    put("name", draft.fieldName.trim())
                    put("crop", draft.crop.trim())
                    put("plot_number", draft.plotNumber.trim())
                    draft.areaAcres?.let { put("area_acres", it) }
                },
            )
            database.setTransactionSuccessful()
            return FieldProfile(
                id = fieldId,
                farmerId = farmerId,
                farmerName = draft.farmerName.trim(),
                village = draft.village.trim(),
                district = draft.district.trim(),
                state = draft.state.trim(),
                fieldName = draft.fieldName.trim(),
                crop = draft.crop.trim(),
                areaAcres = draft.areaAcres,
                phone = draft.phone.trim(),
                pincode = draft.pincode.trim(),
                plotNumber = draft.plotNumber.trim(),
            )
        } finally {
            database.endTransaction()
        }
    }

    @Synchronized
    fun fields(): List<FieldProfile> {
        val sql = """
            SELECT f.id, f.farmer_id, f.name, f.crop, f.area_acres,
                   r.name, r.phone, r.village, r.district, r.state,
                   r.pincode, f.plot_number
            FROM fields f JOIN farmers r ON r.id = f.farmer_id
            ORDER BY f.created_at DESC
        """.trimIndent()
        return db.readableDatabase.rawQuery(sql, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        FieldProfile(
                            id = cursor.getLong(0),
                            farmerId = cursor.getLong(1),
                            fieldName = cursor.getString(2),
                            crop = cursor.getString(3),
                            areaAcres = if (cursor.isNull(4)) null else cursor.getDouble(4),
                            farmerName = cursor.getString(5),
                            phone = cursor.getString(6),
                            village = cursor.getString(7),
                            district = cursor.getString(8),
                            state = cursor.getString(9),
                            pincode = cursor.getString(10),
                            plotNumber = cursor.getString(11),
                        ),
                    )
                }
            }
        }
    }

    @Synchronized
    fun saveTest(test: SoilTestRecord) {
        db.writableDatabase.insertWithOnConflict(
            "soil_tests",
            null,
            ContentValues().apply {
                put("id", test.id)
                test.fieldId?.let { put("field_id", it) }
                put("field_label", test.fieldLabel)
                put("crop", test.crop)
                put("source", test.source.name)
                put("moisture", test.reading.moisturePercent)
                put("temperature", test.reading.temperatureCelsius)
                put("ec", test.reading.ecUsCm)
                put("ph", test.reading.ph)
                put("nitrogen", test.reading.nitrogenMgKg)
                put("phosphorus", test.reading.phosphorusMgKg)
                put("potassium", test.reading.potassiumMgKg)
                put("fertility", test.reading.fertilityMgKg)
                put("score", test.score)
                put("status", test.status.name)
                test.latitude?.let { put("latitude", it) }
                test.longitude?.let { put("longitude", it) }
                test.accuracyMeters?.let { put("accuracy_m", it) }
                test.photoUri?.let { put("photo_uri", it) }
                put("source_note", test.sourceNote)
                put("created_at", test.createdAtEpochMs)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    @Synchronized
    fun tests(): List<SoilTestRecord> {
        return db.readableDatabase.query(
            "soil_tests",
            null,
            null,
            null,
            null,
            null,
            "created_at DESC",
        ).use { cursor ->
            fun index(name: String) = cursor.getColumnIndexOrThrow(name)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SoilTestRecord(
                            id = cursor.getString(index("id")),
                            fieldId = if (cursor.isNull(index("field_id"))) null else cursor.getLong(index("field_id")),
                            fieldLabel = cursor.getString(index("field_label")),
                            crop = cursor.getString(index("crop")),
                            source = ReadingSource.valueOf(cursor.getString(index("source"))),
                            reading = SoilReading(
                                moisturePercent = cursor.getDouble(index("moisture")),
                                temperatureCelsius = cursor.getDouble(index("temperature")),
                                ecUsCm = cursor.getInt(index("ec")),
                                ph = cursor.getDouble(index("ph")),
                                nitrogenMgKg = cursor.getInt(index("nitrogen")),
                                phosphorusMgKg = cursor.getInt(index("phosphorus")),
                                potassiumMgKg = cursor.getInt(index("potassium")),
                                fertilityMgKg = cursor.getInt(index("fertility")),
                            ),
                            score = cursor.getInt(index("score")),
                            status = SoilStatus.valueOf(cursor.getString(index("status"))),
                            latitude = if (cursor.isNull(index("latitude"))) null else cursor.getDouble(index("latitude")),
                            longitude = if (cursor.isNull(index("longitude"))) null else cursor.getDouble(index("longitude")),
                            accuracyMeters = if (cursor.isNull(index("accuracy_m"))) null else cursor.getFloat(index("accuracy_m")),
                            photoUri = if (cursor.isNull(index("photo_uri"))) null else cursor.getString(index("photo_uri")),
                            sourceNote = cursor.getString(index("source_note")),
                            createdAtEpochMs = cursor.getLong(index("created_at")),
                        ),
                    )
                }
            }
        }
    }

    @Synchronized
    fun clearAll() {
        val database = db.writableDatabase
        database.beginTransaction()
        try {
            database.delete("soil_tests", null, null)
            database.delete("fields", null, null)
            database.delete("farmers", null, null)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}

private class SoilDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "joita_soil.db", null, 1) {
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE farmers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                village TEXT NOT NULL,
                district TEXT NOT NULL,
                state TEXT NOT NULL,
                pincode TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE fields (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                farmer_id INTEGER NOT NULL REFERENCES farmers(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                crop TEXT NOT NULL,
                plot_number TEXT NOT NULL DEFAULT '',
                area_acres REAL,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE soil_tests (
                id TEXT PRIMARY KEY,
                field_id INTEGER REFERENCES fields(id) ON DELETE SET NULL,
                field_label TEXT NOT NULL,
                crop TEXT NOT NULL,
                source TEXT NOT NULL,
                moisture REAL NOT NULL,
                temperature REAL NOT NULL,
                ec INTEGER NOT NULL,
                ph REAL NOT NULL,
                nitrogen INTEGER NOT NULL,
                phosphorus INTEGER NOT NULL,
                potassium INTEGER NOT NULL,
                fertility INTEGER NOT NULL,
                score INTEGER NOT NULL,
                status TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                accuracy_m REAL,
                photo_uri TEXT,
                source_note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onConfigure(database: SQLiteDatabase) {
        super.onConfigure(database)
        database.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
