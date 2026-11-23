package com.gesuling.zink.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZinkUpdateTest {

    @Test
    fun testVersionAvailabilityComparison() {
        assertTrue(ZinkUpdateManager.isNewVersionAvailableWithVersion("v0.1.4", "0.1.3"))
        assertTrue(ZinkUpdateManager.isNewVersionAvailableWithVersion("0.1.5", "0.1.3"))
        assertFalse(ZinkUpdateManager.isNewVersionAvailableWithVersion("v0.1.3", "0.1.3"))
        assertFalse(ZinkUpdateManager.isNewVersionAvailableWithVersion("0.1.2", "0.1.3"))
    }
}
