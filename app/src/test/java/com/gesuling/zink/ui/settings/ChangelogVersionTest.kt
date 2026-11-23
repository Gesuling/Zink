package com.gesuling.zink.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogVersionTest {

    @Test
    fun versionComparisonWorksCorrectly() {
        assertEquals(0, compareVersions("v0.1.4", "0.1.4"))
        assertEquals(1, compareVersions("v0.1.4", "v0.1.3"))
        assertEquals(-1, compareVersions("0.1.3", "v0.1.4"))
        assertEquals(0, compareVersions("0.1.4", "0.1.4"))
    }

    @Test
    fun releasePublishedStatusDistinguishesPublishedVsPlanned() {
        assertTrue(isReleasePublished("v0.1.1"))
        assertTrue(isReleasePublished("v0.1.2"))
        assertTrue(isReleasePublished("v0.1.3"))
        assertTrue(isReleasePublished("v0.1.4"))

        assertFalse(isReleasePublished("v0.1.5"))
    }
}
