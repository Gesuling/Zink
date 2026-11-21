package com.gesuling.zink.ui.wallpapers

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperDownloadQualityTest {
    @Test
    fun originalQualityKeepsSourceDimensions() {
        assertEquals(
            4000 to 3000,
            wallpaperDownloadDimensions(4000, 3000, WallpaperDownloadQuality.ORIGINAL)
        )
    }

    @Test
    fun fhdQualityScalesLandscapeTo1080pBounds() {
        assertEquals(
            1920 to 1080,
            wallpaperDownloadDimensions(3840, 2160, WallpaperDownloadQuality.FHD_1080P)
        )
    }

    @Test
    fun fhdQualityScalesPortraitTo1080pBounds() {
        assertEquals(
            1080 to 1920,
            wallpaperDownloadDimensions(2160, 3840, WallpaperDownloadQuality.FHD_1080P)
        )
    }

    @Test
    fun fhdQualityDoesNotUpscaleSmallerAssets() {
        assertEquals(
            1280 to 720,
            wallpaperDownloadDimensions(1280, 720, WallpaperDownloadQuality.FHD_1080P)
        )
    }
}
