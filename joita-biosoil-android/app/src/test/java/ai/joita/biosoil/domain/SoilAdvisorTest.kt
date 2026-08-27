package ai.joita.biosoil.domain

import ai.joita.biosoil.model.SoilReading
import ai.joita.biosoil.model.SoilStatus
import ai.joita.biosoil.model.ParameterStatus
import ai.joita.biosoil.model.MeasurementConfidence
import ai.joita.biosoil.model.ReadingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoilAdvisorTest {
    @Test
    fun balancedReading_isGoodAndKeepsMaintenanceAdvice() {
        val result = SoilAdvisor.assess(
            SoilReading(38.0, 28.0, 650, 6.8, 180, 32, 190, 510),
        )
        assertEquals(100, result.score)
        assertEquals(SoilStatus.GOOD, result.status)
        assertEquals(listOf("maintain_practice"), result.immediateActions)
    }

    @Test
    fun outOfBandReading_neverClaimsDiagnosisAndReturnsActionKeys() {
        val result = SoilAdvisor.assess(
            SoilReading(8.0, 48.0, 3_200, 8.5, 40, 4, 40, 80),
        )
        assertEquals(30, result.score)
        assertEquals(SoilStatus.URGENT, result.status)
        assertTrue("prepare_moist_soil" in result.immediateActions)
        assertTrue("alkaline_ph" in result.immediateActions)
        assertTrue("high_salinity" in result.immediateActions)
        assertTrue("low_nutrients" in result.immediateActions)
        assertEquals(listOf("retest", "consult_expert"), result.followUpActions)
    }

    @Test
    fun drySoil_warnsThatNutrientAndEcReadingsNeedRetest() {
        val result = SoilAdvisor.assess(
            SoilReading(7.0, 29.0, 20, 6.7, 20, 3, 18, 30),
        )
        assertEquals("prepare_moist_soil", result.immediateActions.first())
        assertTrue("low_nutrients" in result.immediateActions)
    }

    @Test
    fun lowEcAndLowSaltIndex_areNotTreatedAsSalinityProblems() {
        val result = SoilAdvisor.assess(
            SoilReading(35.0, 27.0, 0, 6.8, 180, 32, 190, 0),
        )
        assertEquals(ParameterStatus.GOOD, result.assessments.first { it.key == "ec" }.status)
        assertEquals(ParameterStatus.GOOD, result.assessments.first { it.key == "fertility" }.status)
    }

    @Test
    fun drySoil_alwaysRequiresRetestRegardlessOfSampleCount() {
        val reading = SoilReading(7.0, 29.0, 20, 6.7, 20, 3, 18, 30)

        assertEquals(
            MeasurementConfidence.RETEST_REQUIRED,
            SoilAdvisor.measurementConfidence(reading, ReadingSource.USB, sampleCount = 5),
        )
    }

    @Test
    fun averagedUsbReading_isStrongerFieldIndicator() {
        val reading = SoilReading(38.0, 28.0, 650, 6.8, 180, 32, 190, 510)

        assertEquals(
            MeasurementConfidence.FIELD_INDICATOR,
            SoilAdvisor.measurementConfidence(reading, ReadingSource.USB, sampleCount = 3),
        )
        assertEquals(
            MeasurementConfidence.PRELIMINARY,
            SoilAdvisor.measurementConfidence(reading, ReadingSource.USB, sampleCount = 2),
        )
    }

    @Test
    fun sampleAndManualSources_areClearlyDistinguished() {
        val reading = SoilReading(38.0, 28.0, 650, 6.8, 180, 32, 190, 510)

        assertEquals(
            MeasurementConfidence.SAMPLE_DATA,
            SoilAdvisor.measurementConfidence(reading, ReadingSource.SAMPLE, sampleCount = 5),
        )
        assertEquals(
            MeasurementConfidence.MANUAL_ENTRY,
            SoilAdvisor.measurementConfidence(reading, ReadingSource.MANUAL, sampleCount = 1),
        )
    }
}
