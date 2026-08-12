package ai.joita.biosoil.domain

import ai.joita.biosoil.model.AdvisoryResult
import ai.joita.biosoil.model.ParameterAssessment
import ai.joita.biosoil.model.ParameterStatus
import ai.joita.biosoil.model.SoilReading
import ai.joita.biosoil.model.SoilStatus
import kotlin.math.roundToInt

object SoilAdvisor {
    fun assess(reading: SoilReading): AdvisoryResult {
        val assessments = listOf(
            assess("moisture", reading.moisturePercent, 20.0, 60.0, "%"),
            assess("temperature", reading.temperatureCelsius, 10.0, 40.0, "°C"),
            assessUpperLimit("ec", reading.ecUsCm.toDouble(), 2_000.0, "µS/cm"),
            assess("ph", reading.ph, 6.0, 7.5, ""),
            assess("nitrogen", reading.nitrogenMgKg.toDouble(), 120.0, 280.0, "mg/kg"),
            assess("phosphorus", reading.phosphorusMgKg.toDouble(), 10.0, 60.0, "mg/kg"),
            assess("potassium", reading.potassiumMgKg.toDouble(), 110.0, 280.0, "mg/kg"),
            assessUpperLimit("fertility", reading.fertilityMgKg.toDouble(), 850.0, "mg/kg"),
        )

        val score = (100 - assessments.sumOf { assessment ->
            when (assessment.status) {
                ParameterStatus.GOOD -> 0
                ParameterStatus.LOW, ParameterStatus.HIGH -> 10
            }
        }).coerceIn(0, 100)

        val status = when {
            score >= 75 -> SoilStatus.GOOD
            score >= 45 -> SoilStatus.NEEDS_ATTENTION
            else -> SoilStatus.URGENT
        }

        val immediate = buildList {
            if (reading.moisturePercent < 15.0) add("prepare_moist_soil")
            if (assessments.first { it.key == "moisture" }.status == ParameterStatus.LOW) add("irrigate")
            if (assessments.first { it.key == "moisture" }.status == ParameterStatus.HIGH) add("improve_drainage")
            when (assessments.first { it.key == "ph" }.status) {
                ParameterStatus.LOW -> add("acidic_ph")
                ParameterStatus.HIGH -> add("alkaline_ph")
                ParameterStatus.GOOD -> Unit
            }
            if (assessments.first { it.key == "ec" }.status == ParameterStatus.HIGH) add("high_salinity")
            if (assessments.any { it.key in setOf("nitrogen", "phosphorus", "potassium") && it.status == ParameterStatus.LOW }) {
                add("low_nutrients")
            }
            if (assessments.any { it.key in setOf("nitrogen", "phosphorus", "potassium") && it.status == ParameterStatus.HIGH }) {
                add("high_nutrients")
            }
            if (isEmpty()) add("maintain_practice")
        }.distinct()

        return AdvisoryResult(
            score = score,
            status = status,
            assessments = assessments,
            immediateActions = immediate,
            followUpActions = listOf("retest", "consult_expert"),
        )
    }

    private fun assess(
        key: String,
        value: Double,
        low: Double,
        high: Double,
        unit: String,
    ): ParameterAssessment {
        val status = when {
            value < low -> ParameterStatus.LOW
            value > high -> ParameterStatus.HIGH
            else -> ParameterStatus.GOOD
        }
        val display = if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
        return ParameterAssessment(key, listOf(display, unit).filter { it.isNotBlank() }.joinToString(" "), status)
    }

    private fun assessUpperLimit(
        key: String,
        value: Double,
        high: Double,
        unit: String,
    ): ParameterAssessment {
        val status = if (value > high) ParameterStatus.HIGH else ParameterStatus.GOOD
        val display = if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
        return ParameterAssessment(key, listOf(display, unit).filter { it.isNotBlank() }.joinToString(" "), status)
    }
}
