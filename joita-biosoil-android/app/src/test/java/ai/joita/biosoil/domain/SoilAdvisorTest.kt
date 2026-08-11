package ai.joita.biosoil.domain

import ai.joita.biosoil.model.SoilReading
import ai.joita.biosoil.model.SoilStatus
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
        assertEquals(20, result.score)
        assertEquals(SoilStatus.URGENT, result.status)
        assertTrue("irrigate" in result.immediateActions)
        assertTrue("confirm_ph" in result.immediateActions)
        assertTrue("nutrient_plan" in result.immediateActions)
        assertEquals(listOf("retest", "consult_expert"), result.followUpActions)
    }
}
