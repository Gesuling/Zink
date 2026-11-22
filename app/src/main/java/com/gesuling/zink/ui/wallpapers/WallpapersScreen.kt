package com.gesuling.zink.ui.wallpapers

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gesuling.zink.R
import com.gesuling.zink.ui.anim.glowingBorderOnPress
import com.gesuling.zink.ui.anim.pressScale
import com.gesuling.zink.ui.components.ZinkPageHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

import com.gesuling.zink.ui.theme.LocalZinkColors

private val Silver = Color(0xFFE0E0E0)
private val Muted = Color(0xFF888888)

data class WallpaperItem(
    val id: Int,
    val title: String,
    val category: String,
    val fullResId: Int,
    val thumbResId: Int
)

internal fun filterWallpaperItems(
    wallpapers: List<WallpaperItem>,
    favoriteIds: Set<Int>,
    favoritesOnly: Boolean
): List<WallpaperItem> =
    if (favoritesOnly) wallpapers.filter { it.id in favoriteIds } else wallpapers

enum class WallpaperDownloadQuality(val label: String, val maxWidth: Int?, val maxHeight: Int?) {
    ORIGINAL("4K Original", null, null),
    FHD_1080P("FHD 1080p", 1920, 1080)
}

object WallpaperQualityPreferences {
    private const val PREFS_NAME = "zink_wallpaper_prefs"
    private const val KEY_DOWNLOAD_QUALITY = "download_quality"

    fun get(context: Context): WallpaperDownloadQuality {
        val savedValue = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DOWNLOAD_QUALITY, WallpaperDownloadQuality.ORIGINAL.name)
        return WallpaperDownloadQuality.entries.firstOrNull { it.name == savedValue }
            ?: WallpaperDownloadQuality.ORIGINAL
    }

    fun set(context: Context, quality: WallpaperDownloadQuality) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DOWNLOAD_QUALITY, quality.name)
            .apply()
    }
}

