package ai.joita.biosoil.collective

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CollectiveRules {
    fun dateMillis(value: String): Long? {
        val pattern = when {
            value.matches(Regex("\\d{2}-\\d{2}-\\d{4}")) -> "dd-MM-yyyy"
            value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> "yyyy-MM-dd"
            else -> return null
        }
        val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        return parser.parse(value, position)?.takeIf { position.index == value.length }?.time
    }

    fun today(): String = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
    fun validArea(value: String): Boolean = value.toDoubleOrNull()?.let { it.isFinite() && it > 0 && it <= 1_000_000 } == true
    fun validCoordinates(lat: String, lng: String): Boolean =
        (lat.isBlank() && lng.isBlank()) ||
            (lat.toDoubleOrNull()?.let { it.isFinite() && it in -90.0..90.0 } == true &&
                lng.toDoubleOrNull()?.let { it.isFinite() && it in -180.0..180.0 } == true)
    fun validPhone(value: String): Boolean = value.isBlank() ||
        (value.all { it.isDigit() || it in "+ -()" } && value.count { it.isDigit() } in 7..15)
    fun acres(value: Double): String = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

data class CollectiveSnapshot(
    val farmers: List<Farmer> = emptyList(),
    val fields: List<FarmField> = emptyList(),
    val visits: List<FieldVisit> = emptyList(),
) {
    val acres: Double get() = fields.sumOf { it.acreage }
    fun fieldsFor(farmerId: Long) = fields.filter { it.farmerId == farmerId }
    fun visitsFor(fieldId: Long) = visits.filter { it.fieldId == fieldId }
    fun farmerFor(visit: FieldVisit) = fields.find { it.id == visit.fieldId }?.let { field -> farmers.find { it.id == field.farmerId } }
    fun recentVisits() = visits.sortedWith(compareByDescending<FieldVisit> { CollectiveRules.dateMillis(it.date) ?: it.createdAt }.thenByDescending { it.createdAt })
}
