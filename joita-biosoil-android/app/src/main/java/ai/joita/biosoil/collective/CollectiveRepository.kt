package ai.joita.biosoil.collective

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class CollectiveRepository(private val context: Context) : SQLiteOpenHelper(context, "joita_collective.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE farmers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,village TEXT NOT NULL,latitude TEXT,longitude TEXT,lead INTEGER NOT NULL DEFAULT 0,notes TEXT,tenure TEXT,created_at INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE fields(id INTEGER PRIMARY KEY AUTOINCREMENT,farmer_id INTEGER NOT NULL,acreage REAL NOT NULL,crop TEXT NOT NULL,variety TEXT,season TEXT,sowing_date TEXT,soil_type TEXT,irrigation TEXT,inputs TEXT,boundary_notes TEXT,created_at INTEGER NOT NULL,FOREIGN KEY(farmer_id) REFERENCES farmers(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE visits(id INTEGER PRIMARY KEY AUTOINCREMENT,field_id INTEGER NOT NULL,date TEXT NOT NULL,officer TEXT NOT NULL,crop_stage TEXT,observations TEXT,issues TEXT,recommendations TEXT,yield_data TEXT,notes TEXT,photo_path TEXT,created_at INTEGER NOT NULL,FOREIGN KEY(field_id) REFERENCES fields(id) ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX idx_fields_farmer ON fields(farmer_id)")
        db.execSQL("CREATE INDEX idx_visits_field ON visits(field_id)")
    }

    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun farmers(query: String = "", leadOnly: Boolean = false): List<Farmer> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (query.isNotBlank()) { clauses += "(name LIKE ? OR village LIKE ? OR phone LIKE ?)"; repeat(3) { args += "%$query%" } }
        if (leadOnly) clauses += "lead=1"
        val where = clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        return readableDatabase.query("farmers", null, where, args.toTypedArray(), null, null, "lead DESC,name COLLATE NOCASE").use { c ->
            buildList { while (c.moveToNext()) add(Farmer(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6) == 1, c.getString(7), c.getString(8), c.getLong(9))) }
        }
    }

    fun addFarmer(f: Farmer): Long = writableDatabase.insertOrThrow("farmers", null, ContentValues().apply {
        put("name", f.name.trim()); put("phone", f.phone.trim()); put("village", f.village.trim()); put("latitude", f.latitude); put("longitude", f.longitude)
        put("lead", if (f.leadFarmer) 1 else 0); put("notes", f.notes.trim()); put("tenure", f.tenure); put("created_at", f.createdAt)
    })

    fun fields(farmerId: Long): List<FarmField> = readableDatabase.query("fields", null, "farmer_id=?", arrayOf(farmerId.toString()), null, null, "created_at DESC").use { c ->
        buildList { while (c.moveToNext()) add(FarmField(c.getLong(0), c.getLong(1), c.getDouble(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getLong(11))) }
    }

    fun addField(f: FarmField): Long = writableDatabase.insertOrThrow("fields", null, ContentValues().apply {
        put("farmer_id", f.farmerId); put("acreage", f.acreage); put("crop", f.crop.trim()); put("variety", f.variety.trim()); put("season", f.season); put("sowing_date", f.sowingDate)
        put("soil_type", f.soilType); put("irrigation", f.irrigation); put("inputs", f.inputs); put("boundary_notes", f.boundaryNotes); put("created_at", f.createdAt)
    })

    fun visits(fieldId: Long): List<FieldVisit> = readableDatabase.query("visits", null, "field_id=?", arrayOf(fieldId.toString()), null, null, "date DESC,created_at DESC").use { c ->
        buildList { while (c.moveToNext()) add(FieldVisit(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getLong(11))) }
    }

    fun addVisit(v: FieldVisit): Long = writableDatabase.insertOrThrow("visits", null, ContentValues().apply {
        put("field_id", v.fieldId); put("date", v.date); put("officer", v.officer.trim()); put("crop_stage", v.cropStage); put("observations", v.observations); put("issues", v.issues)
        put("recommendations", v.recommendations); put("yield_data", v.yieldData); put("notes", v.notes); put("photo_path", v.photoPath); put("created_at", v.createdAt)
    })

    fun totals(): Triple<Int, Double, Int> {
        val farmers = readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(lead),0) FROM farmers", null).use { it.moveToFirst(); it.getInt(0) to it.getInt(1) }
        val acres = readableDatabase.rawQuery("SELECT COALESCE(SUM(acreage),0) FROM fields", null).use { it.moveToFirst(); it.getDouble(0) }
        return Triple(farmers.first, acres, farmers.second)
    }

    fun savePhoto(bytes: ByteArray): String {
        val dir = File(context.filesDir, "field-photos").apply { mkdirs() }
        return File(dir, "visit-${System.currentTimeMillis()}.jpg").apply { writeBytes(bytes) }.absolutePath
    }
}
