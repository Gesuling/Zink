package com.gesuling.zink.ui.requests

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.gesuling.zink.ui.anim.NaturalOrderComparator
import com.gesuling.zink.ui.anim.glowingBorderOnPress
import com.gesuling.zink.ui.anim.pressScale
import com.gesuling.zink.ui.icons.AppIconItem
import com.gesuling.zink.ui.icons.sampleIconsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.gesuling.zink.ui.theme.LocalZinkColors
import com.gesuling.zink.ui.components.ZinkPageHeader

private val Silver = Color(0xFFE0E0E0)
private val Muted = Color(0xFF888888)

data class UnthemedAppItem(
    val appName: String,
    val packageName: String,
    val componentName: String,
    val icon: Drawable? = null
)

val fallbackUnthemedApps = listOf(
    UnthemedAppItem("Spotify", "com.spotify.music", "com.spotify.music.MainActivity"),
    UnthemedAppItem("Notion", "notion.id", "notion.id.MainActivity"),
    UnthemedAppItem("Obsidian", "md.obsidian", "md.obsidian.MainActivity"),
    UnthemedAppItem("Discord", "com.discord", "com.discord.main.MainActivity"),
    UnthemedAppItem("Signal", "org.thoughtcrime.securesms", "org.thoughtcrime.securesms.HomeActivity"),
    UnthemedAppItem("Netflix", "com.netflix.mediaclient", "com.netflix.mediaclient.ui.launch.UIWebViewActivity"),
    UnthemedAppItem("Twitch", "tv.twitch.android.app", "tv.twitch.android.app.main.MainActivity"),
    UnthemedAppItem("Duolingo", "com.duolingo", "com.duolingo.splash.SplashActivity"),
    UnthemedAppItem("Shazam", "com.shazam.android", "com.shazam.android.SplashActivity"),
    UnthemedAppItem("Pocket", "com.ideashower.readitlater.pro", "com.ideashower.readitlater.pro.activity.SplashActivity")
)

data class RequestsStateSnapshot(
    val selectedMode: Int = 0,
    val searchQuery: String = "",
    val selectedPackageNames: Set<String> = emptySet(),
    val selectedThemedAppIds: Set<Int> = emptySet(),
    val manualAppName: String = "",
    val manualPackageName: String = "",
    val manualNote: String = ""
)

internal fun encodeIntSetToStringSet(set: Set<Int>): Set<String> =
    set.mapTo(mutableSetOf()) { it.toString() }

internal fun decodeStringSetToIntSet(set: Set<String>?): Set<Int> =
    set?.mapNotNullTo(mutableSetOf()) { it.toIntOrNull() } ?: emptySet()

internal fun restoreRequestsStateSnapshot(
    savedMode: Int?,
    savedQuery: String?,
    savedPackages: Set<String>?,
    savedThemedIds: Set<String>?,
    savedManualName: String?,
    savedManualPkg: String?,
    savedManualNote: String?
): RequestsStateSnapshot = RequestsStateSnapshot(
    selectedMode = (savedMode ?: 0).coerceIn(0, 2),
    searchQuery = savedQuery ?: "",
    selectedPackageNames = savedPackages ?: emptySet(),
    selectedThemedAppIds = decodeStringSetToIntSet(savedThemedIds),
    manualAppName = savedManualName ?: "",
    manualPackageName = savedManualPkg ?: "",
    manualNote = savedManualNote ?: ""
)

object RequestsSelectionManager {
    private const val PREFS_NAME = "zink_requests_prefs"
    private const val KEY_SELECTED_MODE = "selected_mode"
    private const val KEY_SEARCH_QUERY = "search_query"
    private const val KEY_SELECTED_PACKAGES = "selected_packages"
    private const val KEY_SELECTED_THEMED_IDS = "selected_themed_ids"
    private const val KEY_MANUAL_APP_NAME = "manual_app_name"
    private const val KEY_MANUAL_PACKAGE_NAME = "manual_package_name"
    private const val KEY_MANUAL_NOTE = "manual_note"

