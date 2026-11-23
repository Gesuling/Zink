package com.gesuling.zink.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gesuling.zink.ui.anim.pressScale
import com.gesuling.zink.ui.theme.LocalZinkColors

private val Silver = Color(0xFFE0E0E0)
private val Muted = Color(0xFF888888)

// Structured metadata: update LATEST_PUBLISHED_VERSION upon official publication of v0.1.4
const val LATEST_PUBLISHED_VERSION = "0.1.4"

fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(parts1.size, parts2.size)) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) return p1.compareTo(p2)
    }
    return 0
}

fun isReleasePublished(version: String): Boolean {
    return compareVersions(version, LATEST_PUBLISHED_VERSION) <= 0
}

data class ReleaseInfo(
    val version: String,
    val date: String,
    val isLatest: Boolean,
    val summary: String,
    val sections: List<ReleaseSection>
)

data class ReleaseSection(
    val title: String,
    val bullets: List<String>
)

val zinkReleaseHistory = listOf(
    ReleaseInfo(
        version = "v0.1.4",
        date = "23 November 2026",
        isLatest = true,
        summary = "What's New v0.1.4: Automatic updates, NDot 55/57 fonts, AMOLED/Gray modes, Xiaomi System Launcher, and Settings UI refinements.",
        sections = listOf(
            ReleaseSection(
                title = "Added",
                bullets = listOf(
                    "Automatic app updates and notifications.",
                    "NDOT 55 and NDOT 57 font options.",
                    "AMOLED Black and Gray Mode.",
                    "Xiaomi HyperOS System Launcher support.",
                    "Expanded launcher support to 15 launchers."
                )
            ),
            ReleaseSection(
                title = "Improved",
                bullets = listOf(
                    "Refined Home, Requests, and Settings UI.",
                    "Improved typography and layout consistency.",
                    "Updated launcher compatibility and verification details.",
                    "Refined launcher-specific intent instructions.",
                    "Improved icon cache management."
                )
            ),
            ReleaseSection(
                title = "Fixed",
                bullets = listOf(
                    "Status bar icon contrast and appearance.",
                    "Xiaomi launcher icon loading.",
                    "Incorrect Samsung and Pixel intent instructions appearing under other launchers.",
                    "Obsolete update testing elements."
                )
            )
        )
    ),
    ReleaseInfo(
        version = "v0.1.3",
        date = "23 November 2026",
        isLatest = false,
        summary = "Apply tab UI enhancements, robust system launcher icon resolution, interactive confidence status filters, and sort options.",
        sections = listOf(
            ReleaseSection(
                title = "Apply Tab Enhancements",
                bullets = listOf(
                    "Robust package and activity icon resolution in LauncherAppIcon ensuring consistent icon rendering across Xiaomi, Nothing, and other OEM devices.",
                    "Interactive and clickable Official, Community, and Unverified confidence status filter badges.",
                    "Fully functional Sort By control supporting Default, Alphabetical (A–Z), Compatibility, and Availability sorting options."
                )
            )
        )
    ),
    ReleaseInfo(
        version = "v0.1.2",
        date = "22 November 2026",
        isLatest = false,
        summary = "Launcher registry refactoring, UI polish, and back navigation enhancements.",
        sections = listOf(
            ReleaseSection(
                title = "Launcher Improvements",
                bullets = listOf(
                    "Separated Original Apex and AirBorne ApexLauncher identities with correct package IDs and source verification.",
                    "Replaced incorrect Apex icon artwork with authentic app icons.",
                    "Improved launcher availability labels, including explicit Delisted status."
                )
            ),
            ReleaseSection(
                title = "UI & Toolbar Polish",
                bullets = listOf(
                    "Simplified launcher setup destination instructions and removed redundant labels.",
                    "Standardized page headers, title positioning, and status bar padding across all screens.",
                    "Aligned Home Settings gear and Settings Back button on the exact same horizontal toolbar line."
                )
            ),
            ReleaseSection(
                title = "Navigation Improvements",
                bullets = listOf(
                    "Added swipe-down dismissal for launcher information sheets.",
                    "Improved system Back navigation across ZINK tabs.",
                    "Back navigation now correctly returns to ZINK Home before exiting."
                )
            )
        )
    ),
    ReleaseInfo(
        version = "v0.1.1",
        date = "21 November 2026",
        isLatest = false,
        summary = "Initial public release of ZINK featuring a refined dark glassOS aesthetic.",
        sections = listOf(
            ReleaseSection(
                title = "Core Features",
                bullets = listOf(
                    "Graphite/black surfaces and Pure AMOLED Black display mode.",
                    "84 curated silver-dark custom icons with category filtering and search.",
                    "43 dark visual wallpapers with favorites persistence, preview zoom/pan, download, and Home/Lock application."
                )
            ),
            ReleaseSection(
                title = "Launcher Compatibility",
                bullets = listOf(
                    "Curated setup guides and compatibility for 15 major launchers."
                )
            ),
            ReleaseSection(
                title = "Icon Requests",
                bullets = listOf(
                    "Built-in request composer supporting installed unthemed apps, themed catalog selection, and manual entries."
                )
            )
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(
    onDismiss: () -> Unit
) {
    val colors = LocalZinkColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expandedVersions = remember { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Muted)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val titleFont = com.gesuling.zink.ui.theme.LocalTitleFont.current.fontFamily
                    Text(
                        text = "RELEASE CHANGELOG",
                        color = Silver,
                        fontFamily = titleFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "ZINK VERSION HISTORY & UPDATES",
                        color = Muted,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .pressScale()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Silver,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            zinkReleaseHistory.forEach { release ->
                val isExpanded = expandedVersions.value.contains(release.version)
                ReleaseCard(
                    release = release,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedVersions.value = if (isExpanded) {
                            expandedVersions.value - release.version
                        } else {
                            expandedVersions.value + release.version
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ReleaseCard(
    release: ReleaseInfo,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalZinkColors.current
    val isPublished = isReleasePublished(release.version)
    val dateDisplay = if (isPublished) {
        "Released · ${release.date}"
    } else {
        "Planned Release · ${release.date}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .border(
                width = 1.dp,
                color = if (release.isLatest) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp)
            )
            .pressScale(onClick = onToggle)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = release.version,
                    color = Silver,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (release.isLatest) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CURRENT",
                            color = Silver,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Text(
                text = dateDisplay,
                color = Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = release.summary,
            color = Silver.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 1.dp
                )

                Spacer(Modifier.height(14.dp))

                release.sections.forEach { section ->
                    Text(
                        text = section.title,
                        color = Silver,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    section.bullets.forEach { bullet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = bullet,
                                color = Silver.copy(alpha = 0.80f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
