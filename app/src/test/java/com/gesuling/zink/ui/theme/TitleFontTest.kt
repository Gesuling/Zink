package com.gesuling.zink.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TitleFontTest {

    @Test
    fun titleFontOptionsParseCorrectlyAndFallbackToDefault() {
        assertEquals(TitleFontOption.GEIST, TitleFontOption.fromId("Geist"))
        assertEquals(TitleFontOption.NDOT_55, TitleFontOption.fromId("NDot55"))
        assertEquals(TitleFontOption.NDOT_57, TitleFontOption.fromId("NDot57"))

        // Graceful fallback for invalid or null options
        assertEquals(TitleFontOption.DEFAULT, TitleFontOption.fromId(null))
        assertEquals(TitleFontOption.DEFAULT, TitleFontOption.fromId("invalid_font_id"))
        assertEquals(TitleFontOption.DEFAULT, TitleFontOption.fromId(""))
    }

    @Test
    fun allTitleFontOptionsHaveValidFontFamilies() {
        TitleFontOption.entries.forEach { option ->
            assertNotNull(option.id)
            assertNotNull(option.label)
            assertNotNull(option.fontFamily)
        }
    }
}