    var selectedMode by mutableIntStateOf(0)
    var searchQuery by mutableStateOf("")
    val selectedPackageNames = mutableStateSetOf<String>()
    val selectedThemedAppIds = mutableStateSetOf<Int>()
    var manualAppName by mutableStateOf("")
    var manualPackageName by mutableStateOf("")
    var manualNote by mutableStateOf("")

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snapshot = restoreRequestsStateSnapshot(
            savedMode = prefs.getInt(KEY_SELECTED_MODE, 0),
            savedQuery = prefs.getString(KEY_SEARCH_QUERY, ""),
            savedPackages = prefs.getStringSet(KEY_SELECTED_PACKAGES, emptySet()),
            savedThemedIds = prefs.getStringSet(KEY_SELECTED_THEMED_IDS, emptySet()),
            savedManualName = prefs.getString(KEY_MANUAL_APP_NAME, ""),
            savedManualPkg = prefs.getString(KEY_MANUAL_PACKAGE_NAME, ""),
            savedManualNote = prefs.getString(KEY_MANUAL_NOTE, "")
        )

        selectedMode = snapshot.selectedMode
        searchQuery = snapshot.searchQuery

        selectedPackageNames.clear()
        selectedPackageNames.addAll(snapshot.selectedPackageNames)

        selectedThemedAppIds.clear()
        selectedThemedAppIds.addAll(snapshot.selectedThemedAppIds)

        manualAppName = snapshot.manualAppName
        manualPackageName = snapshot.manualPackageName
        manualNote = snapshot.manualNote
    }

    fun setMode(context: Context, mode: Int) {
        val validMode = mode.coerceIn(0, 2)
        selectedMode = validMode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SELECTED_MODE, validMode)
            .apply()
    }

    fun setSearchQuery(context: Context, query: String) {
        searchQuery = query
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SEARCH_QUERY, query)
            .apply()
    }

    fun togglePackageSelection(context: Context, packageName: String) {
        if (selectedPackageNames.contains(packageName)) {
            selectedPackageNames.remove(packageName)
        } else {
            selectedPackageNames.add(packageName)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_PACKAGES, selectedPackageNames.toSet())
            .apply()
    }

    fun selectAllPackages(context: Context, packageNames: List<String>) {
        selectedPackageNames.clear()
        selectedPackageNames.addAll(packageNames)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_PACKAGES, selectedPackageNames.toSet())
            .apply()
    }

    fun deselectAllPackages(context: Context) {
        selectedPackageNames.clear()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SELECTED_PACKAGES)
            .apply()
    }

    fun toggleThemedAppSelection(context: Context, appId: Int) {
        if (selectedThemedAppIds.contains(appId)) {
            selectedThemedAppIds.remove(appId)
        } else {
            selectedThemedAppIds.add(appId)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_THEMED_IDS, encodeIntSetToStringSet(selectedThemedAppIds.toSet()))
            .apply()
    }

    fun selectAllThemedApps(context: Context, appIds: List<Int>) {
        selectedThemedAppIds.clear()
        selectedThemedAppIds.addAll(appIds)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_THEMED_IDS, encodeIntSetToStringSet(selectedThemedAppIds.toSet()))
            .apply()
    }

    fun deselectAllThemedApps(context: Context) {
        selectedThemedAppIds.clear()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SELECTED_THEMED_IDS)
            .apply()
    }

    fun setManualAppName(context: Context, name: String) {
        manualAppName = name
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MANUAL_APP_NAME, name)
            .apply()
    }

    fun setManualPackageName(context: Context, pkg: String) {
        manualPackageName = pkg
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MANUAL_PACKAGE_NAME, pkg)
            .apply()
    }

    fun setManualNote(context: Context, note: String) {
        manualNote = note
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MANUAL_NOTE, note)
            .apply()
    }

    fun snapshot(): RequestsStateSnapshot = RequestsStateSnapshot(
        selectedMode = selectedMode,
        searchQuery = searchQuery,
        selectedPackageNames = selectedPackageNames.toSet(),
        selectedThemedAppIds = selectedThemedAppIds.toSet(),
        manualAppName = manualAppName,
        manualPackageName = manualPackageName,
        manualNote = manualNote
    )

    fun resetForTesting() {
        selectedMode = 0
        searchQuery = ""
        selectedPackageNames.clear()
        selectedThemedAppIds.clear()
        manualAppName = ""
        manualPackageName = ""
        manualNote = ""
    }
}

data class RequestSerializationResult(
    val subject: String,
    val body: String,
    val count: Int
)

