package com.gesuling.zink.ui.icons

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gesuling.zink.R
import com.gesuling.zink.ui.anim.NaturalOrderComparator
import com.gesuling.zink.ui.anim.glowingBorderOnPress
import com.gesuling.zink.ui.anim.pressScale

import com.gesuling.zink.ui.theme.LocalZinkColors
import com.gesuling.zink.ui.components.ZinkPageHeader

private val Silver = Color(0xFFE0E0E0)
private val Muted = Color(0xFF888888)

data class AppIconItem(
    val id: Int,
    val appName: String,
    val packageName: String,
    val category: String,
    val drawableResId: Int
)

val sampleIconsList = listOf(
    AppIconItem(1, "YouTube Music", "com.google.android.apps.youtube.music", "Media", R.drawable.icon_youtube_music),
    AppIconItem(2, "YouTube", "com.google.android.youtube", "Media", R.drawable.icon_youtube),
    AppIconItem(3, "Morphe Manager", "com.morphe.manager", "Tools", R.drawable.icon_morphe_manager),
    AppIconItem(4, "MicroG Services", "com.morphe.microg", "System", R.drawable.icon_microg),
    AppIconItem(5, "Proton Mail", "ch.protonmail.android", "Communication", R.drawable.icon_proton_mail),
    AppIconItem(6, "Proton VPN", "ch.protonvpn.android", "Tools", R.drawable.icon_proton_vpn),
    AppIconItem(7, "Call of Duty", "com.activision.callofduty.shooter", "Games", R.drawable.icon_call_of_duty),
    AppIconItem(8, "Adobe Lightroom", "com.adobe.lrmobile", "Photography", R.drawable.icon_adobe_lightroom),
    AppIconItem(9, "Google Play Store", "com.android.vending", "System", R.drawable.icon_play_store),
    AppIconItem(10, "Zero", "com.anotherwhy.zero", "Tools", R.drawable.icon_zero),
    AppIconItem(11, "Vianet", "com.app.vianet", "Tools", R.drawable.icon_vianet),
    AppIconItem(12, "Lauro", "com.asenom.lauro", "Tools", R.drawable.icon_lauro),
    AppIconItem(13, "Brave Browser", "com.brave.browser", "Internet", R.drawable.icon_brave_browser),
    AppIconItem(14, "Casseo", "com.casseo.myapp", "Tools", R.drawable.icon_casseo),
    AppIconItem(15, "1.1.1.1 WARP", "com.cloudflare.onedotonedotonedotone", "Tools", R.drawable.icon_cloudflare_1111),
    AppIconItem(16, "MissingCore Music", "com.cyanchill.missingcore.music", "Media", R.drawable.icon_missingcore_music),
    AppIconItem(17, "Daraz Shopping", "com.daraz.android", "Shopping", R.drawable.icon_daraz),
    AppIconItem(18, "Discord", "com.discord", "Social", R.drawable.icon_discord),
    AppIconItem(19, "ES File Explorer", "com.estrongs.android.pop", "System", R.drawable.icon_es_file_explorer),
    AppIconItem(20, "eSewa Wallet", "com.f1soft.esewa", "Finance", R.drawable.icon_esewa),
    AppIconItem(21, "Nabil Bank", "com.f1soft.nabilmbank", "Finance", R.drawable.icon_nabil_bank),
    AppIconItem(22, "Facebook", "com.facebook.katana", "Social", R.drawable.icon_facebook),
    AppIconItem(23, "Facebook Lite", "com.facebook.lite", "Social", R.drawable.icon_facebook_lite),
    AppIconItem(24, "Messenger", "com.facebook.orca", "Social", R.drawable.icon_messenger),
    AppIconItem(25, "Do Thing", "com.firstyogi.dothing", "Productivity", R.drawable.icon_dothing),
    AppIconItem(26, "Vlog", "com.frontrow.vlog", "Media", R.drawable.icon_vlog),
    AppIconItem(27, "FumoShare", "com.fumoshare.app", "Tools", R.drawable.icon_fumoshare),
    AppIconItem(28, "Zink App", "com.gesuling.zink", "System", R.drawable.icon_zink_app),
    AppIconItem(29, "GitHub", "com.github.android", "Productivity", R.drawable.icon_github),
    AppIconItem(30, "Glyph Four", "com.glyphfour.app", "Customization", R.drawable.icon_glyph_four),
    AppIconItem(31, "Glyph Play", "com.glyphplay.player", "Media", R.drawable.icon_glyph_play),
    AppIconItem(32, "Find My Device", "com.google.android.apps.adm", "System", R.drawable.icon_google_find_device),
    AppIconItem(33, "Authenticator", "com.google.android.apps.authenticator2", "Security", R.drawable.icon_google_authenticator),
    AppIconItem(34, "Google Maps", "com.google.android.apps.maps", "Navigation", R.drawable.icon_google_maps),
    AppIconItem(35, "Google Messages", "com.google.android.apps.messaging", "Communication", R.drawable.icon_google_messages),
    AppIconItem(36, "Personal Safety", "com.google.android.apps.safetyhub", "System", R.drawable.icon_google_safety),
    AppIconItem(37, "Google Translate", "com.google.android.apps.translate", "Tools", R.drawable.icon_google_translate),
    AppIconItem(38, "Google Contacts", "com.google.android.contacts", "System", R.drawable.icon_google_contacts),
    AppIconItem(39, "Google Clock", "com.google.android.deskclock", "System", R.drawable.icon_google_clock),
    AppIconItem(40, "Google Phone", "com.google.android.dialer", "System", R.drawable.icon_google_phone),
    AppIconItem(41, "Gmail", "com.google.android.gm", "Communication", R.drawable.icon_gmail),
    AppIconItem(42, "N-Calculator", "com.hearthborn.studios.ncalc", "Tools", R.drawable.icon_ncalc),
    AppIconItem(43, "N-Dialer", "com.hearthborn.studios.ndial", "System", R.drawable.icon_ndial),
    AppIconItem(44, "N-Files", "com.hearthborn.studios.nfiles", "System", R.drawable.icon_nfiles),
    AppIconItem(45, "Minimal Widgets", "com.jndapp.minimal.widgets", "Customization", R.drawable.icon_minimal_widgets),
    AppIconItem(46, "Khalti Wallet", "com.khalti", "Finance", R.drawable.icon_khalti),
    AppIconItem(47, "Mobile Legends", "com.mobile.legends", "Games", R.drawable.icon_mobile_legends),
    AppIconItem(48, "Monospace Notes", "com.monospace.notes", "Productivity", R.drawable.icon_monospace_notes),
    AppIconItem(49, "Kimi Chat AI", "com.moonshot.kimichat", "AI & Tech", R.drawable.icon_kimi_chat),
    AppIconItem(50, "Mornix", "com.mornix.app", "Tools", R.drawable.icon_mornix),
    AppIconItem(51, "Ncell", "com.mventus.ncell.activity", "Communication", R.drawable.icon_ncell),
    AppIconItem(52, "N-Calendar", "com.ncalendar.app", "System", R.drawable.icon_ncalendar),
    AppIconItem(53, "Terminus Player", "com.necroware.terminusplayer", "Media", R.drawable.icon_terminus_player),
    AppIconItem(54, "Stellarium Mobile", "com.noctuasoftware.stellarium.free", "Education", R.drawable.icon_stellarium),
    AppIconItem(55, "Nothing Camera", "com.nothing.camera", "System", R.drawable.icon_nothing_camera),
    AppIconItem(56, "Nothing Essential", "com.nothing.essentialapps", "System", R.drawable.icon_nothing_essential),
    AppIconItem(57, "Nothing Gallery", "com.nothing.gallery", "System", R.drawable.icon_nothing_gallery),
    AppIconItem(58, "Glyph Composer", "com.nothing.glyph", "Customization", R.drawable.icon_nothing_glyph),
    AppIconItem(59, "Sound Recorder", "com.nothing.soundrecorder", "Tools", R.drawable.icon_nothing_recorder),
    AppIconItem(60, "Nothing Weather", "com.nothing.weather", "Weather", R.drawable.icon_nothing_weather),
    AppIconItem(61, "ChatGPT", "com.openai.chatgpt", "AI & Tech", R.drawable.icon_chatgpt),
    AppIconItem(62, "Orion Store", "com.orion.store", "Shopping", R.drawable.icon_orion_store),
    AppIconItem(63, "Glyph Museum", "com.pauwma.glyphmuseum", "Customization", R.drawable.icon_glyph_museum),
    AppIconItem(64, "Moru Wallet", "com.paynep.moru", "Finance", R.drawable.icon_moru_wallet),
    AppIconItem(65, "PocketPal AI", "com.pocketpalai", "AI & Tech", R.drawable.icon_pocketpal_ai),
    AppIconItem(66, "RAR / WinRAR", "com.rarlab.rar", "Tools", R.drawable.icon_rar),
    AppIconItem(67, "Reddit", "com.reddit.frontpage", "Social", R.drawable.icon_reddit),
    AppIconItem(68, "Stremio", "com.stremio.one", "Media", R.drawable.icon_stremio),
    AppIconItem(69, "Clash of Clans", "com.supercell.clashofclans", "Games", R.drawable.icon_clash_of_clans),
    AppIconItem(70, "Text Based RPG", "com.techyonic.textbasedrpg", "Games", R.drawable.icon_text_rpg),
    AppIconItem(71, "Truecaller", "com.truecaller", "Communication", R.drawable.icon_truecaller),
    AppIconItem(72, "Steam", "com.valvesoftware.android.steam.community", "Games", R.drawable.icon_steam),
    AppIconItem(73, "Steam Chat", "com.valvesoftware.android.steam.friendsui", "Social", R.drawable.icon_steam_chat),
    AppIconItem(74, "Viewfinder", "com.vwfndr.mbl", "Photography", R.drawable.icon_vwfndr),
    AppIconItem(75, "WhatsApp", "com.whatsapp", "Social", R.drawable.icon_whatsapp),
    AppIconItem(76, "Yahoo Mail", "com.yahoo.mobile.client.android.mail", "Communication", R.drawable.icon_yahoo_mail),
    AppIconItem(77, "Yango Ride", "com.yandex.yango", "Travel", R.drawable.icon_yango),
    AppIconItem(78, "Device Info HW", "com.ytheekshana.deviceinfo", "System", R.drawable.icon_device_info),
    AppIconItem(79, "Mornye", "com.zarz.mornye", "Tools", R.drawable.icon_mornye),
    AppIconItem(80, "GetIcon", "de.lemke.geticon", "Tools", R.drawable.icon_geticon),
    AppIconItem(81, "Essential Key", "dev.buttonooo.essential.key", "Tools", R.drawable.icon_essential_key),
    AppIconItem(82, "Home Workout", "homeworkout.homeworkouts.noequipment", "Health", R.drawable.icon_home_workout),
    AppIconItem(83, "Proton Drive", "me.proton.android.drive", "Cloud", R.drawable.icon_proton_drive),
    AppIconItem(84, "Battery Hero", "redpi.apps.batteryhero", "System", R.drawable.icon_battery_hero)
).sortedWith(compareBy(NaturalOrderComparator) { it.appName })

