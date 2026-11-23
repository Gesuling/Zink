package com.gesuling.zink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gesuling.zink.R

// Centralized Font Families for ZINK Typography System
val GeistFontFamily = FontFamily.SansSerif
val GeistMonoFontFamily = FontFamily.Monospace
val NDot55FontFamily = FontFamily(Font(R.font.ndot_55))
val NDot57FontFamily = FontFamily(Font(R.font.ndot_57))

enum class TitleFontOption(val id: String, val label: String, val fontFamily: FontFamily) {
    GEIST("Geist", "Geist", GeistFontFamily),
    NDOT_55("NDot55", "NDot 55", NDot55FontFamily),
    NDOT_57("NDot57", "NDot 57", NDot57FontFamily);

    companion object {
        val DEFAULT = GEIST
        fun fromId(id: String?): TitleFontOption {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

// Material typography styles
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        letterSpacing = 4.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GeistMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
)