internal fun wallpaperDownloadDimensions(
    width: Int,
    height: Int,
    quality: WallpaperDownloadQuality
): Pair<Int, Int> {
    require(width > 0 && height > 0) { "Bitmap dimensions must be positive" }
    val qualityWidth = quality.maxWidth ?: return width to height
    val qualityHeight = quality.maxHeight ?: return width to height
    val (maxWidth, maxHeight) = if (width >= height) {
        qualityWidth to qualityHeight
    } else {
        qualityHeight to qualityWidth
    }
    val scale = minOf(1f, maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    return (width * scale).roundToInt().coerceAtLeast(1) to
            (height * scale).roundToInt().coerceAtLeast(1)
}

internal fun decodeWallpaperForDownload(
    context: Context,
    resId: Int,
    quality: WallpaperDownloadQuality
): Bitmap? {
    if (quality == WallpaperDownloadQuality.ORIGINAL) {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val (targetWidth, targetHeight) =
        wallpaperDownloadDimensions(bounds.outWidth, bounds.outHeight, quality)

    var sampleSize = 1
    while (
        bounds.outWidth / (sampleSize * 2) >= targetWidth &&
        bounds.outHeight / (sampleSize * 2) >= targetHeight
    ) {
        sampleSize *= 2
    }

    val decoded = BitmapFactory.decodeResource(
        context.resources,
        resId,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: return null

    if (decoded.width == targetWidth && decoded.height == targetHeight) return decoded

    return Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also {
        decoded.recycle()
    }
}

enum class WallpaperTarget {
    HOME, LOCK, BOTH
}

enum class WallpaperFitMode {
    FIT, ORIGINAL, FILL
}

internal data class RestoredWallpaperFavorites(
    val availableIds: Set<Int>,
    val unavailableIds: Set<String>
)

internal fun restoreWallpaperFavoriteIds(
    storedIds: Set<String>?,
    availableIds: Set<Int>
): RestoredWallpaperFavorites {
    val favorites = mutableSetOf<Int>()
    val unavailable = mutableSetOf<String>()

    storedIds.orEmpty().forEach { storedId ->
        val id = storedId.toIntOrNull()
        if (id != null && id in availableIds) {
            favorites += id
        } else {
            unavailable += storedId
        }
    }

    return RestoredWallpaperFavorites(favorites, unavailable)
}

internal fun encodeWallpaperFavoriteIds(
    favoriteIds: Set<Int>,
    unavailableIds: Set<String>
): Set<String> = unavailableIds + favoriteIds.map(Int::toString)

// SharedPreferences-backed state shared by Home and Wallpapers.
object FavouritesManager {
    private const val PREFS_NAME = "zink_wallpaper_favorites"
    private const val KEY_FAVORITE_IDS = "favorite_ids"
    private var unavailableSavedIds: Set<String> = emptySet()

    val favoritedIds = mutableStateSetOf<Int>()

    fun restore(context: Context, availableIds: Set<Int>) {
        val storedIds = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAVORITE_IDS, emptySet())
            ?.toSet()
        val restored = restoreWallpaperFavoriteIds(storedIds, availableIds)
        unavailableSavedIds = restored.unavailableIds
        favoritedIds.clear()
        favoritedIds.addAll(restored.availableIds)
    }

    fun isFavorite(id: Int): Boolean = favoritedIds.contains(id)

    fun toggleFavorite(context: Context, id: Int, availableIds: Set<Int>) {
        if (id !in availableIds) return
        if (favoritedIds.contains(id)) {
            favoritedIds.remove(id)
        } else {
            favoritedIds.add(id)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(
                KEY_FAVORITE_IDS,
                encodeWallpaperFavoriteIds(favoritedIds.toSet(), unavailableSavedIds)
            )
            .apply()
    }
}

// Global Memory Bitmap Cache for 60 FPS scrolling without GC thrashing
object WallpaperBitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val memoryCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(resId: Int): Bitmap? = memoryCache.get(resId)

    fun put(resId: Int, bitmap: Bitmap) {
        if (get(resId) == null) {
            memoryCache.put(resId, bitmap)
        }
    }

    fun clear(): Long {
        val sizeInKb = memoryCache.size()
        memoryCache.evictAll()
        return sizeInKb.toLong() * 1024L
    }
}

val sampleWallpapers = listOf(
    WallpaperItem(1, "Hakuna Matata", "Sci-Fi", R.drawable.wp_dark_astronaut, R.drawable.thumb_dark_astronaut),
    WallpaperItem(2, "Gato", "Animals", R.drawable.wp_cat_hiding_dark_eye, R.drawable.thumb_cat_hiding_dark_eye),
    WallpaperItem(3, "Milkyway", "Space", R.drawable.wp_milky_way_galaxy, R.drawable.thumb_milky_way_galaxy),
    WallpaperItem(4, "Lunar Crater", "Space", R.drawable.wp_lunar_moon_crater, R.drawable.thumb_lunar_moon_crater),
    WallpaperItem(5, "IDK what's this", "Wild", R.drawable.wp_black_panther, R.drawable.thumb_black_panther),
    WallpaperItem(6, "bbnoconnection", "Retro", R.drawable.wp_desert_night_cactus, R.drawable.thumb_desert_night_cactus),
    WallpaperItem(7, "Ocean Wave", "Nature", R.drawable.wp_ocean_wave_night, R.drawable.thumb_ocean_wave_night),
    WallpaperItem(8, "Carbon leaves", "Minimal", R.drawable.wp_dark_hooded_figure, R.drawable.thumb_dark_hooded_figure),
    WallpaperItem(9, "Dont kmp", "Animals", R.drawable.wp_black_cat_eyes, R.drawable.thumb_black_cat_eyes),
    WallpaperItem(10, "Colorful Coral Reef", "Nature", R.drawable.wp_colorful_coral_reef, R.drawable.thumb_colorful_coral_reef),
    WallpaperItem(11, "foxu", "Nature", R.drawable.wp_dark_cave_explorer, R.drawable.thumb_dark_cave_explorer),
    WallpaperItem(12, "light beams", "Nature", R.drawable.wp_forest_light_beams, R.drawable.thumb_forest_light_beams),
    WallpaperItem(13, "Floating Island", "Fantasy", R.drawable.wp_floating_island, R.drawable.thumb_floating_island),
    WallpaperItem(14, "Aurora Gradient Sky", "Nature", R.drawable.wp_aurora_gradient_sky, R.drawable.thumb_aurora_gradient_sky),
    WallpaperItem(15, "Golden Eiffel Tower", "Architecture", R.drawable.wp_golden_eiffel_tower, R.drawable.thumb_golden_eiffel_tower),
    WallpaperItem(16, "Ferrari", "Landscape", R.drawable.wp_night_mountain_road, R.drawable.thumb_night_mountain_road),
    WallpaperItem(17, "Snowy Mountain Night", "Landscape", R.drawable.wp_snowy_mountain_night, R.drawable.thumb_snowy_mountain_night),
    WallpaperItem(18, "I am speed", "Automotive", R.drawable.wp_orange_sports_car, R.drawable.thumb_orange_sports_car),
    WallpaperItem(19, "very cool", "Abstract", R.drawable.wp_dark_line_abstract, R.drawable.thumb_dark_line_abstract),
    WallpaperItem(20, "Me and bro", "Tech", R.drawable.wp_camera_lens, R.drawable.thumb_camera_lens),
    WallpaperItem(21, "weeb", "Minimal", R.drawable.wp_chalk_board_math, R.drawable.thumb_chalk_board_math),
    WallpaperItem(22, "Bulb", "Tech", R.drawable.wp_keyboard_rgb_glow, R.drawable.thumb_keyboard_rgb_glow),
    WallpaperItem(23, "Sunflower Dark", "Nature", R.drawable.wp_sunflower_dark, R.drawable.thumb_sunflower_dark),
    WallpaperItem(24, "Deer Night Forest", "Nature", R.drawable.wp_deer_night_forest, R.drawable.thumb_deer_night_forest),
    WallpaperItem(25, "Potted Flowers", "Nature", R.drawable.wp_potted_flowers, R.drawable.thumb_potted_flowers),
    WallpaperItem(26, "Earth Orbit Dark", "Space", R.drawable.wp_earth_from_space_dark, R.drawable.thumb_earth_from_space_dark),
    WallpaperItem(27, "Earth Orbit Blue", "Space", R.drawable.wp_earth_from_space_blue, R.drawable.thumb_earth_from_space_blue),
    WallpaperItem(28, "Green Leaf Drops", "Nature", R.drawable.wp_green_leaf_drops, R.drawable.thumb_green_leaf_drops),
    WallpaperItem(29, "Mario Piranha Plant", "Gaming", R.drawable.wp_mario_piranha_plant, R.drawable.thumb_mario_piranha_plant),
    WallpaperItem(30, "Monochrome Range", "Landscape", R.drawable.wp_monochrome_mountain_range, R.drawable.thumb_monochrome_mountain_range),
    WallpaperItem(31, "Earth through Satellite", "Abstract", R.drawable.wp_retro_aperture_landscape, R.drawable.thumb_retro_aperture_landscape),
    WallpaperItem(32, "Nevermind", "Animals", R.drawable.wp_swimming_cat, R.drawable.thumb_swimming_cat),
    WallpaperItem(33, "Star Destroyer & X-Wing", "Sci-Fi", R.drawable.wp_star_destroyer_xwing, R.drawable.thumb_star_destroyer_xwing),
    WallpaperItem(34, "Spider-Man City", "Superheroes", R.drawable.wp_spiderman_city, R.drawable.thumb_spiderman_city),
    WallpaperItem(35, "Retro Game Boy", "Gaming", R.drawable.wp_retro_gameboy, R.drawable.thumb_retro_gameboy),
    WallpaperItem(36, "Colored Pencils", "Art", R.drawable.wp_colored_pencils, R.drawable.thumb_colored_pencils),
    WallpaperItem(37, "Cherry Blossom", "Nature", R.drawable.wp_cherry_blossom, R.drawable.thumb_cherry_blossom),
    WallpaperItem(38, "Red Corvette Logo", "Automotive", R.drawable.wp_red_corvette, R.drawable.thumb_red_corvette),
    WallpaperItem(39, "Blue Skyscraper", "Architecture", R.drawable.wp_blue_skyscraper, R.drawable.thumb_blue_skyscraper),
    WallpaperItem(40, "Nothing Schematic", "Tech", R.drawable.wp_nothing_schematic, R.drawable.thumb_nothing_schematic),
    WallpaperItem(41, "Geometric Noise", "Abstract", R.drawable.wp_geometric_noise, R.drawable.thumb_geometric_noise),
    WallpaperItem(42, "Nothing Glyph Tech", "Tech", R.drawable.wp_nothing_tech_glyph, R.drawable.thumb_nothing_tech_glyph),
    WallpaperItem(43, "White Calla Lily", "Flowers", R.drawable.wp_white_calla_lily, R.drawable.thumb_white_calla_lily)
)

fun renderCustomWallpaperBitmap(
    context: Context,
    resId: Int,
    scale: Float,
    panOffset: Offset,
    mode: WallpaperFitMode,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    val srcBitmap = BitmapFactory.decodeResource(context.resources, resId)
        ?: return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

    val outBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outBitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    val matrix = Matrix()
    val srcW = srcBitmap.width.toFloat()
    val srcH = srcBitmap.height.toFloat()
    val dstW = targetWidth.toFloat()
    val dstH = targetHeight.toFloat()

    val baseScale = when (mode) {
        WallpaperFitMode.FIT -> Math.min(dstW / srcW, dstH / srcH)
        WallpaperFitMode.FILL -> Math.max(dstW / srcW, dstH / srcH)
        WallpaperFitMode.ORIGINAL -> 1.0f
    }

    val finalScale = baseScale * scale
    val dx = (dstW - srcW * finalScale) / 2f + panOffset.x
    val dy = (dstH - srcH * finalScale) / 2f + panOffset.y

    matrix.postScale(finalScale, finalScale)
    matrix.postTranslate(dx, dy)

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    canvas.drawBitmap(srcBitmap, matrix, paint)
    return outBitmap
}

object WallpapersStateManager {
    private const val PREFS_NAME = "zink_wallpaper_favorites"
    private const val KEY_SHOW_FAVORITES_ONLY = "show_favorites_only"

    var showFavoritesOnly by mutableStateOf(false)

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        showFavoritesOnly = prefs.getBoolean(KEY_SHOW_FAVORITES_ONLY, false)
    }

    fun setShowFavoritesOnly(context: Context, show: Boolean) {
        showFavoritesOnly = show
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_FAVORITES_ONLY, show)
            .apply()
    }

    fun resetForTesting() {
        showFavoritesOnly = false
    }
}