fun serializeRequest(
    selectedMode: Int,
    installedUnthemedApps: List<UnthemedAppItem>,
    selectedPackageNames: Set<String>,
    themedAppList: List<AppIconItem>,
    selectedThemedAppIds: Set<Int>,
    manualAppName: String,
    manualPackageName: String,
    manualNote: String
): RequestSerializationResult {
    val selectedApps = installedUnthemedApps.filter { selectedPackageNames.contains(it.packageName) }
    val selectedThemedApps = themedAppList.filter { selectedThemedAppIds.contains(it.id) }
    val requestCount = when (selectedMode) {
        0 -> selectedApps.size
        1 -> selectedThemedApps.size
        else -> if (manualAppName.isNotBlank() || manualPackageName.isNotBlank()) 1 else 0
    }
    val bodyBuilder = StringBuilder()
    bodyBuilder.append("Zink Icon Request Submission\n\n")

    when (selectedMode) {
        0 -> {
            bodyBuilder.append("Requested Unthemed Apps (${selectedApps.size}):\n")
            selectedApps.forEachIndexed { i, app ->
                bodyBuilder.append("${i + 1}. ${app.appName} (${app.packageName})\n   Component: ComponentInfo{${app.componentName}}\n")
            }
        }
        1 -> {
            bodyBuilder.append("Requested Themed Apps (${selectedThemedApps.size}):\n")
            selectedThemedApps.forEachIndexed { i, app ->
                bodyBuilder.append("${i + 1}. ${app.appName} (${app.packageName})\n")
            }
        }
        2 -> {
            bodyBuilder.append("Manual Icon Request:\n")
            bodyBuilder.append("App Name: $manualAppName\n")
            bodyBuilder.append("Package / Link: $manualPackageName\n")
            if (manualNote.isNotBlank()) {
                bodyBuilder.append("Designer Note: $manualNote\n")
            }
        }
    }

    bodyBuilder.append("\nSent from Zink Android App")
    return RequestSerializationResult(
        subject = "Zink Icon Request ($requestCount apps)",
        body = bodyBuilder.toString(),
        count = requestCount
    )
}

