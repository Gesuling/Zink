package com.gesuling.zink

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.gesuling.zink.ui.anim.glowingBorderOnPress
import com.gesuling.zink.ui.anim.pressScale
import com.gesuling.zink.ui.icons.IconsScreen
import com.gesuling.zink.ui.icons.sampleIconsList
import com.gesuling.zink.ui.requests.RequestsScreen
import com.gesuling.zink.ui.settings.SettingsScreen
import com.gesuling.zink.ui.theme.*
import com.gesuling.zink.ui.apply.ApplyScreen
import com.gesuling.zink.ui.wallpapers.FavouritesManager
import com.gesuling.zink.ui.wallpapers.WallpapersScreen
import com.gesuling.zink.ui.wallpapers.sampleWallpapers

private val Silver = Color(0xFFE0E0E0)
private val Muted = Color(0xFF888888)
cd C:\Users\LENOVO\Documents\Zink
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FavouritesManager.restore(this, sampleWallpapers.mapTo(mutableSetOf()) { it.id })
        enableEdgeToEdge()
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val context = LocalContext.current
            var isPureAmoled by remember {
                mutableStateOf(ZinkPreferences.getPureAmoled(context))
            }
            val zinkColors = if (isPureAmoled) AmoledBlackColors else GraphiteDarkColors

            CompositionLocalProvider(
                LocalZinkColors provides zinkColors,
                LocalPureAmoled provides isPureAmoled
            ) {
                ZinkTheme {
                    ZinkHomeScreen(
                        isPureAmoled = isPureAmoled,
                        onPureAmoledChange = { newVal ->
                            isPureAmoled = newVal
                            ZinkPreferences.setPureAmoled(context, newVal)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ZinkHomeScreen(
    isPureAmoled: Boolean,
    onPureAmoledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var headerArtwork by remember { mutableStateOf(ZinkPreferences.getHeaderArtwork(context)) }
    var launcherRefreshVersion by remember { mutableIntStateOf(0) }
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) launcherRefreshVersion++
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    val colors = LocalZinkColors.current

    val tabs = listOf("Home", "Icons", "Wallpapers", "Apply", "Requests")
    val tabIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.Apps,
        Icons.Outlined.Image,
        Icons.Outlined.Layers,
        Icons.AutoMirrored.Outlined.Send
    )
    val density = LocalDensity.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (isSettingsOpen) {
            Box(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(
                    headerArtwork = headerArtwork,
                    onHeaderArtworkChange = { artworkId ->
                        headerArtwork = ZinkPreferences.setHeaderArtwork(context, artworkId)
                    },
                    isPureAmoled = isPureAmoled,
                    onPuroAmoledChange = onPureAmoledChange,
                    onBackClick = { isSettingsOpen = false },
                    launcherRefreshVersion = launcherRefreshVersion,
                    onApplyToLauncherClick = {
                        selectedTab = 3
                        isSettingsOpen = false
                    }
                )
                GlassmorphicBottomNavigation(
                    tabs = tabs,
                    tabIcons = tabIcons,
                    selectedIndex = selectedTab,
                    onTabSelected = {
                        selectedTab = it
                        isSettingsOpen = false
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        } else {
            Scaffold(
                containerColor = colors.background,
                contentWindowInsets = WindowInsets(0)
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .padding(padding)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            val offsetPx = with(density) { 12.dp.roundToPx() }
                            (slideInVertically(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetY = { offsetPx * direction }
                            ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    targetOffsetY = { -offsetPx * direction }
                                ) + fadeOut(animationSpec = tween(200))
                            )
                        },
                        label = "screen_transition"
                    ) { tab ->
                        when (tab) {
                            1 -> {
                                IconsScreen()
                            }
                            2 -> {
                                WallpapersScreen()
                            }
                            3 -> {
                                ApplyScreen(launcherRefreshVersion)
                            }
                            4 -> {
                                RequestsScreen()
                            }
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = navBarBottom + 104.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(230.dp)
                                    ) {
                                        val headerDrawable = when (headerArtwork) {
                                            "Obsidian" -> R.drawable.obsidian_header
                                            "Nocturne" -> R.drawable.nocturne_header
                                            else -> R.drawable.zink_header
                                        }
                                        val headerAlignment = when (headerArtwork) {
                                            "Obsidian" -> Alignment.Center
                                            "Nocturne" -> Alignment.Center
                                            else -> Alignment.Center
                                        }
                                        Image(
                                            painter = painterResource(headerDrawable),
                                            contentDescription = "Zink header artwork",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alignment = headerAlignment
                                        )

                                        // Subtle Silver Settings Gear Icon over banner artwork (Transparent background, 48dp touch area)
                                        Box(
                                            modifier = Modifier
                                                .statusBarsPadding()
                                                .padding(top = 12.5.dp, end = 12.dp)
                                                .offset(y = (-20).dp)
                                                .align(Alignment.TopEnd)
                                                .size(48.dp)
                                                .pressScale { isSettingsOpen = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Settings,
                                                contentDescription = "Settings",
                                                tint = Silver,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(24.dp))

                                    Text(
                                        text = "Z I N K",
                                        color = Silver,
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Light,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = "DARKNESS, REFINED.",
                                        color = Muted,
                                        fontSize = 10.sp,
                                        letterSpacing = 3.sp,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(Modifier.height(32.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SummaryCard(
                                            "ICONS",
                                            "${sampleIconsList.size}",
                                            Modifier
                                                .weight(1f)
                                                .pressScale { selectedTab = 1 }
                                        )
                                        SummaryCard(
                                            "WALLPAPERS",
                                            "${sampleWallpapers.size}",
                                            Modifier
                                                .weight(1f)
                                                .pressScale { selectedTab = 2 }
                                        )
                                        SummaryCard(
                                            "FAVOURITES",
                                            "${FavouritesManager.favoritedIds.size}",
                                            Modifier
                                                .weight(1f)
                                                .pressScale { selectedTab = 2 }
                                        )
                                    }

                                    Spacer(Modifier.height(32.dp))

                                    Text(
                                        text = "EXPLORE",
                                        color = Silver,
                                        fontSize = 13.sp,
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.padding(horizontal = 22.dp)
                                    )

                                    Spacer(Modifier.height(14.dp))

                                    // Entry Point 2: Settings & Preferences row below Your Favourites in Explore section
                                    listOf(
                                        Triple("Icon Collection", "Explore ${sampleIconsList.size} silver-dark icons", 1),
                                        Triple("Wallpapers", "Explore ${sampleWallpapers.size} dark visual experiences", 2),
                                        Triple("Apply Icon Pack", "Launcher compatibility & setup guide", 3),
                                        Triple("Settings & Preferences", "Configure pack system, theme & launcher", 99)
                                    ).forEach { (title, subtitle, targetTab) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 5.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(colors.cardBackground)
                                                .glowingBorderOnPress(RoundedCornerShape(16.dp))
                                                .pressScale {
                                                    if (targetTab == 99) {
                                                        isSettingsOpen = true
                                                    } else {
                                                        selectedTab = targetTab
                                                    }
                                                }
                                                .padding(18.dp)
                                        ) {
                                            Text(title, color = Silver, fontSize = 15.sp)
                                            Spacer(Modifier.height(5.dp))
                                            Text(subtitle, color = Muted, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabs[tab].uppercase(),
                                        color = Silver,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 3.sp
                                    )
                                }
                            }
                        }
                    }

                    GlassmorphicBottomNavigation(
                        tabs = tabs,
                        tabIcons = tabIcons,
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassmorphicBottomNavigation(
    tabs: List<String>,
    tabIcons: List<ImageVector>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val tapScales = remember { List(tabs.size) { Animatable(1.0f) } }

    BoxWithConstraints(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(CircleShape)
            .background(Color(0xF0141416))
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f)
                ),
                shape = CircleShape
            )
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val tabCount = tabs.size
        val tabWidth = totalWidth / tabCount

        val animatedOffsetX by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioLowBouncy
            ),
            label = "pill_offset"
        )

        // Sliding Active Selection Pill
        Box(
            modifier = Modifier
                .offset(x = animatedOffsetX)
                .width(tabWidth)
                .height(48.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.11f)
                        )
                    )
                )
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Icons-Only Row (No Text Labels)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val currentScale = tapScales[index].value

                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                val scale = tapScales[index]
                                scale.snapTo(1.0f)
                                scale.animateTo(
                                    targetValue = 1.12f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessHigh,
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                                scale.animateTo(
                                    targetValue = 1.0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioNoBouncy
                                    )
                                )
                            }
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tabIcons[index],
                        contentDescription = tab,
                        tint = if (selected) Color.White else Color(0xFF8E8E93),
                        modifier = Modifier
                            .size(22.dp)
                            .then(
                                if (index == 4) {
                                    Modifier
                                        .offset(y = (-1).dp)
                                        .rotate(-45f)
                                } else Modifier
                            )
                            .graphicsLayer {
                                scaleX = currentScale
                                scaleY = currentScale
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, count: String, modifier: Modifier = Modifier) {
    val colors = LocalZinkColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .glowingBorderOnPress(RoundedCornerShape(14.dp))
            .pressScale()
            .padding(vertical = 17.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(250)) { height -> height } + fadeIn())
                    .togetherWith(slideOutVertically(animationSpec = tween(250)) { height -> -height } + fadeOut())
            },
            label = "card_count_anim"
        ) { targetCount ->
            Text(targetCount, color = Silver, fontSize = 23.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(6.dp))
        Text(title, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Preview
@Composable
fun ZinkHomeScreenPreview() {
    ZinkTheme {
        ZinkHomeScreen(isPureAmoled = false, onPureAmoledChange = {})
    }
}

@Preview
@Composable
fun GlassmorphicBottomNavigationPreview() {
    ZinkTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF101010))
                .padding(16.dp)
        ) {
            GlassmorphicBottomNavigation(
                tabs = listOf("Home", "Icons", "Wallpapers", "Apply", "Requests"),
                tabIcons = listOf(
                    Icons.Outlined.Home,
                    Icons.Outlined.Apps,
                    Icons.Outlined.Image,
                    Icons.Outlined.Layers,
                    Icons.AutoMirrored.Outlined.Send
                ),
                selectedIndex = 0,
                onTabSelected = {}
            )
        }
    }
}