object IconsStateManager {
    private const val PREFS_NAME = "zink_icons_prefs"
    private const val KEY_SEARCH_QUERY = "search_query"
    private const val KEY_SELECTED_CATEGORY = "selected_category"

    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("All")

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        searchQuery = prefs.getString(KEY_SEARCH_QUERY, "") ?: ""
        selectedCategory = prefs.getString(KEY_SELECTED_CATEGORY, "All") ?: "All"
    }

    fun setSearchQuery(context: Context, query: String) {
        searchQuery = query
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SEARCH_QUERY, query)
            .apply()
    }

    fun setSelectedCategory(context: Context, category: String) {
        selectedCategory = category
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_CATEGORY, category)
            .apply()
    }

    fun resetForTesting() {
        searchQuery = ""
        selectedCategory = "All"
    }
}

@Composable
fun IconsScreen() {
    val context = LocalContext.current
    val colors = LocalZinkColors.current
    val searchQuery = IconsStateManager.searchQuery
    val selectedCategory = IconsStateManager.selectedCategory
    var selectedIcon by remember { mutableStateOf<AppIconItem?>(null) }

    val categories = remember {
        listOf("All") + sampleIconsList.map { it.category }.distinct().sorted()
    }

    val filteredIcons = remember(searchQuery, selectedCategory) {
        sampleIconsList.filter { item ->
            (selectedCategory == "All" || item.category == selectedCategory) &&
                    (searchQuery.isBlank() ||
                            item.appName.contains(searchQuery, ignoreCase = true) ||
                            item.packageName.contains(searchQuery, ignoreCase = true) ||
                            item.category.contains(searchQuery, ignoreCase = true))
        }.sortedWith(compareBy(NaturalOrderComparator) { it.appName })
    }

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
                title = "ICON CATALOGUE",
                subtitleContent = {
                    AnimatedContent(
                        targetState = filteredIcons.size,
                        transitionSpec = {
                            (slideInVertically { h -> h } + fadeIn()).togetherWith(slideOutVertically { h -> -h } + fadeOut())
                        },
                        label = "icon_count_anim"
                    ) { count ->
                        Text(
                            text = "$count SILVER-GLASS ICONS",
                            color = Muted,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            )
            Spacer(Modifier.height(4.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { IconsStateManager.setSearchQuery(context, it) },
                placeholder = { Text("Search 84 icons...", color = Muted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = Silver
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { IconsStateManager.setSearchQuery(context, "") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Silver)
                        }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.cardBackground,
                    unfocusedContainerColor = colors.cardBackground,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = Silver,
                    unfocusedTextColor = Silver
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            Spacer(Modifier.height(14.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) Silver else Color(0x22141417))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                            .clickable { IconsStateManager.setSelectedCategory(context, cat) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color(0xFF141417) else Silver,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Icons Grid
            if (filteredIcons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No icons found for \"$searchQuery\"",
                        color = Muted,
                        fontSize = 14.sp
                    )
                }
            } else {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = navBarBottom + 104.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredIcons,
                        key = { it.id },
                        contentType = { "app_icon_card" }
                    ) { iconItem ->
                        IconGridCard(
                            iconItem = iconItem,
                            onClick = { selectedIcon = iconItem }
                        )
                    }
                }
            }
        }

        selectedIcon?.let { iconItem ->
            IconDetailDialog(
                iconItem = iconItem,
                onDismiss = { selectedIcon = null }
            )
        }
    }
}

@Composable
fun IconGridCard(
    iconItem: AppIconItem,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.90f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "icon_entry_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .pressScale(onClick = onClick)
    ) {
        Image(
            painter = painterResource(iconItem.drawableResId),
            contentDescription = iconItem.appName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = iconItem.appName,
            color = Silver,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun IconDetailDialog(
    iconItem: AppIconItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalZinkColors.current

    fun copyPackageName() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Package Name", iconItem.packageName)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied: ${iconItem.packageName}", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Silver)
                    }
                }

                Image(
                    painter = painterResource(iconItem.drawableResId),
                    contentDescription = iconItem.appName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(16.dp, CircleShape)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = iconItem.appName,
                    color = Silver,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = iconItem.category.uppercase(),
                    color = Muted,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = iconItem.packageName,
                        color = Muted,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { copyPackageName() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xDD222226),
                        contentColor = Silver
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .pressScale()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Package Name", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Preview
@Composable
fun IconsScreenPreview() {
    IconsScreen()
}
