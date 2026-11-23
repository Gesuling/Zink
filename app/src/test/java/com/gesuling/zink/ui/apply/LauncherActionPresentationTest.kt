package com.gesuling.zink.ui.apply

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
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
        assertEquals(15, LauncherRegistry.launchers.size)

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
    fun playStoreSourcesUseVerifiedDestinationsAndListingLabels() {
        val expectedUrls = mapOf(
            "action" to "https://play.google.com/store/apps/details?id=com.actionlauncher.playstore",
            "niagara" to "https://play.google.com/store/apps/details?id=bitpit.launcher",
            "nova" to "https://play.google.com/store/apps/details?id=com.teslacoilsw.launcher",
            "smart" to "https://play.google.com/store/apps/details?id=ginlemon.flowerfree",
            "hyperion" to "https://play.google.com/store/apps/details?id=projekt.launcher",
            "lawnchair" to "https://play.google.com/store/apps/details?id=app.lawnchair.play",
            "microsoft" to "https://play.google.com/store/apps/details?id=com.microsoft.launcher",
            "nothing" to "https://play.google.com/store/apps/details?id=com.nothing.launcher",
            "oneui" to "https://play.google.com/store/apps/details?id=com.sec.android.app.launcher",
            "apex_airborne" to "https://play.google.com/store/apps/details?id=com.home.launcher.apexlauncher"
        )

        expectedUrls.forEach { (id, url) ->
            val actions = state(id).actionPresentation
            assertEquals(url, actions.sourceUrl)
            assertEquals("OPEN GOOGLE PLAY LISTING", actions.sourceActionLabel)
            assertTrue(actions.sourceIsPlayStore)
        }

        val adwActions = state("adw").actionPresentation
        assertEquals("https://play.google.com/store/apps/details?hl=ro&id=org.adw.launcher", adwActions.sourceUrl)
        assertEquals("CHECK GOOGLE PLAY AVAILABILITY", adwActions.sourceActionLabel)
        assertTrue(adwActions.sourceIsPlayStore)

        val playStoreIds = expectedUrls.keys + "adw"
        playStoreIds.forEach { id ->
            val installedActions = state(id, installed = true).actionPresentation
            assertEquals("VIEW GOOGLE PLAY LISTING", installedActions.sourceActionLabel)
            assertNotNull(installedActions.openActionLabel)
        }
    }

    @Test
    fun launcherApplyMethodsReflectVerifiedCapabilities() {
        val nova = LauncherRegistry.launchers.single { it.id == "nova" }
        assertEquals(ApplyMethod.MANUAL, nova.applyMethod)
        assertNull(nova.directApplyAction)
        assertEquals("Manual Setup Required", state("nova", installed = true).methodBadge)
        assertEquals(
            listOf("Open Nova Settings.", "Navigate to Look & Feel.", "Open Icon Style.", "Select Icon Theme.", "Choose ZINK."),
            nova.setupSteps
        )

        val action = LauncherRegistry.launchers.single { it.id == "action" }
        assertEquals(ApplyMethod.DIRECT, action.applyMethod)
        assertEquals("com.actionlauncher.HONOR_ICON_PACK", action.directApplyAction)

        assertEquals(ApplyMethod.MANUAL, LauncherRegistry.launchers.single { it.id == "smart" }.applyMethod)
        assertEquals(ApplyMethod.MANUAL, LauncherRegistry.launchers.single { it.id == "adw" }.applyMethod)
        assertEquals(ApplyMethod.UNVERIFIED, LauncherRegistry.launchers.single { it.id == "apex" }.applyMethod)
        assertEquals(ApplyMethod.UNVERIFIED, LauncherRegistry.launchers.single { it.id == "apex_airborne" }.applyMethod)
        assertEquals(ApplyMethod.MANUAL, LauncherRegistry.launchers.single { it.id == "lawnchair" }.applyMethod)
        assertEquals(ApplyMethod.MANUAL, LauncherRegistry.launchers.single { it.id == "nothing" }.applyMethod)
        assertEquals(ApplyMethod.ALTERNATIVE, LauncherRegistry.launchers.single { it.id == "xiaomi_system" }.applyMethod)
    }

    @Test
    fun originalApexAndAirborneApexAreSeparatedWithIndependentMetadata() {
        val origApex = LauncherRegistry.launchers.single { it.id == "apex" }
        assertEquals("com.anddoes.launcher", origApex.packageNames.single())
        assertEquals("Android Does", origApex.developer)
        assertNull(origApex.playStoreUrl)
        assertEquals("https://www.apexlauncher.com/", origApex.officialWebsite)
        assertEquals(ListingAvailability.NO_OFFICIAL_LISTING, origApex.listingAvailability)

        val airborneApex = LauncherRegistry.launchers.single { it.id == "apex_airborne" }
        assertEquals("com.home.launcher.apexlauncher", airborneApex.packageNames.single())
        assertEquals("AirBorne", airborneApex.developer)
        assertEquals("https://play.google.com/store/apps/details?id=com.home.launcher.apexlauncher", airborneApex.playStoreUrl)
        assertNull(airborneApex.officialWebsite)
        assertEquals(ListingAvailability.LISTING_FOUND, airborneApex.listingAvailability)

        val adw = state("adw").actionPresentation
        assertEquals(ListingAvailability.REGION_DEPENDENT, state("adw").spec.listingAvailability)
        assertEquals("CHECK GOOGLE PLAY AVAILABILITY", adw.sourceActionLabel)
    }

    @Test
    fun launcherAvailabilityLabelsAndStatusBadgesAccuratelyReflectStoreAndDelistedStatus() {
        val origApex = state("apex")
        assertEquals("Delisted", origApex.availabilityLabel)
        assertEquals("Delisted", origApex.statusBadge)

        val airborneApex = state("apex_airborne")
        assertEquals("Available", airborneApex.availabilityLabel)
        assertEquals("Play listing found", airborneApex.statusBadge)

        assertEquals("Region Dependent", state("adw").availabilityLabel)
        assertEquals("Restricted", state("nothing").availabilityLabel)
        assertEquals("Restricted", state("oneui").availabilityLabel)
        assertEquals("GitHub Source", state("kvaesitso").availabilityLabel)
        assertEquals("Preinstalled", state("pixel").availabilityLabel)

        assertEquals("Installed", state("apex", installed = true).availabilityLabel)
        assertEquals("Installed", state("apex_airborne", installed = true).availabilityLabel)
        assertEquals("Installed", state("nova", installed = true).availabilityLabel)
    }

    @Test
    fun everyVerificationLinkHasAnAccurateLabelAndIntegrationStatus() {
        LauncherRegistry.launchers.forEach { spec ->
            assertEquals(spec.id, spec.verification.linkLabel == null, spec.verification.url == null)
            assertTrue(spec.id, spec.verification.evidence.isNotBlank())
            if (spec.applyMethod == ApplyMethod.DIRECT) {
                assertNotEquals(VerificationStatus.UNVERIFIED, spec.verification.status)
            }
        }
    }

    @Test
    fun onlyNiagaraAndActionRemainDirectAndPixelHasNoStandaloneListing() {
        assertEquals(setOf("niagara", "action"), LauncherRegistry.launchers.filter { it.applyMethod == ApplyMethod.DIRECT }.map { it.id }.toSet())
        assertNull(state("pixel").actionPresentation.sourceUrl)
        assertEquals(ListingAvailability.NO_OFFICIAL_LISTING, state("pixel").spec.listingAvailability)
        assertEquals(ListingAvailability.DEVICE_RESTRICTED, state("nothing").spec.listingAvailability)
        assertEquals(ListingAvailability.DEVICE_RESTRICTED, state("oneui").spec.listingAvailability)
    }

    @Test
    fun directApplyToastReportsDispatchWithoutClaimingApplicationSuccess() {
        val message = directApplyFeedbackMessage("Action Launcher")
        assertTrue(message.contains("Apply request sent"))
        assertTrue(message.contains("Confirm in launcher if prompted"))
        assertFalse(message.contains("applied successfully", ignoreCase = true))
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
        assertEquals("OPEN OFFICIAL GITHUB RELEASES", notInstalled.sourceActionLabel)
        assertEquals("VIEW OFFICIAL GITHUB SOURCE", state("kvaesitso", installed = true).actionPresentation.sourceActionLabel)
    }

    @Test
    fun novaActionAndCurrentLawnchairPackagesAreRecognizedFromHomeCandidates() {
        val packageNames = LauncherRegistry.launchers.flatMap { it.packageNames }.toSet()
        assertTrue("Nova package must be registered", "com.teslacoilsw.launcher" in packageNames)
        assertTrue("Action package must be registered", "com.actionlauncher.playstore" in packageNames)
        assertTrue("Current Lawnchair Play package must be registered", "app.lawnchair.play" in packageNames)
        assertTrue("Lawnchair GitHub package must be registered", "app.lawnchair" in packageNames)

        val candidates = listOf(
            LauncherRegistry.HomeLauncherCandidate("com.teslacoilsw.launcher", "com.teslacoilsw.launcher.NovaLauncher", "Nova Launcher"),
            LauncherRegistry.HomeLauncherCandidate("com.actionlauncher.playstore", "com.actionlauncher.ActionLauncher", "Action Launcher"),
            LauncherRegistry.HomeLauncherCandidate("app.lawnchair.play", "app.lawnchair.LawnchairLauncher", "Lawnchair")
        )
        val normalized = LauncherRegistry.normalizeHomeCandidates(candidates)
        assertEquals(3, normalized.size)
        val matchedRegistryIds = normalized.mapNotNull { candidate ->
            LauncherRegistry.launchers.singleOrNull { candidate.packageName in it.packageNames }?.id
        }
        assertEquals(listOf("nova", "action", "lawnchair"), matchedRegistryIds)
        assertEquals(ApplyMethod.MANUAL, LauncherRegistry.launchers.single { it.id == "lawnchair" }.applyMethod)
    }

    @Test
    fun defaultHomeMustMatchAConcreteCandidateAndMultipleLaunchersRemainDiscoverable() {
        val candidates = LauncherRegistry.normalizeHomeCandidates(
            listOf(
                LauncherRegistry.HomeLauncherCandidate("com.teslacoilsw.launcher", "com.teslacoilsw.launcher.NovaLauncher", "Nova"),
                LauncherRegistry.HomeLauncherCandidate("com.actionlauncher.playstore", "com.actionlauncher.ActionLauncher", "Action"),
                LauncherRegistry.HomeLauncherCandidate("app.lawnchair.play", "app.lawnchair.LawnchairLauncher", "Lawnchair"),
                LauncherRegistry.HomeLauncherCandidate("com.android.settings", "com.android.settings.FallbackHome", "Fallback")
            )
        )
        assertEquals(3, candidates.size)
        assertEquals(
            "app.lawnchair.play",
            LauncherRegistry.selectedHomePackage(
                "app.lawnchair.play",
                "app.lawnchair.LawnchairLauncher",
                candidates
            )
        )
        assertNull(
            LauncherRegistry.selectedHomePackage(
                "com.android.settings",
                "com.android.settings.ResolverActivity",
                candidates
            )
        )
    }

    @Test
    fun unknownHomeLauncherIsListedAsInstalledButUnsupported() {
        val unknown = LauncherRegistry.unregisteredHomeSpec(
            LauncherRegistry.HomeLauncherCandidate("vendor.launcher", "vendor.launcher.Home", "Vendor Home")
        )
        assertEquals(ApplyMethod.ALTERNATIVE, unknown.applyMethod)
        assertEquals(listOf("vendor.launcher"), unknown.packageNames)
        assertNull(unknown.directApplyAction)
        assertNotNull(unknown.unavailableMessage)
        assertEquals(ConfidenceLevel.UNVERIFIED, unknown.verification.confidence)
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

    @Test
    fun confidenceLevelShortLabelsMatchExpectedCategories() {
        assertEquals("Official", ConfidenceLevel.VERIFIED_OFFICIAL_DOCS.shortLabel)
        assertEquals("Source Code", ConfidenceLevel.VERIFIED_SOURCE_CODE.shortLabel)
        assertEquals("Integration Spec", ConfidenceLevel.VERIFIED_INTENT_SPEC.shortLabel)
        assertEquals("Community", ConfidenceLevel.COMMUNITY_VERIFIED.shortLabel)
        assertEquals("Unverified", ConfidenceLevel.UNVERIFIED.shortLabel)
    }

    @Test
    fun launcherConfidenceBreakdownCorrectlySummarizesAllLaunchers() {
        val allStates = LauncherRegistry.launchers.map { state(it.id) }
        val breakdown = LauncherRegistry.getConfidenceBreakdown(allStates)

        assertEquals(15, breakdown.total)
        assertEquals(11, breakdown.counts[ConfidenceLevel.VERIFIED_OFFICIAL_DOCS])
        assertEquals(0, breakdown.counts[ConfidenceLevel.VERIFIED_SOURCE_CODE])
        assertEquals(0, breakdown.counts[ConfidenceLevel.VERIFIED_INTENT_SPEC])
        assertEquals(2, breakdown.counts[ConfidenceLevel.COMMUNITY_VERIFIED])
        assertEquals(2, breakdown.counts[ConfidenceLevel.UNVERIFIED])
        assertEquals("11 OFFICIAL · 2 COMMUNITY · 2 UNVERIFIED", breakdown.summaryLabel())
    }

    @Test
    fun launcherConfidenceBreakdownHandlesSubsetsAndEmptyLists() {
        val emptyBreakdown = LauncherRegistry.getConfidenceBreakdown(emptyList())
        assertEquals(0, emptyBreakdown.total)
        assertEquals("0 LAUNCHERS", emptyBreakdown.summaryLabel())

        val directApplyStates = listOf(state("niagara"), state("action"))
        val directBreakdown = LauncherRegistry.getConfidenceBreakdown(directApplyStates)
        assertEquals(2, directBreakdown.total)
        assertEquals(1, directBreakdown.counts[ConfidenceLevel.VERIFIED_OFFICIAL_DOCS])
        assertEquals(1, directBreakdown.counts[ConfidenceLevel.COMMUNITY_VERIFIED])
        assertEquals(0, directBreakdown.counts[ConfidenceLevel.UNVERIFIED])
        assertEquals("1 OFFICIAL · 1 COMMUNITY", directBreakdown.summaryLabel())

        val unverifiedStates = listOf(state("xiaomi_system"), state("apex_airborne"))
        val unverifiedBreakdown = LauncherRegistry.getConfidenceBreakdown(unverifiedStates)
        assertEquals(2, unverifiedBreakdown.total)
        assertEquals(2, unverifiedBreakdown.counts[ConfidenceLevel.UNVERIFIED])
        assertEquals("2 UNVERIFIED", unverifiedBreakdown.summaryLabel())
    }

    @Test
    fun launcherRuntimeStateProvidesGranularConfidenceLabel() {
        assertEquals("Official", state("nova").confidenceLabel)
        assertEquals("Official", state("niagara").confidenceLabel)
        assertEquals("Official", state("pixel").confidenceLabel)
        assertEquals("Community", state("action").confidenceLabel)
        assertEquals("Unverified", state("xiaomi_system").confidenceLabel)
        assertEquals("Community", state("nothing").confidenceLabel)
        assertEquals("Unverified", state("apex_airborne").confidenceLabel)
    }

    @Test
    fun filterOptionsCorrectlyPartitionLaunchers() {
        val allStates = LauncherRegistry.launchers.map { state(it.id) }

        val directApply = allStates.filter { it.spec.applyMethod == ApplyMethod.DIRECT }
        assertEquals(setOf("niagara", "action"), directApply.map { it.spec.id }.toSet())

        val manualSetup = allStates.filter { it.spec.applyMethod == ApplyMethod.MANUAL }
        assertEquals(
            setOf("nova", "smart", "adw", "nothing", "lawnchair", "hyperion", "microsoft", "kvaesitso"),
            manualSetup.map { it.spec.id }.toSet()
        )

        val systemAndOthers = allStates.filter {
            it.spec.applyMethod == ApplyMethod.ALTERNATIVE || it.spec.applyMethod == ApplyMethod.UNVERIFIED
        }
        assertEquals(
            setOf("xiaomi_system", "pixel", "apex", "apex_airborne", "oneui"),
            systemAndOthers.map { it.spec.id }.toSet()
        )
    }

    @Test
    fun searchQueryMatchesLauncherNameDeveloperPackageOrTagline() {
        val allStates = LauncherRegistry.launchers.map { state(it.id) }

        val byName = allStates.filter { it.spec.name.contains("HyperOS", ignoreCase = true) }
        assertEquals(listOf("xiaomi_system"), byName.map { it.spec.id })

        val byDeveloper = allStates.filter { it.spec.developer.contains("Instabridge", ignoreCase = true) }
        assertEquals(listOf("nova"), byDeveloper.map { it.spec.id })

        val byPackage = allStates.filter { it.spec.packageNames.any { pkg -> pkg.contains("bitpit", ignoreCase = true) } }
        assertEquals(listOf("niagara"), byPackage.map { it.spec.id })

        val byTagline = allStates.filter { it.spec.tagline.contains("Samsung theme alternative", ignoreCase = true) }
        assertEquals(listOf("oneui"), byTagline.map { it.spec.id })
    }

    @Test
    fun xiaomiEnvironmentWithNovaActionLawnchairProducesExpectedInstalledStates() {
        val installedPackages = setOf("com.miui.home", "com.teslacoilsw.launcher", "com.actionlauncher.playstore", "app.lawnchair.play")
        val states = LauncherRegistry.launchers.map { spec ->
            val pkg = spec.packageNames.firstOrNull(installedPackages::contains)
            LauncherRuntimeState(
                spec = spec,
                isInstalled = pkg != null,
                isDefault = pkg == "com.miui.home",
                installedPackage = pkg
            )
        }

        val installedStates = states.filter { it.isInstalled }
        assertEquals(4, installedStates.size)
        assertEquals(setOf("xiaomi_system", "nova", "action", "lawnchair"), installedStates.map { it.spec.id }.toSet())

        val defaultState = states.single { it.isDefault }
        assertEquals("xiaomi_system", defaultState.spec.id)
        assertEquals(ApplyMethod.ALTERNATIVE, defaultState.spec.applyMethod)
        assertEquals("Unverified · Installed", "${defaultState.confidenceLabel} · ${defaultState.availabilityLabel}")

        val actionState = states.single { it.spec.id == "action" }
        assertEquals("Community · Installed", "${actionState.confidenceLabel} · ${actionState.availabilityLabel}")

        val breakdown = LauncherRegistry.getConfidenceBreakdown(installedStates)
        assertEquals(4, breakdown.total)
        assertEquals(2, breakdown.counts[ConfidenceLevel.VERIFIED_OFFICIAL_DOCS])
        assertEquals(1, breakdown.counts[ConfidenceLevel.COMMUNITY_VERIFIED])
        assertEquals(1, breakdown.counts[ConfidenceLevel.UNVERIFIED])
        assertEquals("2 OFFICIAL · 1 COMMUNITY · 1 UNVERIFIED", breakdown.summaryLabel())
    }
}
