package com.gesuling.zink.ui.apply

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gesuling.zink.ui.anim.glowingBorderOnPress
import com.gesuling.zink.ui.anim.pressScale
import androidx.core.graphics.drawable.toBitmap
import com.gesuling.zink.ui.theme.LocalZinkColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Silver = Color(0xFFF2F2F4)
private val Muted = Color(0xFF7C7C82)
private val CardBg = Color(0xFF121212)
private val CardBorder = Color.White.copy(alpha = 0.08f)

private val FilterOptions = listOf("All", "Direct Apply", "Manual Setup", "Installed", "System & Others")

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
            val installedIcon = packageName?.let { installedPackage ->
                runCatching {
                    context.packageManager.getApplicationIcon(installedPackage)
                        .toBitmap(width = 192, height = 192)
                }.getOrNull()
            }
            installedIcon ?: launcherId?.let { id ->
                runCatching {
                    val encoded = context.assets.open("launcher_icon_${id}.png.b64")
                        .bufferedReader()
                        .use { it.readText() }
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            }
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

@Composable
fun ApplyScreen() {
    val context = LocalContext.current
    val colors = LocalZinkColors.current

    val launcherStates = remember { LauncherRegistry.getLauncherStates(context) }
    var selectedLauncher by remember { mutableStateOf<LauncherRuntimeState?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val defaultLauncherState = remember(launcherStates) {
        launcherStates.firstOrNull { it.isDefault }
    }

    val defaultLauncherPkg = remember { LauncherRegistry.detectDefaultLauncherPackage(context) }
    val defaultLauncherName = remember(defaultLauncherState, defaultLauncherPkg) {
        defaultLauncherState?.spec?.name
            ?: defaultLauncherPkg?.let { pkg ->
                try {
                    val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkg
                }
            }
            ?: "Unknown Launcher"
    }

    // Filter and search logic
    val filteredLaunchers = remember(launcherStates, searchQuery, selectedFilter) {
        launcherStates.filter { state ->
            val matchesSearch = searchQuery.isBlank() ||
                    state.spec.name.contains(searchQuery, ignoreCase = true) ||
                    state.spec.developer.contains(searchQuery, ignoreCase = true) ||
                    state.spec.tagline.contains(searchQuery, ignoreCase = true) ||
                    state.spec.packageNames.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                "Direct Apply" -> state.spec.applyMethod == ApplyMethod.DIRECT
                "Manual Setup" -> state.spec.applyMethod == ApplyMethod.MANUAL
                "Installed" -> state.isInstalled
                "System & Others" -> state.spec.applyMethod == ApplyMethod.ALTERNATIVE
                else -> true
            }

            matchesSearch && matchesFilter
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
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Top Header ──
            Text(
                text = "APPLY",
                color = Silver,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "LAUNCHER COMPATIBILITY & SETUP HUB",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))

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
                                applyMethod = ApplyMethod.MANUAL,
                                tagline = "Unlisted launcher detected as system default",
                                supportedVersions = "Current device firmware",
                                requirements = listOf("Default launcher active on device"),
                                settingsPath = listOf("Home Screen (Long-press)", "Settings / Customisation", "Icon pack", "Select ZINK"),
                                setupSteps = listOf(
                                    "Open $defaultLauncherName settings or long-press an empty home screen space.",
                                    "Locate the Home Screen, Appearance, or Customization section.",
                                    "Look for an Icon Pack or Icon Theme setting.",
                                    "If third-party icon packs are supported, select ZINK from the list."
                                ),
                                limitations = listOf(
                                    "This launcher is not pre-cataloged in ZINK's verified compatibility repository.",
                                    "System launchers (such as stock Pixel or ColorOS) may only allow custom icons via dedicated apps or shortcut tools."
                                ),
                                troubleshooting = listOf(
                                    "If third-party icon packs are not supported, consider switching to Nova Launcher, Niagara, or Lawnchair."
                                ),
                                verification = VerificationSource(
                                    url = "https://source.android.com/",
                                    sourceType = "System Package Manager Resolution",
                                    dateChecked = "2026-11-21",
                                    evidence = "Detected via Android CATEGORY_HOME package resolution at runtime.",
                                    confidence = ConfidenceLevel.COMMUNITY_VERIFIED
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
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
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
                            .clickable { selectedFilter = filter }
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

            // ── Section Title & Count ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
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
                Text(
                    text = "${filteredLaunchers.size} VERIFIED",
                    color = Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
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
        ApplyMethod.DIRECT -> "Direct 1-Tap Apply Supported"
        ApplyMethod.MANUAL -> "Supported via Launcher Customisation"
        ApplyMethod.ALTERNATIVE -> "Limited · Companion App or Alternative Required"
        null -> "Detected Default · Tap Guide to Check Compatibility"
    }

    val badgeColor = when (state?.spec?.applyMethod) {
        ApplyMethod.DIRECT -> Color(0xFF6FBF8F)
        ApplyMethod.MANUAL -> Color(0xFF8FA9C4)
        ApplyMethod.ALTERNATIVE -> Color(0xFFC4A872)
        null -> Silver
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CURRENT DEFAULT LAUNCHER",
                        color = Muted,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = defaultName,
                    color = Silver,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = methodText,
                    color = badgeColor,
                    fontSize = 11.sp
                )
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
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
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
                    fontWeight = FontWeight.Medium
                )

                if (state.isDefault) {
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
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = state.spec.developer,
                color = Muted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Method indicator
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
                    fontSize = 11.sp
                )
                Text(
                    text = " · ${if (state.isInstalled) "Installed" else "Available"}",
                    color = Muted,
                    fontSize = 11.sp
                )
            }
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

                        Column(modifier = Modifier.weight(1f)) {
                            if (isLast) {
                                Text(
                                    text = "DESTINATION",
                                    color = Silver,
                                    fontSize = 8.sp,
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(1.dp))
                            }
                            Text(
                                text = step,
                                color = if (isLast) Color.White else Silver,
                                fontSize = 13.sp,
                                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                lineHeight = 18.sp
                            )
                        }
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
    val formattedIntent = remember(spec) {
        LauncherRegistry.formatIntentDetails(context, spec)
    }

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
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF0E0E0E))
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
                Box(modifier = Modifier.fillMaxWidth()) {
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
                        ApplyMethod.DIRECT -> "1-Tap Direct Apply (Confirmed Intent)"
                        ApplyMethod.MANUAL -> "Manual Setup via Launcher Settings"
                        ApplyMethod.ALTERNATIVE -> "Alternative Integration Required"
                    }
                    val methodDescription = when (spec.applyMethod) {
                        ApplyMethod.DIRECT -> "ZINK can invoke this launcher's verified application intent directly to apply the full monochrome silver icon pack with zero manual steps."
                        ApplyMethod.MANUAL -> "This launcher contains an in-app icon pack selector in its settings. Follow the verified navigation path below to select ZINK."
                        ApplyMethod.ALTERNATIVE -> "This system launcher does not support direct third-party icon packs. Use the official companion method (e.g. Samsung Theme Park) or a compatible third-party launcher."
                    }
                    val methodColor = when (spec.applyMethod) {
                        ApplyMethod.DIRECT -> Color(0xFF6FBF8F)
                        ApplyMethod.MANUAL -> Color(0xFF8FA9C4)
                        ApplyMethod.ALTERNATIVE -> Color(0xFFC4A872)
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
                VerticalStepList(steps = spec.settingsPath)

                Spacer(Modifier.height(18.dp))

                // ── 5. Verification Source & Confidence Level ──
                DetailSectionHeader("VERIFICATION SOURCE & CONFIDENCE")
                Spacer(Modifier.height(8.dp))
                InfoBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = Color(0xFF8FAF8F),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = spec.verification.confidence.label,
                            color = Silver,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(6.dp))

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

                    // Clickable link to official documentation
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spec.verification.url))
                                    context.startActivity(intent)
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
                            text = "View Official Documentation Source",
                            color = Silver,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                            val pkg = state.installedPackage ?: spec.packageNames.firstOrNull()
                            if (pkg != null) {
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                    if (intent != null) context.startActivity(intent)
                                } catch (_: Exception) { }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun ActionNotice(text: String, isWarning: Boolean = false) {
    val background = if (isWarning) Color(0xFF302718) else Color(0xFF161616)
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
