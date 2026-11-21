package com.gesuling.zink.ui.wallpapers

import com.gesuling.zink.ui.theme.DEFAULT_HEADER_ARTWORK_ID
import com.gesuling.zink.ui.theme.resolveHeaderArtworkId
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperFavoritesPersistenceTest {
    @Test
    fun firstLaunchRestoresNoFavorites() {
        assertEquals(
            RestoredWallpaperFavorites(emptySet(), emptySet()),
            restoreWallpaperFavoriteIds(storedIds = null, availableIds = setOf(1, 2))
        )
    }

    @Test
    fun restorationFiltersUnknownIdsAndPreservesThemForLater() {
        val restored = restoreWallpaperFavoriteIds(
            storedIds = setOf("1", "999", "removed"),
            availableIds = setOf(1, 2)
        )

        assertEquals(setOf(1), restored.availableIds)
        assertEquals(setOf("999", "removed"), restored.unavailableIds)
        assertEquals(
            setOf("1", "2", "999", "removed"),
            encodeWallpaperFavoriteIds(setOf(1, 2), restored.unavailableIds)
        )
    }

    @Test
    fun missingHeaderArtworkFallsBackToFirstTimeDefault() {
        assertEquals(DEFAULT_HEADER_ARTWORK_ID, resolveHeaderArtworkId(savedId = null))
        assertEquals(DEFAULT_HEADER_ARTWORK_ID, resolveHeaderArtworkId(savedId = "Deleted Artwork"))
    }

    @Test
    fun selectedHeaderArtworkIsRestoredExactly() {
        assertEquals("Nocturne", resolveHeaderArtworkId(savedId = "Nocturne"))
    }
}
