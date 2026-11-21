package com.gesuling.zink

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconAssetInstrumentedTest {
    @Test
    fun lawnchairAndKvaesitsoFallbacksDecodeFromPackagedAssets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        listOf("lawnchair", "kvaesitso").forEach { launcherId ->
            val encoded = context.assets.open("launcher_icon_${launcherId}.png.b64")
                .bufferedReader()
                .use { it.readText() }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            assertTrue("${launcherId} fallback asset must contain PNG bytes", bytes.size > 8)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertNotNull("${launcherId} fallback PNG must decode", bitmap)
            assertTrue("${launcherId} fallback must have positive dimensions", bitmap!!.width > 0 && bitmap.height > 0)
            bitmap.recycle()
        }
    }
}
