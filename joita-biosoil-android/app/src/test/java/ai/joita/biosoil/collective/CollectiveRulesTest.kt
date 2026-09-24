package ai.joita.biosoil.collective

import org.junit.Assert.*
import org.junit.Test

class CollectiveRulesTest {
    @Test fun datesAreStrictAndChronologicallySortable() {
        assertNotNull(CollectiveRules.dateMillis("31-12-2026"))
        assertNull(CollectiveRules.dateMillis("31-02-2026"))
        assertNull(CollectiveRules.dateMillis("2026/08/30"))
        val snapshot = CollectiveSnapshot(visits = listOf(
            FieldVisit(id = 1, fieldId = 1, date = "30-12-2026", officer = "One"),
            FieldVisit(id = 2, fieldId = 1, date = "02-01-2027", officer = "Two"),
        ))
        assertEquals(2L, snapshot.recentVisits().first().id)
    }

    @Test fun fieldAndContactValidationRejectsUnsafeValues() {
        assertTrue(CollectiveRules.validArea("1.25"))
        assertFalse(CollectiveRules.validArea("NaN"))
        assertFalse(CollectiveRules.validArea("-1"))
        assertTrue(CollectiveRules.validCoordinates("28.6139", "77.2090"))
        assertFalse(CollectiveRules.validCoordinates("91", "77"))
        assertFalse(CollectiveRules.validCoordinates("28", ""))
        assertTrue(CollectiveRules.validPhone("+91 98765 43210"))
        assertFalse(CollectiveRules.validPhone("call-me"))
    }
}
