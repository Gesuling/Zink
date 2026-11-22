package com.gesuling.zink.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogVersionTest {

    @Test
    fun versionComparisonWorksCorrectly() {
        assertEquals(0, compareVersions("v0.1.2", "0.1.2"))
        assertEquals(1, compareVersions("v0.1.2", "v0.1.1"))
        assertEquals(-1, compareVersions("0.1.1", "v0.1.2"))
        assertEquals(0, compareVersions("0.1.1", "0.1.1"))
    }

    @Test
    fun releasePublishedStatusDistinguishesPublishedVsPlanned() {
        assertTrue(isReleasePublished("v0.1.1"))
        assertTrue(isReleasePublished("0.1.1"))

        assertFalse(isReleasePublished("v0.1.2"))
        assertFalse(isReleasePublished("v0.1.3"))
    }
}
