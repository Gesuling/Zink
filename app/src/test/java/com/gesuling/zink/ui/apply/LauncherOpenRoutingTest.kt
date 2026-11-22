package com.gesuling.zink.ui.apply

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherOpenRoutingTest {
    @Test
    fun prefersTheRequestedLaunchableActivity() {
        assertEquals(
            LauncherOpenRoute.APP_LAUNCHER,
            chooseLauncherOpenRoute(
                requestedPackage = "com.actionlauncher.playstore",
                isInstalled = true,
                launchIntentPackage = "com.actionlauncher.playstore",
                homeActivityPackage = "com.actionlauncher.playstore"
            )
        )
    }

    @Test
    fun usesTheRequestedHomeActivityWhenThereIsNoAppEntryPoint() {
        assertEquals(
            LauncherOpenRoute.HOME_ACTIVITY,
            chooseLauncherOpenRoute(
                requestedPackage = "com.miui.home",
                isInstalled = true,
                launchIntentPackage = null,
                homeActivityPackage = "com.miui.home"
            )
        )
    }

    @Test
    fun doesNotLaunchAHomeActivityFromAnotherPackage() {
        assertEquals(
            LauncherOpenRoute.UNAVAILABLE,
            chooseLauncherOpenRoute(
                requestedPackage = "com.nothing.launcher",
                isInstalled = true,
                launchIntentPackage = null,
                homeActivityPackage = "com.miui.home"
            )
        )
    }

    @Test
    fun doesNotUseAStaleLaunchIntentWhenPackageIsNotInstalled() {
        assertEquals(
            LauncherOpenRoute.UNAVAILABLE,
            chooseLauncherOpenRoute(
                requestedPackage = "com.nothing.launcher",
                isInstalled = false,
                launchIntentPackage = "com.nothing.launcher",
                homeActivityPackage = "com.nothing.launcher"
            )
        )
    }

    @Test
    fun missingPackageEntryPointsAreUnavailable() {
        assertEquals(
            LauncherOpenRoute.UNAVAILABLE,
            chooseLauncherOpenRoute(
                requestedPackage = "vendor.launcher",
                isInstalled = true,
                launchIntentPackage = null,
                homeActivityPackage = null
            )
        )
    }
}
