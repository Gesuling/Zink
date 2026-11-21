package com.gesuling.zink

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gesuling.zink.ui.wallpapers.WallpaperDownloadQuality
import com.gesuling.zink.ui.wallpapers.WallpaperQualityPreferences
import com.gesuling.zink.ui.wallpapers.decodeWallpaperForDownload
import com.gesuling.zink.ui.wallpapers.wallpaperDownloadDimensions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallpaperDownloadQualityInstrumentedTest {
    @Test
    fun persistedQualityControlsDecodedWallpaperDimensions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("zink_wallpaper_prefs", 0)
        val previousValue = preferences.getString("download_quality", null)

        try {
            preferences.edit().remove("download_quality").commit()
            assertEquals(WallpaperDownloadQuality.ORIGINAL, WallpaperQualityPreferences.get(context))

            WallpaperQualityPreferences.set(context, WallpaperDownloadQuality.FHD_1080P)
            val selectedQuality = WallpaperQualityPreferences.get(context)
            assertEquals(WallpaperDownloadQuality.FHD_1080P, selectedQuality)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, R.drawable.wp_cherry_blossom, bounds)
            val expectedDimensions = wallpaperDownloadDimensions(
                bounds.outWidth,
                bounds.outHeight,
                selectedQuality
            )

            val processedBitmap = decodeWallpaperForDownload(
                context,
                R.drawable.wp_cherry_blossom,
                selectedQuality
            ) ?: error("Wallpaper decoding returned null")
            try {
                assertEquals(expectedDimensions.first, processedBitmap.width)
                assertEquals(expectedDimensions.second, processedBitmap.height)
            } finally {
                processedBitmap.recycle()
            }
        } finally {
            val editor = preferences.edit()
            if (previousValue == null) {
                editor.remove("download_quality")
            } else {
                editor.putString("download_quality", previousValue)
            }
            editor.commit()
        }
    }
}
