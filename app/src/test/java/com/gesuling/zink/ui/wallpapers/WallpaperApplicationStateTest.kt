package com.gesuling.zink.ui.wallpapers

import android.app.WallpaperManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WallpaperApplicationStateTest {

    private fun getTargetFlags(target: WallpaperTarget): Int = when (target) {
        WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
        WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
        WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
    }

    private fun getTargetName(target: WallpaperTarget): String = when (target) {
        WallpaperTarget.HOME -> "Home screen"
        WallpaperTarget.LOCK -> "Lock screen"
        WallpaperTarget.BOTH -> "Home & Lock screens"
    }

    @Test
    fun wallpaperTargetFlagsAreMappedCorrectly() {
        assertEquals(WallpaperManager.FLAG_SYSTEM, getTargetFlags(WallpaperTarget.HOME))
        assertEquals(WallpaperManager.FLAG_LOCK, getTargetFlags(WallpaperTarget.LOCK))
        assertEquals(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, getTargetFlags(WallpaperTarget.BOTH))
    }

    @Test
    fun wallpaperTargetDisplayNamesAreAccurate() {
        assertEquals("Home screen", getTargetName(WallpaperTarget.HOME))
        assertEquals("Lock screen", getTargetName(WallpaperTarget.LOCK))
        assertEquals("Home & Lock screens", getTargetName(WallpaperTarget.BOTH))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidWallpaperDimensionsThrowException() {
        wallpaperDownloadDimensions(width = 0, height = 0, quality = WallpaperDownloadQuality.FHD_1080P)
    }

    @Test
    fun validWallpaperDimensionsScaleCorrectlyForFhd() {
        val scaled = wallpaperDownloadDimensions(width = 4000, height = 3000, quality = WallpaperDownloadQuality.FHD_1080P)
        assertNotNull(scaled)
        assertEquals(1440, scaled.first)
        assertEquals(1080, scaled.second)
    }
}
