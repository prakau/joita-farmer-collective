package ai.joita.biosoil.collective

import org.junit.Assert.*
import org.junit.Test

class ImpactSchemaTest {
    private val baseline = mapOf("stage" to "बेसलाइन")
    private val anchors = mapOf("farmerRef" to "CCF-001", "plotRef" to "P-001", "projectAcres" to "1.5", "cropStage" to "गेहूँ / बढ़वार")
    @Test fun incompleteAnswersStayUnknownAndRequireOfficerAndStage() {
        assertNull(ImpactSchema.validate(baseline + anchors, "24-09-2026", "कर्मी"))
        assertNotNull(ImpactSchema.validate(emptyMap(), "24-09-2026", "कर्मी"))
        assertNotNull(ImpactSchema.validate(baseline, "24-09-2026", ""))
    }
    @Test fun readingsNeedValidRangesAndUnits() {
        listOf("ph" to "15", "moisture" to "101", "ec" to "NaN", "nitrogen" to "-1", "irrigationCount" to "1.5").forEach {
            assertNotNull(ImpactSchema.validate(baseline + it, "24-09-2026", "Officer"))
        }
        assertNotNull(ImpactSchema.validate(baseline + ("ec" to "0.5"), "24-09-2026", "Officer"))
        assertNull(ImpactSchema.validate(baseline + anchors + mapOf("ec" to "0.5", "ecUnit" to "dS/m", "ph" to "7.2", "temperature" to "-2"), "24-09-2026", "Officer"))
    }
    @Test fun datesAndStableQuestionKeysAreValidated() {
        assertNotNull(ImpactSchema.validate(baseline + anchors + ("followupDate" to "31-02-2026"), "24-09-2026", "Officer"))
        val keys = ImpactSchema.sections.flatMap { it.questions }.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(6, ImpactSchema.sections.size)
        assertTrue(keys.containsAll(listOf("farmAssistUse", "ph", "ec", "salinity", "moisture", "temperature", "nitrogen", "phosphorus", "potassium", "trialType", "co2Basis", "trainingPast", "noBurning")))
    }
}
