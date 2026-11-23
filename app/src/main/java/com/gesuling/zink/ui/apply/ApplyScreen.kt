package com.gesuling.zink.ui.apply

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gesuling.zink.ui.anim.glowingBorderOnPress
import com.gesuling.zink.ui.anim.pressScale
import androidx.core.graphics.drawable.toBitmap
import com.gesuling.zink.ui.theme.LocalZinkColors
import com.gesuling.zink.ui.components.ZinkPageHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private val Silver = Color(0xFFF2F2F4)
private val Muted = Color(0xFF7C7C82)
private val CardBg = Color(0xFF121212)
private val CardBorder = Color.White.copy(alpha = 0.08f)

private val FilterOptions = listOf("All", "Direct Apply", "Manual Setup", "Installed", "System & Others")

private fun android.graphics.drawable.Drawable.toSafeBitmap(width: Int = 192, height: Int = 192): Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable && bitmap != null) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

@Composable
private fun LauncherAppIcon(
    launcherId: String?,
    packageName: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, launcherId, packageName) {
        value = withContext(Dispatchers.IO) {
            var icon: Bitmap? = null
            val pm = context.packageManager

            if (launcherId == "xiaomi_system") {
                icon = runCatching {
                    val encoded = context.assets.open("launcher_icon_xiaomi_system.png.b64")
                        .bufferedReader()
                        .use { it.readText() }
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            }
            if (packageName != null) {
                // 1. Try getApplicationIcon
                icon = runCatching {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationIcon(appInfo).toSafeBitmap(192, 192)
                }.onFailure { e ->
                    android.util.Log.d("ZinkIcon", "Package lookup / app icon failure for package '$packageName' (launcherId=$launcherId): ${e.javaClass.simpleName} - ${e.message}")
                }.getOrNull()

                // 2. Try HOME intent resolve info icon (critical for system home apps like com.miui.home)
                if (icon == null) {
                    icon = runCatching {
                        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(packageName)
                        val resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                            ?: pm.queryIntentActivities(homeIntent, 0).firstOrNull()
                        resolveInfo?.loadIcon(pm)?.toSafeBitmap(192, 192)
                    }.onFailure { e ->
                        android.util.Log.d("ZinkIcon", "HOME activity icon resolution failure for package '$packageName': ${e.javaClass.simpleName} - ${e.message}")
                    }.getOrNull()
                }

                // 3. Try LAUNCHER intent getLaunchIntentForPackage
                if (icon == null) {
                    icon = runCatching {
                        val launchIntent = pm.getLaunchIntentForPackage(packageName)
                        launchIntent?.let { pm.getActivityIcon(it)?.toSafeBitmap(192, 192) }
                    }.onFailure { e ->
                        android.util.Log.d("ZinkIcon", "Launch intent icon resolution failure for package '$packageName': ${e.javaClass.simpleName} - ${e.message}")
                    }.getOrNull()
                }
            }

            // 4. Try asset fallback
            if (icon == null && launcherId != null) {
                icon = runCatching {
                    val encoded = context.assets.open("launcher_icon_${launcherId}.png.b64")
                        .bufferedReader()
                        .use { it.readText() }
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.onFailure { e ->
                    android.util.Log.d("ZinkIcon", "Asset fallback failure for launcherId '$launcherId': ${e.javaClass.simpleName} - ${e.message}")
                }.getOrNull()
            }

            if (icon == null) {
                android.util.Log.e("ZinkIcon", "All icon resolution methods failed for packageName='$packageName', launcherId='$launcherId', description='$contentDescription'. Falling back to letter text.")
            }
            icon
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = contentDescription.take(1).uppercase(),
                color = Silver,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun openExternalSource(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: android.content.ActivityNotFoundException) {
        // No browser or compatible app is installed; keep the detail sheet open.
    }
}

private fun openGooglePlayListing(context: Context, httpsUrl: String) {
    val httpsUri = Uri.parse(httpsUrl)
    val packageId = httpsUri.getQueryParameter("id")
    val marketUri = packageId?.let {
        Uri.parse("market://details").buildUpon().appendQueryParameter("id", it).build()
    }

    if (marketUri != null) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, marketUri).setPackage("com.android.vending")
            )
            return
        } catch (_: android.content.ActivityNotFoundException) {
            // Fall through to the HTTPS listing when Google Play is not available.
        }
    }

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, httpsUri))
    } catch (_: android.content.ActivityNotFoundException) {
        // Keep this action silent if neither the Play Store nor a browser can open the listing.
    }
}

