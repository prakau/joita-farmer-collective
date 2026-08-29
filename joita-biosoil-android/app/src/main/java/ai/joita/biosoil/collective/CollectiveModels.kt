package ai.joita.biosoil.collective

data class Farmer(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val village: String,
    val latitude: String = "",
    val longitude: String = "",
    val leadFarmer: Boolean = false,
    val notes: String = "",
    val tenure: String = "Owned",
    val createdAt: Long = System.currentTimeMillis(),
)

data class FarmField(
    val id: Long = 0,
    val farmerId: Long,
    val acreage: Double,
    val crop: String,
    val variety: String = "",
    val season: String = "Kharif",
    val sowingDate: String = "",
    val soilType: String = "",
    val irrigation: String = "",
    val inputs: String = "",
    val boundaryNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class FieldVisit(
    val id: Long = 0,
    val fieldId: Long,
    val date: String,
    val officer: String,
    val cropStage: String = "",
    val observations: String = "",
    val issues: String = "",
    val recommendations: String = "",
    val yieldData: String = "",
    val notes: String = "",
    val photoPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
