package com.gesuling.zink.ui.requests

import com.gesuling.zink.ui.icons.AppIconItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestSerializationTest {

    private val sampleUnthemed = listOf(
        UnthemedAppItem("App One", "com.one.app", "com.one.app.MainActivity"),
        UnthemedAppItem("App Two", "com.two.app", "com.two.app.MainActivity"),
        UnthemedAppItem("App Three", "com.three.app", "com.three.app.MainActivity")
    )

    private val sampleThemed = listOf(
        AppIconItem(1, "Icon One", "com.one.themed", "System", 0),
        AppIconItem(2, "Icon Two", "com.two.themed", "System", 0)
    )

    @Test
    fun unthemedAppSerializationIncludesOnlySelectedAppsWithCorrectDetails() {
        val selectedPackages = setOf("com.one.app", "com.three.app")
        val result = serializeRequest(
            selectedMode = 0,
            installedUnthemedApps = sampleUnthemed,
            selectedPackageNames = selectedPackages,
            themedAppList = emptyList(),
            selectedThemedAppIds = emptySet(),
            manualAppName = "",
            manualPackageName = "",
            manualNote = ""
        )

        assertEquals(2, result.count)
        assertEquals("Zink Icon Request (2 apps)", result.subject)
        assertTrue(result.body.contains("App One (com.one.app)"))
        assertTrue(result.body.contains("App Three (com.three.app)"))
        assertFalse(result.body.contains("App Two"))
        assertTrue(result.body.contains("Component: ComponentInfo{com.one.app.MainActivity}"))
    }

    @Test
    fun themedAppSerializationIncludesOnlySelectedItems() {
        val selectedIds = setOf(2)
        val result = serializeRequest(
            selectedMode = 1,
            installedUnthemedApps = emptyList(),
            selectedPackageNames = emptySet(),
            themedAppList = sampleThemed,
            selectedThemedAppIds = selectedIds,
            manualAppName = "",
            manualPackageName = "",
            manualNote = ""
        )

        assertEquals(1, result.count)
        assertEquals("Zink Icon Request (1 apps)", result.subject)
        assertTrue(result.body.contains("Icon Two (com.two.themed)"))
        assertFalse(result.body.contains("Icon One"))
    }

    @Test
    fun manualRequestSerializationIncludesAllFieldsAndOptionalNote() {
        val resultWithNote = serializeRequest(
            selectedMode = 2,
            installedUnthemedApps = emptyList(),
            selectedPackageNames = emptySet(),
            themedAppList = emptyList(),
            selectedThemedAppIds = emptySet(),
            manualAppName = "Spotify",
            manualPackageName = "com.spotify.music",
            manualNote = "Clean silver glyph"
        )

        assertEquals(1, resultWithNote.count)
        assertTrue(resultWithNote.body.contains("App Name: Spotify"))
        assertTrue(resultWithNote.body.contains("Package / Link: com.spotify.music"))
        assertTrue(resultWithNote.body.contains("Designer Note: Clean silver glyph"))

        val resultWithoutNote = serializeRequest(
            selectedMode = 2,
            installedUnthemedApps = emptyList(),
            selectedPackageNames = emptySet(),
            themedAppList = emptyList(),
            selectedThemedAppIds = emptySet(),
            manualAppName = "WhatsApp",
            manualPackageName = "com.whatsapp",
            manualNote = ""
        )

        assertEquals(1, resultWithoutNote.count)
        assertTrue(resultWithoutNote.body.contains("App Name: WhatsApp"))
        assertFalse(resultWithoutNote.body.contains("Designer Note"))
    }

    @Test
    fun emptySubmissionsHandledAppropriatelyWithZeroCount() {
        val result = serializeRequest(
            selectedMode = 0,
            installedUnthemedApps = sampleUnthemed,
            selectedPackageNames = emptySet(),
            themedAppList = sampleThemed,
            selectedThemedAppIds = emptySet(),
            manualAppName = "",
            manualPackageName = "",
            manualNote = ""
        )

        assertEquals(0, result.count)
        assertEquals("Zink Icon Request (0 apps)", result.subject)
        assertTrue(result.body.contains("Requested Unthemed Apps (0)"))
    }
}
