package ai.joita.biosoil.collective

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CollectiveRepository(private val context: Context, name: String = "joita_collective.db") : SQLiteOpenHelper(context, name, null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE farmers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,village TEXT NOT NULL,latitude TEXT,longitude TEXT,lead INTEGER NOT NULL DEFAULT 0,notes TEXT,tenure TEXT,created_at INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE fields(id INTEGER PRIMARY KEY AUTOINCREMENT,farmer_id INTEGER NOT NULL,acreage REAL NOT NULL,crop TEXT NOT NULL,variety TEXT,season TEXT,sowing_date TEXT,soil_type TEXT,irrigation TEXT,inputs TEXT,boundary_notes TEXT,created_at INTEGER NOT NULL,FOREIGN KEY(farmer_id) REFERENCES farmers(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE visits(id INTEGER PRIMARY KEY AUTOINCREMENT,field_id INTEGER NOT NULL,date TEXT NOT NULL,officer TEXT NOT NULL,crop_stage TEXT,observations TEXT,issues TEXT,recommendations TEXT,yield_data TEXT,notes TEXT,photo_path TEXT,created_at INTEGER NOT NULL,FOREIGN KEY(field_id) REFERENCES fields(id) ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX idx_fields_farmer ON fields(farmer_id)")
        db.execSQL("CREATE INDEX idx_visits_field ON visits(field_id)")
        addRegistrationColumns(db)
        addImpactTable(db)
    }

    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) addRegistrationColumns(db)
        if (oldVersion < 3) addImpactTable(db)
    }

    private fun addImpactTable(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE impact_assessments(id INTEGER PRIMARY KEY AUTOINCREMENT,farmer_id INTEGER NOT NULL,date TEXT NOT NULL,officer TEXT NOT NULL,answers TEXT NOT NULL,photos TEXT NOT NULL,consented_at INTEGER NOT NULL,created_at INTEGER NOT NULL,FOREIGN KEY(farmer_id) REFERENCES farmers(id) ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX idx_impact_farmer ON impact_assessments(farmer_id)")
    }

    /** Append only: a follow-up or correction never overwrites the earlier assessment. */
    fun addImpact(value: ImpactAssessment): Long {
        require(value.id == 0L)
        require(ImpactSchema.validate(value.answers, value.date, value.officer) == null)
        return writableDatabase.insertOrThrow("impact_assessments", null, ContentValues().apply {
            put("farmer_id", value.farmerId); put("date", value.date); put("officer", value.officer)
            put("answers", org.json.JSONObject(value.answers).toString()); put("photos", org.json.JSONObject(value.photos).toString())
            put("consented_at", value.consentedAt); put("created_at", value.createdAt)
        })
    }

    fun impacts(): List<ImpactAssessment> = readableDatabase.query("impact_assessments", null, null, null, null, null, "created_at DESC,id DESC").use { c ->
        fun map(raw: String): Map<String, String> { val json = org.json.JSONObject(raw); return json.keys().asSequence().associateWith { json.getString(it) } }
        buildList { while (c.moveToNext()) add(ImpactAssessment(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), map(c.getString(4)), map(c.getString(5)), c.getLong(6), c.getLong(7))) }
    }

    private fun addRegistrationColumns(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE farmers ADD COLUMN photo_path TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE farmers ADD COLUMN recorded_by TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE farmers ADD COLUMN acknowledged_at INTEGER NOT NULL DEFAULT 0")
    }

    fun farmers(query: String = "", leadOnly: Boolean = false): List<Farmer> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (query.isNotBlank()) { clauses += "(name LIKE ? OR village LIKE ? OR phone LIKE ?)"; repeat(3) { args += "%$query%" } }
        if (leadOnly) clauses += "lead=1"
        val where = clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        return readableDatabase.query("farmers", null, where, args.toTypedArray(), null, null, "lead DESC,name COLLATE NOCASE").use { c ->
            buildList { while (c.moveToNext()) add(Farmer(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6) == 1, c.getString(7), c.getString(8), c.getLong(9), c.getString(10), c.getString(11), c.getLong(12))) }
        }
    }

    fun addFarmer(f: Farmer): Long = save("farmers", f.id, ContentValues().apply {
        put("name", f.name.trim()); put("phone", f.phone.trim()); put("village", f.village.trim()); put("latitude", f.latitude); put("longitude", f.longitude)
        put("lead", if (f.leadFarmer) 1 else 0); put("notes", f.notes.trim()); put("tenure", f.tenure); put("created_at", f.createdAt)
        put("photo_path", f.photoPath); put("recorded_by", f.recordedBy.trim()); put("acknowledged_at", f.acknowledgedAt)
    })

    fun fields(farmerId: Long? = null): List<FarmField> = readableDatabase.query("fields", null, if (farmerId == null) null else "farmer_id=?", farmerId?.let { arrayOf(it.toString()) }, null, null, "created_at DESC").use { c ->
        buildList { while (c.moveToNext()) add(FarmField(c.getLong(0), c.getLong(1), c.getDouble(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getLong(11))) }
    }

    fun addField(f: FarmField): Long = save("fields", f.id, ContentValues().apply {
        put("farmer_id", f.farmerId); put("acreage", f.acreage); put("crop", f.crop.trim()); put("variety", f.variety.trim()); put("season", f.season); put("sowing_date", f.sowingDate)
        put("soil_type", f.soilType); put("irrigation", f.irrigation); put("inputs", f.inputs); put("boundary_notes", f.boundaryNotes); put("created_at", f.createdAt)
    })

    fun visits(fieldId: Long? = null): List<FieldVisit> = readableDatabase.query("visits", null, if (fieldId == null) null else "field_id=?", fieldId?.let { arrayOf(it.toString()) }, null, null, "created_at DESC").use { c ->
        buildList { while (c.moveToNext()) add(FieldVisit(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getLong(11))) }
    }.sortedWith(compareByDescending<FieldVisit> { CollectiveRules.dateMillis(it.date) ?: it.createdAt }.thenByDescending { it.createdAt })

    fun addVisit(v: FieldVisit): Long = save("visits", v.id, ContentValues().apply {
        put("field_id", v.fieldId); put("date", v.date); put("officer", v.officer.trim()); put("crop_stage", v.cropStage); put("observations", v.observations); put("issues", v.issues)
        put("recommendations", v.recommendations); put("yield_data", v.yieldData); put("notes", v.notes); put("photo_path", v.photoPath); put("created_at", v.createdAt)
    })

    fun totals(): Triple<Int, Double, Int> {
        val farmers = readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(lead),0) FROM farmers", null).use { it.moveToFirst(); it.getInt(0) to it.getInt(1) }
        val acres = readableDatabase.rawQuery("SELECT COALESCE(SUM(acreage),0) FROM fields", null).use { it.moveToFirst(); it.getDouble(0) }
        return Triple(farmers.first, acres, farmers.second)
    }

    // UPDATE retains IDs and child records; REPLACE would trigger cascading deletes.
    private fun save(table: String, id: Long, values: ContentValues): Long {
        if (id == 0L) return writableDatabase.insertOrThrow(table, null, values)
        check(writableDatabase.update(table, values, "id=?", arrayOf(id.toString())) == 1) { "Record no longer exists" }
        return id
    }

    fun snapshot() = CollectiveSnapshot(farmers(), fields(), visits(), impacts())
}
