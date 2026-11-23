package com.gesuling.zink.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gesuling.zink.ui.anim.pressScale
import com.gesuling.zink.ui.apply.LauncherRegistry
import com.gesuling.zink.ui.components.ZinkToolbarHeader
import com.gesuling.zink.ui.theme.LocalZinkColors
import com.gesuling.zink.ui.theme.TitleFontOption
import com.gesuling.zink.ui.wallpapers.WallpaperBitmapCache
import com.gesuling.zink.ui.wallpapers.WallpaperDownloadQuality
import com.gesuling.zink.ui.wallpapers.WallpaperQualityPreferences

private val Silver = Color(0xFFE0E0E0)
private val Muted = Color(0xFF888888)

object ZinkCacheManager {
    fun clearCaches(context: Context): CacheClearResult {
        val ramBytesCleared = WallpaperBitmapCache.clear()

        var diskBytesCleared = 0L
        val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
        for (dir in cacheDirs) {
            if (dir.exists() && dir.isDirectory) {
                diskBytesCleared += deleteRecursive(dir)
            }
        }

        return CacheClearResult(ramBytesCleared, diskBytesCleared)
    }

    private fun deleteRecursive(file: java.io.File): Long {
        var cleared = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                cleared += deleteRecursive(child)
            }
        } else {
            val len = file.length()
            if (file.delete()) {
                cleared += len
            }
        }
        return cleared
    }
}

data class CacheClearResult(val ramBytes: Long, val diskBytes: Long) {
    val totalBytes: Long get() = ramBytes + diskBytes
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(java.util.Locale.US, "%.1f MB", mb)
}

