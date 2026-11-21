package com.gesuling.zink

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gesuling.zink.ui.theme.ZinkPreferences
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashScreenModeInstrumentedTest {
    @Test
    fun splashIconFollowsSystemModeAndIgnoresZinkAppearancePreference() {
        assumeTrue("Platform splash icon attributes require Android 12+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previousAppAppearance = ZinkPreferences.getPureAmoled(context)

        try {
            for (systemNightMode in listOf(false, true)) {
                for (zinkDarkAppearance in listOf(false, true)) {
                    ZinkPreferences.setPureAmoled(context, zinkDarkAppearance)

                    val expectedIcon = if (systemNightMode) {
                        R.drawable.zink_splash_icon_dark
                    } else {
                        R.drawable.zink_splash_icon_light
                    }
                    assertEquals(
                        "System night=$systemNightMode, ZINK dark=$zinkDarkAppearance",
                        expectedIcon,
                        resolveSplashIcon(context, systemNightMode)
                    )
                }
            }
        } finally {
            ZinkPreferences.setPureAmoled(context, previousAppAppearance)
        }
    }

    private fun resolveSplashIcon(context: Context, systemNightMode: Boolean): Int {
        val configuration = Configuration(context.resources.configuration).apply {
            val nightBits = if (systemNightMode) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightBits
        }
        val configuredContext = context.createConfigurationContext(configuration)
        val theme = configuredContext.resources.newTheme().apply {
            applyStyle(R.style.Theme_Zink, true)
        }
        val attributes = theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.windowSplashScreenAnimatedIcon)
        )
        return try {
            attributes.getResourceId(0, 0)
        } finally {
            attributes.recycle()
        }
    }
}