@Composable
fun WallpapersScreen() {
    val context = LocalContext.current
    val colors = LocalZinkColors.current
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    val showFavoritesOnly = WallpapersStateManager.showFavoritesOnly
    val visibleWallpapers = filterWallpaperItems(
        wallpapers = sampleWallpapers,
        favoriteIds = FavouritesManager.favoritedIds,
        favoritesOnly = showFavoritesOnly
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ZinkPageHeader(
                title = "WALLPAPERS",
                subtitleContent = {
                    Text(
                        text = if (showFavoritesOnly) {
                            "${visibleWallpapers.size} SAVED FAVOURITES"
                        } else {
                            "${sampleWallpapers.size} CURATED DARK EXPERIENCES"
                        },
                        color = Muted,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }
            )
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x11FFFFFF))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All Wallpapers" to false, "Favourites" to true).forEach { (label, favoritesOnly) ->
                    val selected = showFavoritesOnly == favoritesOnly
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Color(0xFF282830) else Color.Transparent)
                            .clickable { WallpapersStateManager.setShowFavoritesOnly(context, favoritesOnly) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Silver else Muted,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (visibleWallpapers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "No Favourites Yet",
                        color = Silver,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tap the heart on a wallpaper to save it here.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            } else {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = navBarBottom + 104.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = visibleWallpapers,
                        key = { it.id },
                        contentType = { "wallpaper_grid_card" }
                    ) { wallpaper ->
                        WallpaperGridCard(
                            wallpaper = wallpaper,
                            onClick = { selectedWallpaper = wallpaper }
                        )
                    }
                }
            }
        }

        selectedWallpaper?.let { wallpaper ->
            FullWallpaperPreviewDialog(
                wallpaper = wallpaper,
                onDismiss = { selectedWallpaper = null }
            )
        }
    }
}