enum class SortOption(val label: String) {
    DEFAULT("Default"),
    ALPHABETICAL("Alphabetical (A–Z)"),
    COMPATIBILITY("Compatibility"),
    AVAILABILITY("Availability")
}

object ApplyStateManager {
    private const val PREFS_NAME = "zink_apply_prefs"
    private const val KEY_SEARCH_QUERY = "search_query"
    private const val KEY_SELECTED_FILTER = "selected_filter"
    private const val KEY_SELECTED_CONFIDENCE = "selected_confidence"
    private const val KEY_SORT_OPTION = "sort_option"

    var searchQuery by mutableStateOf("")
    var selectedFilter by mutableStateOf("All")
    var selectedConfidenceLevel by mutableStateOf<ConfidenceLevel?>(null)
    var sortOption by mutableStateOf(SortOption.DEFAULT)

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        searchQuery = prefs.getString(KEY_SEARCH_QUERY, "") ?: ""
        selectedFilter = prefs.getString(KEY_SELECTED_FILTER, "All") ?: "All"
        selectedConfidenceLevel = prefs.getString(KEY_SELECTED_CONFIDENCE, null)?.let { name ->
            runCatching { ConfidenceLevel.valueOf(name) }.getOrNull()
        }
        sortOption = prefs.getString(KEY_SORT_OPTION, null)?.let { name ->
            runCatching { SortOption.valueOf(name) }.getOrNull()
        } ?: SortOption.DEFAULT
    }

    fun setSearchQuery(context: Context, query: String) {
        searchQuery = query
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SEARCH_QUERY, query)
            .apply()
    }

    fun setSelectedFilter(context: Context, filter: String) {
        selectedFilter = filter
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_FILTER, filter)
            .apply()
    }

    fun toggleConfidenceLevel(context: Context, level: ConfidenceLevel) {
        selectedConfidenceLevel = if (selectedConfidenceLevel == level) null else level
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_CONFIDENCE, selectedConfidenceLevel?.name)
            .apply()
    }

    fun setSortOption(context: Context, option: SortOption) {
        sortOption = option
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SORT_OPTION, option.name)
            .apply()
    }

    fun resetForTesting() {
        searchQuery = ""
        selectedFilter = "All"
        selectedConfidenceLevel = null
        sortOption = SortOption.DEFAULT
    }
}

