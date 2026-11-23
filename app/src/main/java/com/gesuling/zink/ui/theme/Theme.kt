package com.gesuling.zink.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class ZinkColors(
    val background: Color,
    val surface: Color,
    val glass: Color,
    val cardBackground: Color
)

val GraphiteDarkColors = ZinkColors(
    background = Color(0xFF121212),
    surface = Color(0xFF1A1A1A),
    glass = Color(0xF01C1C20),
    cardBackground = Color(0xFF1A1A1A)
)

val AmoledBlackColors = ZinkColors(
    background = Color(0xFF000000),
    surface = Color(0xFF141414),
    glass = Color(0xF0121214),
    cardBackground = Color(0xFF141414)
)

val LocalZinkColors = compositionLocalOf { GraphiteDarkColors }
val LocalPureAmoled = compositionLocalOf { false }
val LocalTitleFont = compositionLocalOf { TitleFontOption.DEFAULT }

internal const val DEFAULT_HEADER_ARTWORK_ID = "Frieren"
internal val HEADER_ARTWORK_IDS = setOf(DEFAULT_HEADER_ARTWORK_ID, "Obsidian", "Nocturne")

internal fun resolveHeaderArtworkId(savedId: String?): String =
    savedId?.takeIf { it in HEADER_ARTWORK_IDS } ?: DEFAULT_HEADER_ARTWORK_ID

object ZinkPreferences {
    private const val PREF_NAME = "zink_theme_prefs"
    private const val KEY_PURE_AMOLED = "is_pure_amoled"
    private const val KEY_HEADER_ARTWORK = "home_header_artwork"
    private const val KEY_TITLE_FONT = "title_font_option"

    fun getPureAmoled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PURE_AMOLED, false)
    }

    fun setPureAmoled(context: Context, isPure: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PURE_AMOLED, isPure).apply()
    }

    fun getHeaderArtwork(context: Context): String {
        val savedId = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HEADER_ARTWORK, null)
        return resolveHeaderArtworkId(savedId)
    }

    fun setHeaderArtwork(context: Context, artworkId: String): String {
        val resolvedId = resolveHeaderArtworkId(artworkId)
        if (artworkId in HEADER_ARTWORK_IDS) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HEADER_ARTWORK, resolvedId)
                .apply()
        }
        return resolvedId
    }

    fun getTitleFont(context: Context): TitleFontOption {
        val savedId = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TITLE_FONT, null)
        return TitleFontOption.fromId(savedId)
    }

    fun setTitleFont(context: Context, fontOption: TitleFontOption) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TITLE_FONT, fontOption.id)
            .apply()
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun ZinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
