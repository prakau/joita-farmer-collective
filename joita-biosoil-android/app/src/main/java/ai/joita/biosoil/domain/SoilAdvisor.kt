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
            assess("ec", reading.ecUsCm.toDouble(), 100.0, 2_000.0, "µS/cm"),
            assess("ph", reading.ph, 6.0, 7.5, ""),
            assess("nitrogen", reading.nitrogenMgKg.toDouble(), 120.0, 280.0, "mg/kg"),
            assess("phosphorus", reading.phosphorusMgKg.toDouble(), 10.0, 60.0, "mg/kg"),
            assess("potassium", reading.potassiumMgKg.toDouble(), 110.0, 280.0, "mg/kg"),
            assess("fertility", reading.fertilityMgKg.toDouble(), 200.0, 850.0, "mg/kg"),
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
            if (assessments.first { it.key == "moisture" }.status == ParameterStatus.LOW) add("irrigate")
            if (assessments.first { it.key == "moisture" }.status == ParameterStatus.HIGH) add("improve_drainage")
            if (assessments.first { it.key == "ph" }.status != ParameterStatus.GOOD) add("confirm_ph")
            if (assessments.any { it.key in setOf("nitrogen", "phosphorus", "potassium") && it.status != ParameterStatus.GOOD }) add("nutrient_plan")
            if (isEmpty()) add("maintain_practice")
        }

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
}

