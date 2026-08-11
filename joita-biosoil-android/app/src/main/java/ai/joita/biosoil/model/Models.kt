package ai.joita.biosoil.model

import java.util.UUID

enum class ReadingSource { USB, MANUAL, SAMPLE }

data class FieldDraft(
    val farmerName: String,
    val village: String,
    val district: String,
    val state: String,
    val fieldName: String,
    val crop: String,
    val areaAcres: Double?,
    val phone: String = "",
    val pincode: String = "",
    val plotNumber: String = "",
)

data class FieldProfile(
    val id: Long,
    val farmerId: Long,
    val farmerName: String,
    val village: String,
    val district: String,
    val state: String,
    val fieldName: String,
    val crop: String,
    val areaAcres: Double?,
    val phone: String = "",
    val pincode: String = "",
    val plotNumber: String = "",
)

data class SoilReading(
    val moisturePercent: Double,
    val temperatureCelsius: Double,
    val ecUsCm: Int,
    val ph: Double,
    val nitrogenMgKg: Int,
    val phosphorusMgKg: Int,
    val potassiumMgKg: Int,
    val fertilityMgKg: Int,
)

data class SoilTestRecord(
    val id: String = UUID.randomUUID().toString(),
    val fieldId: Long?,
    val fieldLabel: String,
    val crop: String,
    val source: ReadingSource,
    val reading: SoilReading,
    val score: Int,
    val status: SoilStatus,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val photoUri: String? = null,
    val sourceNote: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

enum class SoilStatus { GOOD, NEEDS_ATTENTION, URGENT }

enum class ParameterStatus { GOOD, LOW, HIGH }

data class ParameterAssessment(
    val key: String,
    val value: String,
    val status: ParameterStatus,
)

data class AdvisoryResult(
    val score: Int,
    val status: SoilStatus,
    val assessments: List<ParameterAssessment>,
    val immediateActions: List<String>,
    val followUpActions: List<String>,
)
