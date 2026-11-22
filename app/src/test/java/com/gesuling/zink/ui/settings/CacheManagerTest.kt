package com.gesuling.zink.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheManagerTest {

    @Test
    fun testFormatBytes() {
        assertEquals("512 B", formatBytes(512L))
        assertEquals("1.0 KB", formatBytes(1024L))
        assertEquals("1.5 KB", formatBytes(1536L))
        assertEquals("1.0 MB", formatBytes(1024L * 1024L))
        assertEquals("2.5 MB", formatBytes((2.5 * 1024 * 1024).toLong()))
    }
}
