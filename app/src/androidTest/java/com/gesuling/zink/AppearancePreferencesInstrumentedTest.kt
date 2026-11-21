package com.gesuling.zink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gesuling.zink.ui.theme.ZinkPreferences
import com.gesuling.zink.ui.wallpapers.FavouritesManager
import com.gesuling.zink.ui.wallpapers.sampleWallpapers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearancePreferencesInstrumentedTest {
    @Test
    fun wallpaperFavoritesRestoreAfterInMemoryStateReset() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("zink_wallpaper_favorites", 0)
        val previousValue = preferences.getStringSet("favorite_ids", null)?.toSet()
        val availableIds = sampleWallpapers.mapTo(mutableSetOf()) { it.id }

        try {
            preferences.edit().remove("favorite_ids").commit()
            FavouritesManager.restore(context, availableIds)
            assertTrue(FavouritesManager.favoritedIds.isEmpty())

            FavouritesManager.toggleFavorite(context, 1, availableIds)
            FavouritesManager.toggleFavorite(context, 2, availableIds)

            // Reload from SharedPreferences after clearing process-memory state.
            FavouritesManager.favoritedIds.clear()
            FavouritesManager.restore(context, availableIds)
            assertEquals(setOf(1, 2), FavouritesManager.favoritedIds.toSet())
        } finally {
            val editor = preferences.edit()
            if (previousValue == null) {
                editor.remove("favorite_ids")
            } else {
                editor.putStringSet("favorite_ids", previousValue)
            }
            editor.commit()
            FavouritesManager.restore(context, availableIds)
        }
    }

    @Test
    fun missingWallpaperIdsStayHiddenAndAreNotDiscardedOnToggle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("zink_wallpaper_favorites", 0)
        val previousValue = preferences.getStringSet("favorite_ids", null)?.toSet()
        val availableIds = sampleWallpapers.mapTo(mutableSetOf()) { it.id }

        try {
            preferences.edit().putStringSet("favorite_ids", setOf("1", "999", "removed")).commit()
            FavouritesManager.restore(context, availableIds)
            assertEquals(setOf(1), FavouritesManager.favoritedIds.toSet())

            FavouritesManager.toggleFavorite(context, 2, availableIds)
            assertEquals(setOf("1", "2", "999", "removed"), preferences.getStringSet("favorite_ids", emptySet()))
        } finally {
            val editor = preferences.edit()
            if (previousValue == null) {
                editor.remove("favorite_ids")
            } else {
                editor.putStringSet("favorite_ids", previousValue)
            }
            editor.commit()
            FavouritesManager.restore(context, availableIds)
        }
    }

    @Test
    fun headerArtworkUsesDefaultThenRestoresSavedChoiceAndHandlesUnknownId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("zink_theme_prefs", 0)
        val previousValue = preferences.getString("home_header_artwork", null)

        try {
            preferences.edit().remove("home_header_artwork").commit()
            assertEquals("Frieren", ZinkPreferences.getHeaderArtwork(context))

            ZinkPreferences.setHeaderArtwork(context, "Nocturne")
            assertEquals("Nocturne", ZinkPreferences.getHeaderArtwork(context))

            preferences.edit().putString("home_header_artwork", "Removed Artwork").commit()
            assertEquals("Frieren", ZinkPreferences.getHeaderArtwork(context))
            assertEquals("Removed Artwork", preferences.getString("home_header_artwork", null))
        } finally {
            val editor = preferences.edit()
            if (previousValue == null) {
                editor.remove("home_header_artwork")
            } else {
                editor.putString("home_header_artwork", previousValue)
            }
            editor.commit()
        }
    }
}
