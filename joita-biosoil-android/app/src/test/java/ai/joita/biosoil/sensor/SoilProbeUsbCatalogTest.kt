package ai.joita.biosoil.sensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoilProbeUsbCatalogTest {
    @Test
    fun recognizesEveryProbeIdRecoveredFromOriginalApk() {
        assertTrue(SoilProbeUsbCatalog.isKnown(6_790, 29_987))
        assertTrue(SoilProbeUsbCatalog.isKnown(6_790, 21_795))
        assertTrue(SoilProbeUsbCatalog.isKnown(6_790, 21_778))
    }

    @Test
    fun rejectsOtherSerialDevicesSoKnownProbeCanBePrioritized() {
        assertFalse(SoilProbeUsbCatalog.isKnown(6_790, 1))
        assertFalse(SoilProbeUsbCatalog.isKnown(1, 29_987))
    }
}