@Composable
fun RequestsScreen() {
    val context = LocalContext.current
    val colors = LocalZinkColors.current

    val selectedMode = RequestsSelectionManager.selectedMode
    val searchQuery = RequestsSelectionManager.searchQuery

    val manualAppName = RequestsSelectionManager.manualAppName
    val manualPackageName = RequestsSelectionManager.manualPackageName
    val manualNote = RequestsSelectionManager.manualNote

    var installedUnthemedApps by remember { mutableStateOf<List<UnthemedAppItem>>(emptyList()) }
    val selectedPackageNames = RequestsSelectionManager.selectedPackageNames
    val selectedThemedAppIds = RequestsSelectionManager.selectedThemedAppIds
    var isLoadingApps by remember { mutableStateOf(true) }

    val themedAppList = remember { sampleIconsList.sortedWith(compareBy(NaturalOrderComparator) { it.appName }) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mappedPackages = sampleIconsList.map { it.packageName }.toSet()
            val scannedMap = mutableMapOf<String, UnthemedAppItem>()

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = try {
                pm.queryIntentActivities(mainIntent, 0)
            } catch (e: Exception) {
                emptyList()
            }

            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (pkg != context.packageName && !mappedPackages.contains(pkg) && !scannedMap.containsKey(pkg)) {
                    val label = ri.loadLabel(pm).toString()
                    val actName = ri.activityInfo.name
                    val iconDrawable = try { ri.loadIcon(pm) } catch (e: Exception) { null }
                    scannedMap[pkg] = UnthemedAppItem(label, pkg, "$pkg/$actName", iconDrawable)
                }
            }

            if (scannedMap.size < 3) {
                val installed = try { pm.getInstalledPackages(0) } catch (e: Exception) { emptyList() }
                for (pi in installed) {
                    val pkg = pi.packageName
                    if (pkg != context.packageName && !mappedPackages.contains(pkg) && !scannedMap.containsKey(pkg)) {
                        val appInfo = pi.applicationInfo
                        if (appInfo != null) {
                            val launchIntent = pm.getLaunchIntentForPackage(pkg)
                            if (launchIntent != null) {
                                val label = appInfo.loadLabel(pm).toString()
                                val actName = launchIntent.component?.className ?: "$pkg.MainActivity"
                                val iconDrawable = try { appInfo.loadIcon(pm) } catch (e: Exception) { null }
                                scannedMap[pkg] = UnthemedAppItem(label, pkg, "$pkg/$actName", iconDrawable)
                            }
                        }
                    }
                }
            }

            val scannedList = scannedMap.values.sortedWith(compareBy(NaturalOrderComparator) { it.appName })
            val fallbackSorted = fallbackUnthemedApps.sortedWith(compareBy(NaturalOrderComparator) { it.appName })
            val listToUse = if (scannedList.isNotEmpty()) scannedList else fallbackSorted

            withContext(Dispatchers.Main) {
                installedUnthemedApps = listToUse
                isLoadingApps = false
            }
        }
    }

    val filteredApps = remember(searchQuery, installedUnthemedApps) {
        installedUnthemedApps.filter { item ->
            searchQuery.isBlank() ||
                    item.appName.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true)
        }.sortedWith(compareBy(NaturalOrderComparator) { it.appName })
    }

    val filteredThemedApps = remember(searchQuery, themedAppList) {
        themedAppList.filter { item ->
            searchQuery.isBlank() ||
                    item.appName.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true)
        }.sortedWith(compareBy(NaturalOrderComparator) { it.appName })
    }

    fun submitRequest() {
        val serialized = serializeRequest(
            selectedMode = selectedMode,
            installedUnthemedApps = installedUnthemedApps,
            selectedPackageNames = selectedPackageNames,
            themedAppList = themedAppList,
            selectedThemedAppIds = selectedThemedAppIds,
            manualAppName = manualAppName,
            manualPackageName = manualPackageName,
            manualNote = manualNote
        )
        val requestBody = serialized.body
        val requestSubject = serialized.subject
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:gesuling@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, requestSubject)
            putExtra(Intent.EXTRA_TEXT, requestBody)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send Icon Request via Email"))
        } catch (_: Exception) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(requestSubject, "$requestSubject\n\n$requestBody"))
                Toast.makeText(context, "No email client found. Request copied to clipboard.", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open email or copy the request.", Toast.LENGTH_LONG).show()
            }
        }
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
                title = "REQUESTS",
                subtitle = "SUBMIT UNTHEMED APPS ANYTIME"
            )
            Spacer(Modifier.height(4.dp))

            // Dynamic Summary Cards according to active segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val leftTitle = when (selectedMode) {
                    0 -> "MISSING"
                    1 -> "THEMED"
                    else -> "MANUAL"
                }
                val leftCount = when (selectedMode) {
                    0 -> "${installedUnthemedApps.size}"
                    1 -> "${themedAppList.size}"
                    else -> "1"
                }
                val rightCount = when (selectedMode) {
                    0 -> "${selectedPackageNames.size}"
                    1 -> "${selectedThemedAppIds.size}"
                    else -> "1"
                }

                SummaryCardBox(
                    title = leftTitle,
                    count = leftCount,
                    modifier = Modifier.weight(1f)
                )
                SummaryCardBox(
                    title = "SELECTED",
                    count = rightCount,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Segmented Switch: 3 Equally Sized Options (Unthemed Apps | Themed Apps | Manual Entry)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(23.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    0 to "Unthemed Apps",
                    1 to "Themed Apps",
                    2 to "Manual Entry"
                ).forEach { (modeIndex, labelText) ->
                    val isSelected = selectedMode == modeIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF282830) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { RequestsSelectionManager.setMode(context, modeIndex) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelText,
                            color = if (isSelected) Color.White else Silver,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedMode) {
                0 -> {
                    // Unthemed Apps View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { RequestsSelectionManager.setSearchQuery(context, it) },
                            placeholder = { Text("Search installed apps...", color = Muted, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = Silver)
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.cardBackground,
                                unfocusedContainerColor = colors.cardBackground,
                                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Silver,
                                unfocusedTextColor = Silver
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        )

                        val isAllSelected = selectedPackageNames.size == installedUnthemedApps.size
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.cardBackground)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                                .clickable {
                                    if (isAllSelected) {
                                        RequestsSelectionManager.deselectAllPackages(context)
                                    } else {
                                        RequestsSelectionManager.selectAllPackages(context, installedUnthemedApps.map { it.packageName })
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAllSelected) "Clear" else "Select All",
                                color = Silver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Silver, strokeWidth = 2.dp)
                        }
                    } else {
                        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = navBarBottom + 160.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val isChecked = selectedPackageNames.contains(app.packageName)
                                UnthemedAppCard(
                                    app = app,
                                    isChecked = isChecked,
                                    onToggle = {
                                        RequestsSelectionManager.togglePackageSelection(context, app.packageName)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Themed Apps View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { RequestsSelectionManager.setSearchQuery(context, it) },
                            placeholder = { Text("Search themed apps...", color = Muted, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = Silver)
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.cardBackground,
                                unfocusedContainerColor = colors.cardBackground,
                                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Silver,
                                unfocusedTextColor = Silver
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        )

                        val isAllSelected = selectedThemedAppIds.size == themedAppList.size
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.cardBackground)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                                .clickable {
                                    if (isAllSelected) {
                                        RequestsSelectionManager.deselectAllThemedApps(context)
                                    } else {
                                        RequestsSelectionManager.selectAllThemedApps(context, themedAppList.map { it.id })
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAllSelected) "Clear" else "Select All",
                                color = Silver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = navBarBottom + 104.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredThemedApps, key = { it.id }) { item ->
                            val isChecked = selectedThemedAppIds.contains(item.id)
                            ThemedAppCard(
                                item = item,
                                isSelected = isChecked,
                                onToggle = {
                                    RequestsSelectionManager.toggleThemedAppSelection(context, item.id)
                                }
                            )
                        }
                    }
                }
                else -> {
                    // Manual Entry Form
                    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = navBarBottom + 104.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.cardBackground)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Manual App Request",
                                color = Silver,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedTextField(
                                value = manualAppName,
                                onValueChange = { RequestsSelectionManager.setManualAppName(context, it) },
                                label = { Text("App Name", color = Muted) },
                                placeholder = { Text("e.g. Spotify", color = Muted) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Silver,
                                    unfocusedTextColor = Silver,
                                    focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = manualPackageName,
                                onValueChange = { RequestsSelectionManager.setManualPackageName(context, it) },
                                label = { Text("Package Name or Play Store Link", color = Muted) },
                                placeholder = { Text("e.g. com.spotify.music", color = Muted) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Silver,
                                    unfocusedTextColor = Silver,
                                    focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = manualNote,
                                onValueChange = { RequestsSelectionManager.setManualNote(context, it) },
                                label = { Text("Designer Note (Optional)", color = Muted) },
                                placeholder = { Text("e.g. Minimal silver glass logo style", color = Muted) },
                                maxLines = 3,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Silver,
                                    unfocusedTextColor = Silver,
                                    focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { submitRequest() },
                                enabled = manualAppName.isNotBlank() || manualPackageName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xDD222226),
                                    contentColor = Silver
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .pressScale()
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Submit Manual Request", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Floating submit bar for selected catalog apps
        val selectedRequestCount = when (selectedMode) {
            0 -> selectedPackageNames.size
            1 -> selectedThemedAppIds.size
            else -> 0
        }
        if (selectedMode in 0..1 && selectedRequestCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 82.dp, start = 20.dp, end = 20.dp)
            ) {
                Button(
                    onClick = { submitRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xEE222226),
                        contentColor = Silver
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .pressScale()
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Submit Request ($selectedRequestCount Apps)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCardBox(
    title: String,
    count: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalZinkColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(250)) { h -> h } + fadeIn())
                    .togetherWith(slideOutVertically(animationSpec = tween(250)) { h -> -h } + fadeOut())
            },
            label = "req_summary_anim"
        ) { targetCount ->
            Text(targetCount, color = Silver, fontSize = 24.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(4.dp))
        Text(title, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun UnthemedAppCard(
    app: UnthemedAppItem,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalZinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .glowingBorderOnPress(RoundedCornerShape(18.dp))
            .pressScale(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E24))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val iconDrawable = app.icon
            if (iconDrawable != null) {
                Image(
                    bitmap = iconDrawable.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Android,
                    contentDescription = null,
                    tint = Silver,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = Silver,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = app.packageName,
                color = Muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        Icon(
            imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isChecked) "Selected" else "Unselected",
            tint = if (isChecked) Color.White else Muted,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ThemedAppCard(
    item: AppIconItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalZinkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .glowingBorderOnPress(RoundedCornerShape(18.dp))
            .pressScale(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(item.drawableResId),
            contentDescription = item.appName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(46.dp)
                .shadow(6.dp, CircleShape)
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.appName,
                color = Silver,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.packageName,
                color = Muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isSelected) "Selected" else "Unselected",
            tint = if (isSelected) Color.White else Muted,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview
@Composable
fun RequestsScreenPreview() {
    RequestsScreen()
}
