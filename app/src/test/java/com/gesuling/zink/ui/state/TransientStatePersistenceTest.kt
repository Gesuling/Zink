package com.gesuling.zink.ui.state

import com.gesuling.zink.ui.apply.ApplyStateManager
import com.gesuling.zink.ui.icons.IconsStateManager
import com.gesuling.zink.ui.requests.RequestsSelectionManager
import com.gesuling.zink.ui.requests.RequestsStateSnapshot
import com.gesuling.zink.ui.requests.decodeStringSetToIntSet
import com.gesuling.zink.ui.requests.encodeIntSetToStringSet
import com.gesuling.zink.ui.requests.restoreRequestsStateSnapshot
import com.gesuling.zink.ui.wallpapers.WallpapersStateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransientStatePersistenceTest {

    @Test
    fun requestsStateSnapshotDefaultsWhenSavedValuesAreNull() {
        val snapshot = restoreRequestsStateSnapshot(
            savedMode = null,
            savedQuery = null,
            savedPackages = null,
            savedThemedIds = null,
            savedManualName = null,
            savedManualPkg = null,
            savedManualNote = null
        )

        assertEquals(
            RequestsStateSnapshot(
                selectedMode = 0,
                searchQuery = "",
                selectedPackageNames = emptySet(),
                selectedThemedAppIds = emptySet(),
                manualAppName = "",
                manualPackageName = "",
                manualNote = ""
            ),
            snapshot
        )
    }

    @Test
    fun requestsStateSnapshotRestoresExactCustomValues() {
        val snapshot = restoreRequestsStateSnapshot(
            savedMode = 2,
            savedQuery = "Spotify",
            savedPackages = setOf("com.spotify.music", "com.discord"),
            savedThemedIds = setOf("1", "42", "invalid"),
            savedManualName = "Custom App",
            savedManualPkg = "com.custom.app",
            savedManualNote = "Silver theme preferred"
        )

        assertEquals(2, snapshot.selectedMode)
        assertEquals("Spotify", snapshot.searchQuery)
        assertEquals(setOf("com.spotify.music", "com.discord"), snapshot.selectedPackageNames)
        assertEquals(setOf(1, 42), snapshot.selectedThemedAppIds)
        assertEquals("Custom App", snapshot.manualAppName)
        assertEquals("com.custom.app", snapshot.manualPackageName)
        assertEquals("Silver theme preferred", snapshot.manualNote)
    }

    @Test
    fun requestsStateSnapshotCoercesOutOfRangeModeIndex() {
        val negativeMode = restoreRequestsStateSnapshot(-1, null, null, null, null, null, null)
        assertEquals(0, negativeMode.selectedMode)

        val overflowMode = restoreRequestsStateSnapshot(99, null, null, null, null, null, null)
        assertEquals(2, overflowMode.selectedMode)
    }

    @Test
    fun requestsIntSetSetEncodingAndDecodingMatchesData() {
        val originalIds = setOf(10, 20, 30)
        val encoded = encodeIntSetToStringSet(originalIds)
        assertEquals(setOf("10", "20", "30"), encoded)

        val decoded = decodeStringSetToIntSet(encoded)
        assertEquals(originalIds, decoded)

        val decodedWithCorrupted = decodeStringSetToIntSet(setOf("15", "abc", "25"))
        assertEquals(setOf(15, 25), decodedWithCorrupted)
    }

    @Test
    fun requestsSelectionManagerSnapshotReflectsCurrentState() {
        RequestsSelectionManager.resetForTesting()
        assertEquals(0, RequestsSelectionManager.selectedMode)
        assertEquals("", RequestsSelectionManager.searchQuery)
        assertTrue(RequestsSelectionManager.selectedPackageNames.isEmpty())
        assertTrue(RequestsSelectionManager.selectedThemedAppIds.isEmpty())

        RequestsSelectionManager.selectedMode = 1
        RequestsSelectionManager.searchQuery = "Search text"
        RequestsSelectionManager.selectedPackageNames.add("org.telegram.messenger")
        RequestsSelectionManager.selectedThemedAppIds.add(7)
        RequestsSelectionManager.manualAppName = "Manual App"

        val snapshot = RequestsSelectionManager.snapshot()
        assertEquals(1, snapshot.selectedMode)
        assertEquals("Search text", snapshot.searchQuery)
        assertEquals(setOf("org.telegram.messenger"), snapshot.selectedPackageNames)
        assertEquals(setOf(7), snapshot.selectedThemedAppIds)
        assertEquals("Manual App", snapshot.manualAppName)

        RequestsSelectionManager.resetForTesting()
        assertTrue(RequestsSelectionManager.selectedPackageNames.isEmpty())
    }

    @Test
    fun applyStateManagerResetsAndProvidesDefaults() {
        ApplyStateManager.resetForTesting()
        assertEquals("", ApplyStateManager.searchQuery)
        assertEquals("All", ApplyStateManager.selectedFilter)

        ApplyStateManager.searchQuery = "Nova"
        ApplyStateManager.selectedFilter = "Direct Apply"

        assertEquals("Nova", ApplyStateManager.searchQuery)
        assertEquals("Direct Apply", ApplyStateManager.selectedFilter)

        ApplyStateManager.resetForTesting()
        assertEquals("", ApplyStateManager.searchQuery)
        assertEquals("All", ApplyStateManager.selectedFilter)
    }

    @Test
    fun iconsStateManagerResetsAndProvidesDefaults() {
        IconsStateManager.resetForTesting()
        assertEquals("", IconsStateManager.searchQuery)
        assertEquals("All", IconsStateManager.selectedCategory)

        IconsStateManager.searchQuery = "Camera"
        IconsStateManager.selectedCategory = "Media"

        assertEquals("Camera", IconsStateManager.searchQuery)
        assertEquals("Media", IconsStateManager.selectedCategory)

        IconsStateManager.resetForTesting()
        assertEquals("", IconsStateManager.searchQuery)
        assertEquals("All", IconsStateManager.selectedCategory)
    }

    @Test
    fun wallpapersStateManagerResetsAndProvidesDefaults() {
        WallpapersStateManager.resetForTesting()
        assertFalse(WallpapersStateManager.showFavoritesOnly)

        WallpapersStateManager.showFavoritesOnly = true
        assertTrue(WallpapersStateManager.showFavoritesOnly)

        WallpapersStateManager.resetForTesting()
        assertFalse(WallpapersStateManager.showFavoritesOnly)
    }
}