@Composable
fun FavouritesScreen() {
    val colors = LocalZinkColors.current
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    val favoritedList = remember {
        derivedStateOf {
            sampleWallpapers.filter { FavouritesManager.isFavorite(it.id) }
        }
    }.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ZinkPageHeader(
                title = "FAVOURITES",
                subtitleContent = {
                    AnimatedContent(
                        targetState = favoritedList.size,
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(250)) { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically(animationSpec = tween(250)) { height -> -height } + fadeOut())
                        },
                        label = "fav_count_anim"
                    ) { count ->
                        Text(
                            text = "$count SAVED WALLPAPERS",
                            color = Muted,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            )
            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = favoritedList.isEmpty(),
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300, easing = FastOutSlowInEasing), initialScale = 0.90f),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No Favourites Yet",
                            color = Silver,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap the heart icon on any wallpaper preview to save your favourite wallpapers here.",
                            color = Muted,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = favoritedList.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = navBarBottom + 104.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = favoritedList,
                        key = { it.id },
                        contentType = { "wallpaper_grid_card" }
                    ) { wallpaper ->
                        WallpaperGridCard(
                            wallpaper = wallpaper,
                            onClick = { selectedWallpaper = wallpaper }
                        )
                    }
                }
            }
        }

        selectedWallpaper?.let { wallpaper ->
            FullWallpaperPreviewDialog(
                wallpaper = wallpaper,
                onDismiss = { selectedWallpaper = null }
            )
        }
    }
}

