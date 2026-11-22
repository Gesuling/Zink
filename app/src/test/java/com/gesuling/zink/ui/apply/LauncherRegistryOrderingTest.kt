package com.gesuling.zink.ui.apply

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherRegistryOrderingTest {

    @Test
    fun launcherEntriesAppearInIntendedOrderAndSize() {
        val launchers = LauncherRegistry.launchers
        assertEquals(15, launchers.size)
        val expectedOrderIds = listOf(
            "nova", "niagara", "smart", "action", "adw",
            "apex", "apex_airborne", "nothing", "lawnchair",
            "hyperion", "microsoft", "kvaesitso", "oneui",
            "xiaomi_system", "pixel"
        )
        assertEquals(expectedOrderIds, launchers.map { it.id })
    }

    @Test
    fun eachLauncherRetainsCorrectCompatibilityCategoryAndMethod() {
        val methods = LauncherRegistry.launchers.associate { it.id to it.applyMethod }
        assertEquals(ApplyMethod.MANUAL, methods["nova"])
        assertEquals(ApplyMethod.DIRECT, methods["niagara"])
        assertEquals(ApplyMethod.MANUAL, methods["smart"])
        assertEquals(ApplyMethod.DIRECT, methods["action"])
        assertEquals(ApplyMethod.MANUAL, methods["adw"])
        assertEquals(ApplyMethod.UNVERIFIED, methods["apex"])
        assertEquals(ApplyMethod.UNVERIFIED, methods["apex_airborne"])
        assertEquals(ApplyMethod.UNVERIFIED, methods["nothing"])
        assertEquals(ApplyMethod.MANUAL, methods["lawnchair"])
        assertEquals(ApplyMethod.MANUAL, methods["hyperion"])
        assertEquals(ApplyMethod.MANUAL, methods["microsoft"])
        assertEquals(ApplyMethod.MANUAL, methods["kvaesitso"])
        assertEquals(ApplyMethod.ALTERNATIVE, methods["oneui"])
        assertEquals(ApplyMethod.ALTERNATIVE, methods["xiaomi_system"])
        assertEquals(ApplyMethod.ALTERNATIVE, methods["pixel"])
    }

    @Test
    fun filteringLauncherStatesDoesNotMutateOriginalRegistry() {
        val originalSize = LauncherRegistry.launchers.size
        val directApplyLaunchers = LauncherRegistry.launchers.filter { it.applyMethod == ApplyMethod.DIRECT }
        assertEquals(2, directApplyLaunchers.size)
        assertEquals(originalSize, LauncherRegistry.launchers.size)
    }

    @Test
    fun unknownOrNewlyDiscoveredLaunchersReceiveCorrectFallbackBehavior() {
        val unknownCandidate = LauncherRegistry.HomeLauncherCandidate(
            packageName = "unknown.vendor.launcher",
            activityName = "unknown.vendor.launcher.HomeActivity",
            label = "Vendor Launcher"
        )
        val fallbackSpec = LauncherRegistry.unregisteredHomeSpec(unknownCandidate)

        assertEquals("installed_unknown_vendor_launcher", fallbackSpec.id)
        assertEquals("Vendor Launcher", fallbackSpec.name)
        assertEquals(listOf("unknown.vendor.launcher"), fallbackSpec.packageNames)
        assertEquals(ApplyMethod.ALTERNATIVE, fallbackSpec.applyMethod)
        assertNull(fallbackSpec.directApplyAction)
        assertNotNull(fallbackSpec.unavailableMessage)
        assertEquals(ConfidenceLevel.UNVERIFIED, fallbackSpec.verification.confidence)
    }

    @Test
    fun preservesSpecificLauncherBehaviorsForNovaActionLawnchairAndXiaomi() {
        val nova = LauncherRegistry.launchers.single { it.id == "nova" }
        assertEquals(ApplyMethod.MANUAL, nova.applyMethod)
        assertNull(nova.directApplyAction)

        val action = LauncherRegistry.launchers.single { it.id == "action" }
        assertEquals(ApplyMethod.DIRECT, action.applyMethod)
        assertEquals("com.actionlauncher.HONOR_ICON_PACK", action.directApplyAction)

        val lawnchair = LauncherRegistry.launchers.single { it.id == "lawnchair" }
        assertTrue("app.lawnchair.play" in lawnchair.packageNames)
        assertEquals(ApplyMethod.MANUAL, lawnchair.applyMethod)

        val xiaomi = LauncherRegistry.launchers.single { it.id == "xiaomi_system" }
        assertEquals(listOf("com.miui.home"), xiaomi.packageNames)
        assertEquals(ApplyMethod.ALTERNATIVE, xiaomi.applyMethod)
    }
}
