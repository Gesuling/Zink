package com.gesuling.zink.ui.apply

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherActionPresentationTest {
    private fun state(id: String, installed: Boolean = false): LauncherRuntimeState {
        val spec = LauncherRegistry.launchers.single { it.id == id }
        return LauncherRuntimeState(
            spec = spec,
            isInstalled = installed,
            isDefault = false,
            installedPackage = if (installed) spec.packageNames.first() else null
        )
    }

    @Test
    fun allRegisteredLaunchersHaveAnActionOrAvailabilityNotice() {
        assertEquals(14, LauncherRegistry.launchers.size)

        LauncherRegistry.launchers.forEach { spec ->
            val actions = state(spec.id).actionPresentation
            assertTrue(
                "${spec.name} must show a source action or an availability notice",
                actions.sourceActionLabel != null ||
                    actions.deviceRestrictionWarning != null ||
                    actions.availabilityMessage != null
            )
        }
    }

    @Test
    fun playStoreSourcesUseExpectedLinksAndInstallLabels() {
        val expectedUrls = mapOf(
            "action" to "https://play.google.com/store/apps/details?id=com.actionlauncher.playstore",
            "apex" to "https://play.google.com/store/apps/details?id=com.home.launcher.apexlauncher",
            "niagara" to "https://play.google.com/store/apps/details?id=bitpit.launcher",
            "nova" to "https://play.google.com/store/apps/details?id=com.teslacoilsw.launcher",
            "smart" to "https://play.google.com/store/apps/details?id=ginlemon.flowerfree",
            "hyperion" to "https://play.google.com/store/apps/details?id=projekt.launcher",
            "lawnchair" to "https://play.google.com/store/apps/details?id=app.lawnchair.play",
            "microsoft" to "https://play.google.com/store/apps/details?id=com.microsoft.launcher",
            "nothing" to "https://play.google.com/store/apps/details?id=com.nothing.launcher",
            "oneui" to "https://play.google.com/store/apps/details?id=com.sec.android.app.launcher",
            "adw" to "https://play.google.com/store/apps/details?id=org.adw.launcher"
        )

        expectedUrls.forEach { (id, url) ->
            val actions = state(id).actionPresentation
            assertEquals(url, actions.sourceUrl)
            assertEquals("INSTALL FROM GOOGLE PLAY", actions.sourceActionLabel)
            assertTrue(actions.sourceIsPlayStore)
        }

        expectedUrls.keys.forEach { id ->
            val installedActions = state(id, installed = true).actionPresentation
            assertEquals("VIEW ON GOOGLE PLAY", installedActions.sourceActionLabel)
            assertNotNull(installedActions.openActionLabel)
        }
    }

    @Test
    fun xiaomiSystemLauncherIsDetectedAsAlternativeWithoutDirectApply() {
        val xiaomi = LauncherRegistry.launchers.single { it.id == "xiaomi_system" }
        assertEquals(listOf("com.miui.home"), xiaomi.packageNames)
        assertEquals(ApplyMethod.ALTERNATIVE, xiaomi.applyMethod)
        assertNotNull(xiaomi.unavailableMessage)
        assertNull(xiaomi.directApplyAction)
    }

    @Test
    fun kvaesitsoUsesOfficialGithubReleaseSource() {
        val notInstalled = state("kvaesitso").actionPresentation
        assertEquals("https://github.com/MM2-0/Kvaesitso/releases", notInstalled.sourceUrl)
        assertEquals("DOWNLOAD FROM GITHUB", notInstalled.sourceActionLabel)
        assertEquals("VIEW ON GITHUB", state("kvaesitso", installed = true).actionPresentation.sourceActionLabel)
    }

    @Test
    fun restrictedLaunchersShowWarningsAndPixelHasNoInstallationUrl() {
        assertEquals(
            "Device restricted – Requires Nothing OS 2.5.0 or later.",
            state("nothing").actionPresentation.deviceRestrictionWarning
        )
        assertEquals(
            "Device restricted – Designed for Samsung Galaxy devices.",
            state("oneui").actionPresentation.deviceRestrictionWarning
        )
        val pixel = state("pixel").actionPresentation
        assertEquals(
            "Device restricted – Preinstalled on supported Google Pixel devices. Not available as a standalone installation for other devices.",
            pixel.deviceRestrictionWarning
        )
        assertNull(pixel.sourceUrl)
        assertNull(pixel.sourceActionLabel)
        assertNotNull(state("pixel", installed = true).actionPresentation.openActionLabel)
    }
}
