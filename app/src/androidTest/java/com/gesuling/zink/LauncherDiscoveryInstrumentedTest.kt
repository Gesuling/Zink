package com.gesuling.zink

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gesuling.zink.ui.apply.LauncherRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherDiscoveryInstrumentedTest {
    @Test
    fun runtimeSnapshotMatchesPackageManagersVisibleHomeActivities() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homeActivities = packageManager.queryIntentActivities(homeIntent, 0)
        val visibleHomeActivities = homeActivities.mapNotNull { it.activityInfo?.packageName }.toSet()
        val candidates = LauncherRegistry.normalizeHomeCandidates(homeActivities.mapNotNull { result ->
            result.activityInfo?.let { activity ->
                LauncherRegistry.HomeLauncherCandidate(
                    packageName = activity.packageName,
                    activityName = activity.name,
                    label = result.loadLabel(packageManager).toString()
                )
            }
        })
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
        val expectedDefault = LauncherRegistry.selectedHomePackage(
            resolved?.packageName,
            resolved?.name,
            candidates
        )

        val snapshot = LauncherRegistry.getRuntimeSnapshot(context)
        assertEquals(expectedDefault, snapshot.defaultHomePackage)
        snapshot.launchers.filter { it.isInstalled }.forEach { state ->
            assertTrue(
                "Installed launcher ${state.spec.name} must be a HOME activity returned by PackageManager",
                state.installedPackage in visibleHomeActivities
            )
        }
        assertEquals(
            expectedDefault,
            snapshot.launchers.firstOrNull { it.isDefault }?.installedPackage
        )
        assertFalse(
            "A resource overlay must not be surfaced as an installed HOME launcher",
            snapshot.launchers.any { it.installedPackage == "android.miui.home.launcher.res" }
        )
    }
}