private fun currentDefaultLauncherSummary(context: Context): String {
    val packageName = LauncherRegistry.detectDefaultLauncherPackage(context)
        ?: return "Apply ZINK icons to your default launcher."
    val label = LauncherRegistry.getApplicationLabel(context, packageName)
    return "Apply ZINK icons to $label."
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(
    headerArtwork: String,
    onHeaderArtworkChange: (String) -> Unit,
    isPureAmoled: Boolean,
    onPureAmoledChange: (Boolean) -> Unit,
    titleFontOption: TitleFontOption = TitleFontOption.DEFAULT,
    onTitleFontChange: (TitleFontOption) -> Unit = {},
    onBackClick: () -> Unit,
    onApplyToLauncherClick: () -> Unit,
    launcherRefreshVersion: Int = 0,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isSettingsOpen: Boolean = true
) {
    val context = LocalContext.current
    val colors = LocalZinkColors.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var currentLauncherSummary by remember(context) {
        mutableStateOf(currentDefaultLauncherSummary(context))
    }
    LaunchedEffect(launcherRefreshVersion) {
        currentLauncherSummary = currentDefaultLauncherSummary(context)
    }

    var wallpaperQuality by remember {
        mutableStateOf(WallpaperQualityPreferences.get(context))
    }

    var showChangelog by remember { mutableStateOf(false) }
    val versionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1.3"
        } catch (_: Exception) {
            "0.1.3"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ZinkToolbarHeader(
                title = "SETTINGS",
                subtitle = "PREFERENCES & PACK SYSTEM",
                onBackClick = onBackClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                isSettingsOpen = isSettingsOpen
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = navBarBottom + 104.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // 1. APPEARANCE Group
                SettingsSectionHeader("APPEARANCE")
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                ) {
                    // Row 1: Pure AMOLED Black
                    SettingsSwitchRow(
                        title = "Pure AMOLED Black",
                        subtitle = "Enable pure black backgrounds.",
                        checked = isPureAmoled,
                        onCheckedChange = onPureAmoledChange
                    )

                    SettingsDivider()

                    // Row 2: Unthemed Icon Scale
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unthemed Icon Scale", color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Adjust icon size in unsupported launchers.",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("0.74×", color = Silver, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    SettingsDivider()

                    // Row 3: Home Header Artwork
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text("Home Header Artwork", color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Frieren", "Obsidian", "Nocturne").forEach { style ->
                                val isSelected = headerArtwork == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color(0xFF282830) else Color(0x11FFFFFF))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onHeaderArtworkChange(style) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style,
                                        color = if (isSelected) Color.White else Muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // Row 4: Title Font Style
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text("Title Font Style", color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TitleFontOption.entries.forEach { option ->
                                val isSelected = titleFontOption == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color(0xFF282830) else Color(0x11FFFFFF))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onTitleFontChange(option) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) Color.White else Muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 2. LAUNCHER & ICONS Group
                SettingsSectionHeader("LAUNCHER & ICONS")
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                ) {
                    // Row 1: Apply to Launcher
                    SettingsClickableRow(
                        title = "Apply to Launcher",
                        subtitle = currentLauncherSummary,
                        trailing = {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Muted)
                        },
                        onClick = {
                            currentLauncherSummary = currentDefaultLauncherSummary(context)
                            onApplyToLauncherClick()
                        }
                    )

                    SettingsDivider()

                    // Row 2: Wallpaper Download Quality
                    SettingsClickableRow(
                        title = "Wallpaper Download Quality",
                        subtitle = "Target resolution for downloaded wallpapers",
                        trailing = {
                            Text(wallpaperQuality.label, color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        },
                        onClick = {
                            wallpaperQuality = if (wallpaperQuality == WallpaperDownloadQuality.ORIGINAL) {
                                WallpaperDownloadQuality.FHD_1080P
                            } else {
                                WallpaperDownloadQuality.ORIGINAL
                            }
                            WallpaperQualityPreferences.set(context, wallpaperQuality)
                            Toast.makeText(context, "Wallpaper quality: ${wallpaperQuality.label}", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsDivider()

                    // Row 3: Clear Icon Cache
                    SettingsClickableRow(
                        title = "Clear Icon Cache",
                        subtitle = "Remove cached icons to free up storage.",
                        trailing = {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear Cache",
                                tint = Silver,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            val result = ZinkCacheManager.clearCaches(context)
                            val ramText = formatBytes(result.ramBytes)
                            val diskText = formatBytes(result.diskBytes)
                            if (result.totalBytes > 0) {
                                Toast.makeText(
                                    context,
                                    "Cleared $ramText RAM cache and $diskText disk cache.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Cache is already empty (0 KB cleared).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 3. ABOUT Group
                SettingsSectionHeader("ABOUT")
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                ) {
                    // Row 1: App Version (Informational)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Version", color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text("Current installed application version", color = Muted, fontSize = 12.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("v$versionName", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    SettingsDivider()

                    // Row 2: Changelogs
                    SettingsClickableRow(
                        title = "Changelogs",
                        subtitle = "Explore version history and updates.",
                        trailing = {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Changelogs",
                                tint = Muted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            showChangelog = true
                        }
                    )

                    SettingsDivider()

                    // Row 3: GitHub Repository
                    SettingsClickableRow(
                        title = "GitHub Repository",
                        subtitle = "View source code & releases",
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "GitHub Repository",
                                tint = Silver,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Gesuling/Zink")
                                )
                            )
                        }
                    )
                }
            }
        }

        if (showChangelog) {
            ChangelogBottomSheet(onDismiss = { showChangelog = false })
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    val titleFont = com.gesuling.zink.ui.theme.LocalTitleFont.current.fontFamily
    Text(
        text = text,
        color = Muted,
        fontFamily = titleFont,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.08f),
        thickness = 1.dp
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Silver,
                checkedTrackColor = Color(0xFF282830),
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Color(0xFF141418)
            )
        )
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.width(12.dp))

        trailing()
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        headerArtwork = "Frieren",
        onHeaderArtworkChange = {},
        isPureAmoled = false,
        onPureAmoledChange = {},
        onBackClick = {},
        onApplyToLauncherClick = {}
    )
}