@Composable
fun WallpaperGridCard(
    wallpaper: WallpaperItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalZinkColors.current
    var bitmapState by remember(wallpaper.thumbResId) {
        mutableStateOf(WallpaperBitmapCache.get(wallpaper.thumbResId))
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.92f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "card_entry_scale"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "card_entry_alpha"
    )

    LaunchedEffect(wallpaper.thumbResId) {
        if (bitmapState == null) {
            withContext(Dispatchers.IO) {
                val decoded = BitmapFactory.decodeResource(context.resources, wallpaper.thumbResId)
                decoded?.let {
                    WallpaperBitmapCache.put(wallpaper.thumbResId, it)
                    withContext(Dispatchers.Main) {
                        bitmapState = it
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .glowingBorderOnPress(RoundedCornerShape(18.dp))
            .pressScale(onClick = onClick)
    ) {
        val bitmap = bitmapState
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(wallpaper.thumbResId),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = wallpaper.title,
                color = Silver,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FullWallpaperPreviewDialog(
    wallpaper: WallpaperItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }

    // Fit Mode: Original selected by default
    var fitMode by remember { mutableStateOf(WallpaperFitMode.ORIGINAL) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var showApplyOptions by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    var isAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAppeared = true
    }

    val previewScale by animateFloatAsState(
        targetValue = if (isAppeared) 1.0f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "preview_scale"
    )

    val previewAlpha by animateFloatAsState(
        targetValue = if (isAppeared) 1.0f else 0f,
        animationSpec = tween(250),
        label = "preview_alpha"
    )

    val topBarOffset by animateDpAsState(
        targetValue = if (isAppeared) 0.dp else (-40).dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "top_bar_offset"
    )

    val bottomBarOffset by animateDpAsState(
        targetValue = if (isAppeared) 0.dp else 40.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "bottom_bar_offset"
    )

    val favScale = remember { Animatable(1.0f) }
    val downloadScale = remember { Animatable(1.0f) }

    fun downloadWallpaper() {
        if (isDownloading) return
        isDownloading = true
        scope.launch {
            downloadScale.animateTo(1.25f, spring(stiffness = Spring.StiffnessHigh))
            downloadScale.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium))
        }

        val quality = WallpaperQualityPreferences.get(context)
        scope.launch(Dispatchers.IO) {
            var success = false
            var errorMsg = ""
            try {
                val bitmap = decodeWallpaperForDownload(context, wallpaper.fullResId, quality)
                    ?: throw IllegalStateException("Unable to decode wallpaper")
                try {
                    val cleanTitle = wallpaper.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                    val filename = "Zink_${cleanTitle}_${System.currentTimeMillis()}.jpg"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Zink")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            }
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            context.contentResolver.update(uri, contentValues, null, null)
                            success = true
                        }
                    } else {
                        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        val zinkDir = File(picturesDir, "Zink").apply { mkdirs() }
                        val imageFile = File(zinkDir, filename)
                        FileOutputStream(imageFile).use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        }
                        success = true
                    }
                } finally {
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: "Error downloading wallpaper"
            }

            withContext(Dispatchers.Main) {
                isDownloading = false
                if (success) {
                    Toast.makeText(context, "Wallpaper downloaded to gallery!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Download failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setWallpaper(target: WallpaperTarget) {
        isApplying = true
        scope.launch(Dispatchers.IO) {
            val wm = WallpaperManager.getInstance(context)
            var success = false
            var errorMsg = ""
            try {
                val flags = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                val processedBitmap = renderCustomWallpaperBitmap(
                    context = context,
                    resId = wallpaper.fullResId,
                    scale = zoomScale,
                    panOffset = panOffset,
                    mode = fitMode,
                    targetWidth = screenWidthPx,
                    targetHeight = screenHeightPx
                )
                wm.setBitmap(processedBitmap, null, true, flags)
                success = true
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: "Error applying wallpaper"
            }

            withContext(Dispatchers.Main) {
                isApplying = false
                if (success) {
                    val targetName = when (target) {
                        WallpaperTarget.HOME -> "Home screen"
                        WallpaperTarget.LOCK -> "Lock screen"
                        WallpaperTarget.BOTH -> "Home & Lock screens"
                    }
                    Toast.makeText(context, "Wallpaper applied to $targetName!", Toast.LENGTH_SHORT).show()
                    showApplyOptions = false
                } else {
                    Toast.makeText(context, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val colors = LocalZinkColors.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // Interactive Fullscreen Wallpaper Viewport (Direct gesture control, no crop overlays)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.75f, 2.0f)
                            panOffset += pan
                        }
                    }
                    .graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                        alpha = previewAlpha
                    }
            ) {
                val contentScaleMode = when (fitMode) {
                    WallpaperFitMode.FIT -> ContentScale.Fit
                    WallpaperFitMode.FILL -> ContentScale.Crop
                    WallpaperFitMode.ORIGINAL -> ContentScale.None
                }

                Image(
                    painter = painterResource(wallpaper.fullResId),
                    contentDescription = wallpaper.title,
                    contentScale = contentScaleMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                )
            }

            // Top Header Bar (Close | Title | Reset)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = topBarOffset)
                    .height(110.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .pressScale()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Silver
                    )
                }

                Text(
                    text = wallpaper.title,
                    color = Silver,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Reset Positioning Button
                IconButton(
                    onClick = {
                        fitMode = WallpaperFitMode.ORIGINAL
                        zoomScale = 1.0f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .pressScale()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset positioning",
                        tint = Silver
                    )
                }
            }

            // Bottom Control Panel & Action Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = bottomBarOffset)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Zoom Slider Row (Placed neatly above Fit selector)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zoom: ${(zoomScale * 100).roundToInt()}%",
                        color = Silver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(80.dp)
                    )

                    Slider(
                        value = zoomScale,
                        onValueChange = { zoomScale = it },
                        valueRange = 0.75f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Silver,
                            activeTrackColor = Silver,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // 2. Fit Mode Selector Pills Row (Fit | Original | Fill)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        WallpaperFitMode.FIT to "Fit",
                        WallpaperFitMode.ORIGINAL to "Original",
                        WallpaperFitMode.FILL to "Fill"
                    ).forEach { (mode, label) ->
                        val isSelected = fitMode == mode

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Silver else Color(0x22141417))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    fitMode = mode
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF141417) else Silver,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 3. Main Action Row (Download | Set Wallpaper | Favourite)
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download Icon (Left)
                    IconButton(
                        onClick = { downloadWallpaper() },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = downloadScale.value
                                scaleY = downloadScale.value
                            }
                            .clip(CircleShape)
                            .background(Color(0xDD222226))
                            .glowingBorderOnPress(CircleShape)
                            .pressScale()
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Silver,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "Download wallpaper",
                                tint = Silver,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Set Wallpaper Primary Action Button (Center)
                    Button(
                        onClick = { showApplyOptions = true },
                        enabled = !isApplying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xDD222226),
                            contentColor = Silver
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .pressScale()
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Silver,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Applying...", fontSize = 14.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Set Wallpaper", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Favourite Icon (Right)
                    val isFav = FavouritesManager.isFavorite(wallpaper.id)
                    IconButton(
                        onClick = {
                            scope.launch {
                                favScale.animateTo(1.35f, spring(stiffness = Spring.StiffnessHigh))
                                favScale.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                            FavouritesManager.toggleFavorite(
                                context = context,
                                id = wallpaper.id,
                                availableIds = sampleWallpapers.mapTo(mutableSetOf()) { it.id }
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = favScale.value
                                scaleY = favScale.value
                            }
                            .clip(CircleShape)
                            .background(Color(0xDD222226))
                            .border(
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isFav) Color(0xFFFF5252).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f)
                                ),
                                shape = CircleShape
                            )
                            .pressScale()
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favourite wallpaper",
                            tint = if (isFav) Color(0xFFFF5252) else Silver,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (showApplyOptions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { showApplyOptions = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Color(0xFF141417))
                            .border(
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .navigationBarsPadding()
                            .padding(24.dp)
                            .clickable(enabled = false) {}
                    ) {
                        Text(
                            text = "Apply Wallpaper to",
                            color = Silver,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(Modifier.height(20.dp))

                        WallpaperOptionRow(
                            title = "Home Screen",
                            subtitle = "Apply custom crop to Home screen",
                            icon = Icons.Outlined.PhoneAndroid,
                            onClick = { setWallpaper(WallpaperTarget.HOME) }
                        )

                        Spacer(Modifier.height(10.dp))

                        WallpaperOptionRow(
                            title = "Lock Screen",
                            subtitle = "Apply custom crop to Lock screen",
                            icon = Icons.Outlined.Lock,
                            onClick = { setWallpaper(WallpaperTarget.LOCK) }
                        )

                        Spacer(Modifier.height(10.dp))

                        WallpaperOptionRow(
                            title = "Home & Lock Screen",
                            subtitle = "Apply custom crop to both screens",
                            icon = Icons.Outlined.Check,
                            onClick = { setWallpaper(WallpaperTarget.BOTH) }
                        )

                        Spacer(Modifier.height(16.dp))

                        TextButton(
                            onClick = { showApplyOptions = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Cancel", color = Muted, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalZinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .glowingBorderOnPress(RoundedCornerShape(16.dp))
            .pressScale(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Silver,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Preview
@Composable
fun WallpapersScreenPreview() {
    WallpapersScreen()
}