@Composable
fun ApplyScreen(launcherRefreshVersion: Int = 0) {
    val context = LocalContext.current
    val colors = LocalZinkColors.current

    var launcherSnapshot by remember(context) {
        mutableStateOf(LauncherRegistry.getRuntimeSnapshot(context))
    }
    val launcherStates = launcherSnapshot.launchers
    val defaultLauncherPkg = launcherSnapshot.defaultHomePackage
    var selectedLauncher by remember { mutableStateOf<LauncherRuntimeState?>(null) }
    val searchQuery = ApplyStateManager.searchQuery
    val selectedFilter = ApplyStateManager.selectedFilter
    val selectedConfidence = ApplyStateManager.selectedConfidenceLevel
    val sortOption = ApplyStateManager.sortOption

    val defaultLauncherState = launcherStates.firstOrNull { it.isDefault }
    val defaultLauncherName = defaultLauncherPkg?.let {
        LauncherRegistry.getApplicationLabel(context, it)
    } ?: "No default HOME launcher selected"

    LaunchedEffect(launcherRefreshVersion) {
        val refreshedSnapshot = LauncherRegistry.getRuntimeSnapshot(context)
        launcherSnapshot = refreshedSnapshot
        selectedLauncher = selectedLauncher?.let { selected ->
            refreshedSnapshot.launchers.firstOrNull { it.spec.id == selected.spec.id } ?: selected
        }
    }

    // Filter and search logic
    val filteredLaunchers = remember(launcherStates, searchQuery, selectedFilter, selectedConfidence, sortOption) {
        val filtered = launcherStates.filter { state ->
            val matchesSearch = searchQuery.isBlank() ||
                    state.spec.name.contains(searchQuery, ignoreCase = true) ||
                    state.spec.developer.contains(searchQuery, ignoreCase = true) ||
                    state.spec.tagline.contains(searchQuery, ignoreCase = true) ||
                    state.spec.packageNames.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                "Direct Apply" -> state.spec.applyMethod == ApplyMethod.DIRECT
                "Manual Setup" -> state.spec.applyMethod == ApplyMethod.MANUAL
                "Installed" -> state.isInstalled
                "System & Others" -> state.spec.applyMethod == ApplyMethod.ALTERNATIVE || state.spec.applyMethod == ApplyMethod.UNVERIFIED
                else -> true
            }

            val matchesConfidence = when (selectedConfidence) {
                ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                ConfidenceLevel.VERIFIED_SOURCE_CODE,
                ConfidenceLevel.VERIFIED_INTENT_SPEC -> {
                    state.spec.verification.confidence == ConfidenceLevel.VERIFIED_OFFICIAL_DOCS ||
                            state.spec.verification.confidence == ConfidenceLevel.VERIFIED_SOURCE_CODE ||
                            state.spec.verification.confidence == ConfidenceLevel.VERIFIED_INTENT_SPEC
                }
                ConfidenceLevel.COMMUNITY_VERIFIED -> state.spec.verification.confidence == ConfidenceLevel.COMMUNITY_VERIFIED
                ConfidenceLevel.UNVERIFIED -> state.spec.verification.confidence == ConfidenceLevel.UNVERIFIED
                null -> true
            }

            matchesSearch && matchesFilter && matchesConfidence
        }

        when (sortOption) {
            SortOption.ALPHABETICAL -> filtered.sortedBy { it.spec.name }
            SortOption.COMPATIBILITY -> filtered.sortedBy {
                when (it.spec.applyMethod) {
                    ApplyMethod.DIRECT -> 0
                    ApplyMethod.MANUAL -> 1
                    ApplyMethod.ALTERNATIVE -> 2
                    ApplyMethod.UNVERIFIED -> 3
                }
            }
            SortOption.AVAILABILITY -> filtered.sortedWith(
                compareByDescending<LauncherRuntimeState> { it.isInstalled }.thenBy { it.spec.name }
            )
            SortOption.DEFAULT -> filtered.sortedWith(
                compareByDescending<LauncherRuntimeState> { it.isDefault }
                    .thenByDescending { it.isInstalled }
                    .thenBy {
                        when (it.spec.applyMethod) {
                            ApplyMethod.DIRECT -> 0
                            ApplyMethod.MANUAL -> 1
                            ApplyMethod.ALTERNATIVE -> 2
                            ApplyMethod.UNVERIFIED -> 3
                        }
                    }
            )
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                title = "APPLY",
                subtitle = "LAUNCHER COMPATIBILITY & SETUP HUB"
            )
            Spacer(Modifier.height(4.dp))

            // ── Prominent Default Launcher Hero Card ──
            DefaultLauncherHeroCard(
                defaultName = defaultLauncherName,
                state = defaultLauncherState,
                launcherId = defaultLauncherState?.spec?.id,
                packageName = defaultLauncherPkg,
                onApply = {
                    if (defaultLauncherState != null) {
                        if (defaultLauncherState.spec.applyMethod == ApplyMethod.DIRECT) {
                            LauncherRegistry.executeDirectApply(context, defaultLauncherState)
                        } else {
                            selectedLauncher = defaultLauncherState
                        }
                    } else {
                        Toast.makeText(context, "Please follow guide for $defaultLauncherName", Toast.LENGTH_SHORT).show()
                    }
                },
                onViewGuide = {
                    if (defaultLauncherState != null) {
                        selectedLauncher = defaultLauncherState
                    } else if (defaultLauncherPkg != null) {
                        selectedLauncher = LauncherRuntimeState(
                            spec = LauncherSpec(
                                id = "generic_default",
                                name = defaultLauncherName,
                                developer = "Device System",
                                packageNames = listOf(defaultLauncherPkg),
                                playStoreUrl = null,
                                applyMethod = ApplyMethod.ALTERNATIVE,
                                tagline = "Unlisted default launcher; ZINK integration is not verified",
                                supportedVersions = "Current device firmware",
                                requirements = listOf("Default launcher active on device"),
                                settingsPath = listOf("Install a compatible third-party launcher", "Set it as the default HOME app", "Select ZINK in its icon settings"),
                                setupSteps = listOf(
                                    "$defaultLauncherName is detected as the selected HOME app, but ZINK has no verified icon-pack integration for it.",
                                    "Install a launcher with ZINK icon-pack support, such as Nova, Niagara, Smart Launcher, or Lawnchair.",
                                    "Set that launcher as the default HOME app in Android Settings.",
                                    "Open its icon settings and select ZINK."
                                ),
                                limitations = listOf(
                                    "This launcher is not in ZINK's curated launcher registry, so compatibility is unknown.",
                                    "ZINK cannot confirm that it accepts standard Android icon-pack metadata."
                                ),
                                troubleshooting = listOf(
                                    "If this launcher does not list ZINK as an icon pack, switch to a launcher with a verified ZINK integration."
                                ),
                                unavailableMessage = "No verified ZINK icon-pack integration for this launcher. Use a compatible third-party launcher.",
                                verification = VerificationSource(
                                    url = "https://source.android.com/",
                                    sourceType = "System Package Manager Resolution",
                                    dateChecked = "2026-11-21",
                                    evidence = "Detected via Android CATEGORY_HOME package resolution at runtime.",
                                    confidence = ConfidenceLevel.UNVERIFIED
                                )
                            ),
                            isInstalled = true,
                            isDefault = true,
                            installedPackage = defaultLauncherPkg
                        )
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = navBarBottom + 104.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            // ── Search & Filter Controls ──
            LauncherSearchBar(
                query = searchQuery,
                onQueryChange = { ApplyStateManager.setSearchQuery(context, it) },
                onClear = { ApplyStateManager.setSearchQuery(context, "") }
            )

            Spacer(Modifier.height(12.dp))

            // Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FilterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { ApplyStateManager.setSelectedFilter(context, filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Silver else Muted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Section Title & Granular Confidence Summary ──
            val confidenceBreakdown = remember(launcherStates) {
                LauncherRegistry.getConfidenceBreakdown(launcherStates)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUPPORTED LAUNCHERS",
                        color = Muted,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "${filteredLaunchers.size} TOTAL",
                            color = Muted,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )

                        var showSortMenu by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { showSortMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Sort: ${ApplyStateManager.sortOption.label}",
                                        color = Silver,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Sort",
                                        tint = Silver,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(colors.cardBackground)
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                color = if (ApplyStateManager.sortOption == option) Color.White else Silver,
                                                fontSize = 12.sp
                                            )
                                        },
                                        onClick = {
                                            ApplyStateManager.setSortOption(context, option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (launcherStates.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    LauncherConfidenceBadgesRow(
                        confidenceBreakdown = confidenceBreakdown,
                        selectedConfidence = ApplyStateManager.selectedConfidenceLevel,
                        onToggleConfidence = { level ->
                            ApplyStateManager.toggleConfidenceLevel(context, level)
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Launcher Cards ──
            if (filteredLaunchers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "No matching launchers found",
                            color = Silver,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Try clearing your search query or filter",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                filteredLaunchers.forEach { state ->
                    LauncherDirectoryCard(
                        state = state,
                        onDirectApply = {
                            LauncherRegistry.executeDirectApply(context, state)
                        },
                        onOpenGuide = {
                            selectedLauncher = state
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            }
        }

        // ── Comprehensive Launcher Detail & Setup Guide Bottom Sheet ──
        if (selectedLauncher != null) {
            LauncherDetailSheet(
                state = selectedLauncher!!,
                onDismiss = { selectedLauncher = null },
                onDirectApply = {
                    LauncherRegistry.executeDirectApply(context, selectedLauncher!!)
                }
            )
        }
    }
}

// ─── Launcher Confidence Badges Row ───

@Composable
private fun LauncherConfidenceBadgesRow(
    confidenceBreakdown: LauncherRegistry.ConfidenceBreakdown,
    selectedConfidence: ConfidenceLevel?,
    onToggleConfidence: (ConfidenceLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val officialCount = confidenceBreakdown.counts.entries
            .filter { it.key == ConfidenceLevel.VERIFIED_OFFICIAL_DOCS || it.key == ConfidenceLevel.VERIFIED_SOURCE_CODE || it.key == ConfidenceLevel.VERIFIED_INTENT_SPEC }
            .sumOf { it.value }
        val communityCount = confidenceBreakdown.counts[ConfidenceLevel.COMMUNITY_VERIFIED] ?: 0
        val unverifiedCount = confidenceBreakdown.counts[ConfidenceLevel.UNVERIFIED] ?: 0

        val categories = listOf(
            Triple(ConfidenceLevel.VERIFIED_OFFICIAL_DOCS, officialCount, Color(0xFF6FBF8F)),
            Triple(ConfidenceLevel.COMMUNITY_VERIFIED, communityCount, Color(0xFFC4A872)),
            Triple(ConfidenceLevel.UNVERIFIED, unverifiedCount, Muted)
        )

        categories.filter { it.second > 0 }.forEach { (level, count, badgeColor) ->
            val isSelected = when (level) {
                ConfidenceLevel.VERIFIED_OFFICIAL_DOCS -> {
                    selectedConfidence == ConfidenceLevel.VERIFIED_OFFICIAL_DOCS ||
                            selectedConfidence == ConfidenceLevel.VERIFIED_SOURCE_CODE ||
                            selectedConfidence == ConfidenceLevel.VERIFIED_INTENT_SPEC
                }
                else -> selectedConfidence == level
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) badgeColor else badgeColor.copy(alpha = 0.10f))
                    .border(1.dp, badgeColor.copy(alpha = if (isSelected) 0.8f else 0.22f), RoundedCornerShape(6.dp))
                    .clickable { onToggleConfidence(level) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$count ${level.shortLabel.uppercase()}",
                    color = if (isSelected) Color.Black else badgeColor,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── Default Launcher Hero Card ───

@Composable
private fun DefaultLauncherHeroCard(
    defaultName: String,
    state: LauncherRuntimeState?,
    launcherId: String?,
    packageName: String?,
    onApply: () -> Unit,
    onViewGuide: () -> Unit
) {
    val methodText = when (state?.spec?.applyMethod) {
        ApplyMethod.DIRECT -> "Direct Apply Documented"
        ApplyMethod.MANUAL -> "Manual Setup Required"
        ApplyMethod.ALTERNATIVE -> "Alternative Required"
        ApplyMethod.UNVERIFIED -> "Compatibility Unverified"
        null -> "No verified icon-pack integration · Tap for alternatives"
    }

    val badgeColor = when (state?.spec?.applyMethod) {
        ApplyMethod.DIRECT -> Color(0xFF6FBF8F)
        ApplyMethod.MANUAL -> Color(0xFF8FA9C4)
        ApplyMethod.ALTERNATIVE -> Color(0xFFC4A872)
        ApplyMethod.UNVERIFIED -> Muted
        null -> Silver
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LocalZinkColors.current.cardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 48dp Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                LauncherAppIcon(
                    launcherId = launcherId,
                    packageName = packageName,
                    contentDescription = defaultName,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CURRENT DEFAULT LAUNCHER",
                    color = Muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = defaultName,
                        color = Silver,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEFAULT",
                            color = Silver,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = methodText,
                    color = badgeColor,
                    fontSize = 11.sp
                )
                if (packageName != null) {
                    Text(
                        text = packageName,
                        color = Muted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state?.spec?.applyMethod == ApplyMethod.DIRECT && state.isInstalled) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .pressScale(onClick = onApply),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FlashOn,
                            contentDescription = null,
                            tint = Silver,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "1-TAP APPLY",
                            color = Silver,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                    .pressScale(onClick = onViewGuide),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VIEW SETUP GUIDE",
                    color = Silver,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

// ─── Search Bar ───

@Composable
private fun LauncherSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search",
            tint = Muted,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search launcher name, developer, or package...",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Silver,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(Silver),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = Muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Launcher Directory Card ───

@Composable
private fun LauncherDirectoryCard(
    state: LauncherRuntimeState,
    onDirectApply: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val methodColor = when (state.spec.applyMethod) {
        ApplyMethod.DIRECT -> Color(0xFF6FBF8F)
        ApplyMethod.MANUAL -> Color(0xFF8FA9C4)
        ApplyMethod.ALTERNATIVE -> Color(0xFFC4A872)
        ApplyMethod.UNVERIFIED -> Muted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LocalZinkColors.current.cardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .glowingBorderOnPress(RoundedCornerShape(18.dp))
            .pressScale(onClick = onOpenGuide)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Launcher 44dp Icon Container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            LauncherAppIcon(
                launcherId = state.spec.id,
                packageName = state.installedPackage,
                contentDescription = state.spec.name,
                modifier = Modifier.fillMaxSize().padding(3.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.spec.name,
                    color = Silver,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = state.spec.developer,
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(methodColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = state.methodBadge,
                    color = methodColor,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${state.confidenceLabel} · ${state.availabilityLabel}",
                color = Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Direct Apply shortcut button (if supported and installed)
        if (state.spec.applyMethod == ApplyMethod.DIRECT && state.isInstalled) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDirectApply
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FlashOn,
                        contentDescription = null,
                        tint = Silver,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "APPLY",
                        color = Silver,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = "Details",
            tint = Muted,
            modifier = Modifier.size(13.dp)
        )
    }
}

// ─── Option 3: Vertical Step List Component ───

@Composable
private fun VerticalStepList(
    steps: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        steps.forEachIndexed { index, step ->
            val isLast = index == steps.lastIndex

            val stepIcon = when {
                isLast -> Icons.Outlined.Layers
                step.contains("Home", ignoreCase = true) || step.contains("Search", ignoreCase = true) -> Icons.Outlined.TouchApp
                step.contains("Setting", ignoreCase = true) || step.contains("Preference", ignoreCase = true) -> Icons.Outlined.Settings
                step.contains("Appearance", ignoreCase = true) || step.contains("Customisation", ignoreCase = true) || step.contains("Personalisation", ignoreCase = true) || step.contains("Look", ignoreCase = true) || step.contains("Theme", ignoreCase = true) -> Icons.Outlined.Tune
                step.contains("Store", ignoreCase = true) || step.contains("Install", ignoreCase = true) -> Icons.AutoMirrored.Outlined.OpenInNew
                else -> Icons.Outlined.Apps
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Circle indicator and vertical connecting line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(32.dp)
                ) {
                    // Numbered Circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLast) Color.White.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isLast) Color.White.copy(alpha = 0.50f)
                                else Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (isLast) Color.White else Silver,
                            fontSize = 12.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    // Vertical connecting line
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Right Step Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (!isLast) 10.dp else 0.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isLast) Color.White.copy(alpha = 0.10f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isLast) Color.White.copy(alpha = 0.32f)
                            else Color.White.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = stepIcon,
                            contentDescription = null,
                            tint = if (isLast) Color.White else Silver.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = step,
                            color = if (isLast) Color.White else Silver,
                            fontSize = 13.sp,
                            fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Intent / Action Code Container with Copy Action ───

@Composable
private fun IntentCodeContainer(
    intentDetails: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C0C0E))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INTENT SPECIFICATION",
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Copy Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .pressScale(onClick = onCopy)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = Silver,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "COPY",
                        color = Silver,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Horizontally scrollable code box so long action strings never stretch or break layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = intentDetails,
                color = Silver.copy(alpha = 0.90f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// ─── Standardized Launcher Detail Sheet ───

@Composable
private fun LauncherDetailSheet(
    state: LauncherRuntimeState,
    onDismiss: () -> Unit,
    onDirectApply: () -> Unit
) {
    val context = LocalContext.current
    val spec = state.spec
    var launcherOpenNotice by remember(spec.id) { mutableStateOf<String?>(null) }
    val formattedIntent = remember(spec) {
        LauncherRegistry.formatIntentDetails(context, spec)
    }

    val sheetOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .offset { IntOffset(0, max(0f, sheetOffset.value).roundToInt()) }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(LocalZinkColors.current.cardBackground)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
            ) {
                // Drag handle & Top Close Button Row
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount > 0) {
                                        change.consume()
                                        coroutineScope.launch {
                                            sheetOffset.snapTo((sheetOffset.value + dragAmount).coerceAtLeast(0f))
                                        }
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (sheetOffset.value > 80f) {
                                            onDismiss()
                                        } else {
                                            sheetOffset.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        sheetOffset.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                                    }
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .align(Alignment.Center)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .pressScale(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Silver,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── 1. Launcher Name & Identity Header ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        LauncherAppIcon(
                            launcherId = spec.id,
                            packageName = state.installedPackage,
                            contentDescription = spec.name,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            text = spec.name,
                            color = Silver,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${spec.developer} · ${spec.supportedVersions}",
                            color = Muted,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val methodColor = when (spec.applyMethod) {
                                ApplyMethod.DIRECT -> Color(0xFF6FBF8F)
                                ApplyMethod.MANUAL -> Color(0xFF8FA9C4)
                                ApplyMethod.ALTERNATIVE -> Color(0xFFC4A872)
                                ApplyMethod.UNVERIFIED -> Muted
                            }
                            Text(
                                text = state.methodBadge,
                                color = methodColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = " · ${state.statusBadge}",
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // ── 2. Verified Application Method ──
                DetailSectionHeader("VERIFIED APPLICATION METHOD")
                Spacer(Modifier.height(8.dp))
                InfoBox {
                    val methodTitle = when (spec.applyMethod) {
                        ApplyMethod.DIRECT -> "Direct Apply · Officially Documented"
                        ApplyMethod.MANUAL -> "Manual Setup Required"
                        ApplyMethod.ALTERNATIVE -> "Alternative Integration Required"
                        ApplyMethod.UNVERIFIED -> "Compatibility Unverified"
                    }
                    val methodDescription = when (spec.applyMethod) {
                        ApplyMethod.DIRECT -> "ZINK sends this launcher's configured apply request. Follow any launcher confirmation shown; sending the request does not confirm successful application."
                        ApplyMethod.MANUAL -> "Follow the launcher-specific settings steps below to select ZINK manually. The OPEN action can launch the app when installed."
                        ApplyMethod.ALTERNATIVE -> "ZINK has no verified standard icon-pack integration for this system launcher. Follow the alternative guidance below."
                        ApplyMethod.UNVERIFIED -> "Available evidence does not establish a working ZINK icon-pack method for this launcher. Do not treat installation or detection as compatibility."
                    }
                    val methodColor = when (spec.applyMethod) {
                        ApplyMethod.DIRECT -> Color(0xFF6FBF8F)
                        ApplyMethod.MANUAL -> Color(0xFF8FA9C4)
                        ApplyMethod.ALTERNATIVE -> Color(0xFFC4A872)
                        ApplyMethod.UNVERIFIED -> Muted
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(methodColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = methodTitle,
                            color = Silver,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = methodDescription,
                        color = Silver.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(Modifier.height(18.dp))

                // ── 3. Verified Intent / Action ──
                DetailSectionHeader("VERIFIED INTENT / ACTION")
                Spacer(Modifier.height(8.dp))
                IntentCodeContainer(
                    intentDetails = formattedIntent,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Intent Action", formattedIntent)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Intent details copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(Modifier.height(18.dp))

                // ── 4. Settings Navigation Path (Option 3: Vertical Step List) ──
                DetailSectionHeader("SETTINGS NAVIGATION PATH")
                Spacer(Modifier.height(8.dp))
                if (spec.id == "nothing") {
                    Text(
                        text = "Nothing OS 5.0 and later",
                        color = Silver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    VerticalStepList(
                        steps = listOf(
                            "Home Screen (Long-press empty area)",
                            "Customisation",
                            "Theme & Style",
                            "Icon Pack",
                            "Select ZINK"
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Nothing OS 2.5.0–4.x",
                        color = Silver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    VerticalStepList(
                        steps = listOf(
                            "Home Screen (Long-press empty area)",
                            "Customisation (or Personalisation)",
                            "Icon Pack",
                            "Select ZINK"
                        )
                    )
                } else {
                    VerticalStepList(steps = spec.settingsPath)
                }

                Spacer(Modifier.height(18.dp))

                // ── 5. Verification Source & Confidence Level ──
                DetailSectionHeader("VERIFICATION SOURCE & CONFIDENCE")
                Spacer(Modifier.height(8.dp))
                InfoBox {
                    val confidenceColor = when (spec.verification.confidence) {
                        ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                        ConfidenceLevel.VERIFIED_SOURCE_CODE -> Color(0xFF8FAF8F)
                        ConfidenceLevel.VERIFIED_INTENT_SPEC,
                        ConfidenceLevel.COMMUNITY_VERIFIED -> Color(0xFFC4A872)
                        ConfidenceLevel.UNVERIFIED -> Muted
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = confidenceColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${spec.verification.status.label} · ${spec.verification.confidence.label}",
                            color = Silver,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Integration status: ${spec.verification.status.label}",
                        color = Muted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Play availability: ${spec.listingAvailability.label}",
                        color = Muted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = spec.listingAvailability.explanation,
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Source Type: ${spec.verification.sourceType}",
                        color = Muted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Last Verified: ${spec.verification.dateChecked}",
                        color = Muted,
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = spec.verification.evidence,
                        color = Silver.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    if (spec.verification.url != null && spec.verification.linkLabel != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(spec.verification.url)))
                                    } catch (_: Exception) { }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = Silver,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = spec.verification.linkLabel,
                            color = Silver,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        }
                    }
                }

                // ── 6. Known Limitations ──
                if (spec.limitations.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    DetailSectionHeader("KNOWN LIMITATIONS")
                    Spacer(Modifier.height(8.dp))
                    InfoBox {
                        spec.limitations.forEach { limitation ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text("•", color = Muted, fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = limitation,
                                    color = Muted,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // ── 7. Troubleshooting & Tips ──
                if (spec.troubleshooting.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    DetailSectionHeader("TROUBLESHOOTING & TIPS")
                    Spacer(Modifier.height(8.dp))
                    InfoBox {
                        spec.troubleshooting.forEach { tip ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text("•", color = Color(0xFFAAAA77), fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tip,
                                    color = Silver.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // ── 8. Actions ──
                Spacer(Modifier.height(24.dp))
                DetailSectionHeader("ACTIONS")
                Spacer(Modifier.height(10.dp))

                val actionPresentation = state.actionPresentation

                // Direct Apply Button (if installed and direct apply supported)
                if (spec.applyMethod == ApplyMethod.DIRECT && state.isInstalled) {
                    ActionSheetButton(
                        text = "1-TAP APPLY TO ${spec.name.uppercase()}",
                        isPrimary = true,
                        icon = Icons.Outlined.FlashOn,
                        onClick = onDirectApply
                    )
                    Spacer(Modifier.height(8.dp))
                }

                actionPresentation.deviceRestrictionWarning?.let { warning ->
                    ActionNotice(text = warning, isWarning = true)
                    Spacer(Modifier.height(8.dp))
                }

                actionPresentation.openActionLabel?.let { label ->
                    ActionSheetButton(
                        text = label,
                        icon = Icons.Outlined.Settings,
                        onClick = {
                            val pkg = state.installedPackage
                            val result = if (pkg == null) {
                                LauncherOpenResult(
                                    opened = false,
                                    message = "${spec.name} is not available as an installed launcher on this device."
                                )
                            } else {
                                openLauncher(context, pkg, spec.name)
                            }
                            launcherOpenNotice = result.message
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                launcherOpenNotice?.let { message ->
                    ActionNotice(text = message, isWarning = true)
                    Spacer(Modifier.height(8.dp))
                    ActionSheetButton(
                        text = "OPEN DEFAULT APP SETTINGS",
                        icon = Icons.Outlined.Settings,
                        onClick = {
                            if (!openDefaultAppsSettings(context)) {
                                launcherOpenNotice =
                                    "$message Open Android Settings → Apps → Default apps → Home app manually."
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                val sourceUrl = actionPresentation.sourceUrl
                val sourceActionLabel = actionPresentation.sourceActionLabel
                if (sourceUrl != null && sourceActionLabel != null) {
                    ActionSheetButton(
                        text = sourceActionLabel,
                        isPrimary = !state.isInstalled,
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = {
                            if (actionPresentation.sourceIsPlayStore) {
                                openGooglePlayListing(context, sourceUrl)
                            } else {
                                openExternalSource(context, sourceUrl)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                actionPresentation.availabilityMessage?.let { message ->
                    ActionNotice(text = message)
                    Spacer(Modifier.height(8.dp))
                }

                // Close Button
                ActionSheetButton(
                    text = "CLOSE",
                    icon = Icons.Outlined.Close,
                    onClick = onDismiss
                )
            }
        }
    }
}

// ─── Shared UI Helpers ───

@Composable
private fun DetailSectionHeader(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 10.sp,
        letterSpacing = 1.8.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
private fun InfoBox(content: @Composable ColumnScope.() -> Unit) {
    val cardBg = LocalZinkColors.current.cardBackground
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun ActionNotice(text: String, isWarning: Boolean = false) {
    val cardBg = LocalZinkColors.current.cardBackground
    val background = if (isWarning) Color(0xFF302718) else cardBg
    val foreground = if (isWarning) Color(0xFFFFD27A) else Color(0xFFB8B8BE)
    val outline = if (isWarning) Color(0xFFFFD27A).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, outline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(text = text, color = foreground, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ActionSheetButton(
    text: String,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPrimary) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (isPrimary) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(12.dp)
            )
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Silver,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Silver,
                fontSize = 11.sp,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 1.2.sp
            )
        }
    }
}
