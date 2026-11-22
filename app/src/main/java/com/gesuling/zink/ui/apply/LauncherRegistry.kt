package com.gesuling.zink.ui.apply

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast

/**
 * Direct-apply feedback describes request dispatch, never confirmed application.
 */
internal fun directApplyFeedbackMessage(launcherName: String): String =
    "Apply request sent to $launcherName. Confirm in launcher if prompted."

internal enum class LauncherOpenRoute {
    APP_LAUNCHER,
    HOME_ACTIVITY,
    UNAVAILABLE
}

internal fun chooseLauncherOpenRoute(
    requestedPackage: String,
    isInstalled: Boolean,
    launchIntentPackage: String?,
    homeActivityPackage: String?
): LauncherOpenRoute = when {
    !isInstalled -> LauncherOpenRoute.UNAVAILABLE
    launchIntentPackage == requestedPackage -> LauncherOpenRoute.APP_LAUNCHER
    homeActivityPackage == requestedPackage -> LauncherOpenRoute.HOME_ACTIVITY
    else -> LauncherOpenRoute.UNAVAILABLE
}

internal data class LauncherOpenResult(
    val opened: Boolean,
    val message: String? = null
)

/**
 * Opens only the requested installed launcher. Some system launchers expose a HOME activity but
 * no regular app-drawer entry point, so use that package's own HOME activity as a safe fallback.
 */
internal fun openLauncher(context: Context, packageName: String, launcherName: String): LauncherOpenResult {
    val packageManager = context.packageManager
    val installed = try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    val launchIntent = if (installed) packageManager.getLaunchIntentForPackage(packageName) else null
    val launchActivityPackage = launchIntent?.resolveActivity(packageManager)?.packageName
        ?.takeIf { it == packageName }

    val homeIntent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .setPackage(packageName)
    val homeActivity = if (installed) {
        try {
            packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .firstOrNull { it.activityInfo?.packageName == packageName }
                ?.activityInfo
        } catch (_: SecurityException) {
            null
        }
    } else {
        null
    }

    val route = chooseLauncherOpenRoute(
        requestedPackage = packageName,
        isInstalled = installed,
        launchIntentPackage = launchActivityPackage,
        homeActivityPackage = homeActivity?.packageName
    )
    val intent = when (route) {
        LauncherOpenRoute.APP_LAUNCHER -> launchIntent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        LauncherOpenRoute.HOME_ACTIVITY -> homeActivity?.let { activityInfo ->
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setComponent(ComponentName(packageName, activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        LauncherOpenRoute.UNAVAILABLE -> null
    }

    if (intent == null) {
        return LauncherOpenResult(
            opened = false,
            message = if (installed) {
                "$launcherName cannot be opened directly from ZINK. Press Home to use the active launcher, or open Android Settings → Apps → Default apps → Home app to select it."
            } else {
                "$launcherName is not installed or does not expose an activity that ZINK can open. Use its official source above, or choose it in Android Settings → Apps → Default apps → Home app after installation."
            }
        )
    }

    return try {
        context.startActivity(intent)
        LauncherOpenResult(opened = true)
    } catch (_: ActivityNotFoundException) {
        LauncherOpenResult(
            opened = false,
            message = "$launcherName could not be opened by Android. Press Home to use the active launcher, or choose it through Android Settings → Apps → Default apps → Home app."
        )
    } catch (_: SecurityException) {
        LauncherOpenResult(
            opened = false,
            message = "Android blocked opening $launcherName from ZINK. Press Home to use the active launcher, or choose it through Android Settings → Apps → Default apps → Home app."
        )
    }
}

internal fun openDefaultAppsSettings(context: Context): Boolean {
    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) == null) return false
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/**
 * Verified Application Methods for Android Launchers.
 */
enum class ApplyMethod {
    /** A launcher-specific, evidence-backed external action applies the pack. */
    DIRECT,
    /** The launcher exposes a user-selected icon-pack configuration flow. */
    MANUAL,
    /** Applying ZINK requires a different app or launcher-owned theme format. */
    ALTERNATIVE,
    /** Available evidence does not establish a supported ZINK application method. */
    UNVERIFIED
}

enum class VerificationStatus(val label: String) {
    DEVICE_TESTED("Device Tested"),
    OFFICIALLY_DOCUMENTED("Officially Documented"),
    INTEGRATION_EVIDENCE("Integration Evidence"),
    PACKAGE_VERIFIED("Package Verified"),
    UNVERIFIED("Unverified")
}

enum class ConfidenceLevel(val label: String) {
    VERIFIED_OFFICIAL_DOCS("High · Official source"),
    VERIFIED_SOURCE_CODE("High · Source code"),
    VERIFIED_INTENT_SPEC("Moderate · Integration evidence"),
    COMMUNITY_VERIFIED("Limited · Community evidence"),
    UNVERIFIED("Unverified");

    val shortLabel: String
        get() = when (this) {
            VERIFIED_OFFICIAL_DOCS -> "Official"
            VERIFIED_SOURCE_CODE -> "Source Code"
            VERIFIED_INTENT_SPEC -> "Integration Spec"
            COMMUNITY_VERIFIED -> "Community"
            UNVERIFIED -> "Unverified"
        }
}

enum class ListingAvailability(val label: String, val explanation: String) {
    LISTING_FOUND(
        "Google Play listing found",
        "The listing opened during this audit. Google Play still determines availability for the user's region and device."
    ),
    REGION_DEPENDENT(
        "Google Play availability varies by region",
        "The listing was visible in a Romanian Play view but returned 404 in the default/US view. Installation availability was not verified."
    ),
    DEVICE_RESTRICTED(
        "Google Play listing is device-restricted",
        "The listing is visible, but Google Play determines whether this device and system version are eligible."
    ),
    LISTING_MISMATCH(
        "No matching official Google Play listing verified",
        "The configured Play URL resolved to a different app and publisher. No matching official listing was verified."
    ),
    NO_OFFICIAL_LISTING(
        "No official Google Play listing found",
        "No official standalone Google Play listing was found during this audit."
    ),
    UNVERIFIED(
        "Installation availability unverified",
        "A current, matching installation source was not verified."
    )
}

/**
 * Evidence record for launcher identity and ZINK integration claims.
 */
data class VerificationSource(
    val url: String?,
    val sourceType: String,
    val dateChecked: String,
    val evidence: String,
    val confidence: ConfidenceLevel,
    val status: VerificationStatus = VerificationStatus.UNVERIFIED,
    val linkLabel: String? = url?.let { "OPEN VERIFICATION SOURCE" }
)

/**
 * Specification and metadata for a supported Android launcher.
 */
data class LauncherSpec(
    val id: String,
    val name: String,
    val developer: String,
    val packageNames: List<String>,
    val playStoreUrl: String?,
    val applyMethod: ApplyMethod,
    val tagline: String,
    val supportedVersions: String,
    val requirements: List<String>,
    val settingsPath: List<String>,
    val setupSteps: List<String>,
    val limitations: List<String>,
    val troubleshooting: List<String>,
    val alternativeMethod: String? = null,
    val verification: VerificationSource,
    val directApplyAction: String? = null,
    val directApplyExtras: Map<String, String> = emptyMap(),
    val githubReleaseUrl: String? = null,
    val deviceRestrictionWarning: String? = null,
    val unavailableMessage: String? = null,
    val officialWebsite: String? = null,
    val listingAvailability: ListingAvailability = ListingAvailability.UNVERIFIED
)

/**
 * User-facing actions shown in a launcher detail sheet for a given install state.
 */
data class LauncherActionPresentation(
    val openActionLabel: String?,
    val sourceActionLabel: String?,
    val sourceUrl: String?,
    val sourceIsPlayStore: Boolean,
    val deviceRestrictionWarning: String?,
    val availabilityMessage: String?
)

/**
 * Runtime status of a launcher on the active user device.
 */
data class LauncherRuntimeState(
    val spec: LauncherSpec,
    val isInstalled: Boolean,
    val isDefault: Boolean,
    val installedPackage: String?
) {
    val statusBadge: String
        get() = when {
            isDefault -> "Default Launcher"
            isInstalled -> "Installed"
            spec.id == "apex" -> "Delisted"
            spec.playStoreUrl != null && spec.listingAvailability == ListingAvailability.REGION_DEPENDENT -> "Varies by Region"
            spec.playStoreUrl != null && spec.listingAvailability == ListingAvailability.DEVICE_RESTRICTED -> "Device Restricted"
            spec.playStoreUrl != null -> "Play listing found"
            spec.githubReleaseUrl != null -> "Available from GitHub"
            else -> "System Application"
        }

    val availabilityLabel: String
        get() = when {
            isInstalled -> "Installed"
            spec.id == "apex" -> "Delisted"
            spec.listingAvailability == ListingAvailability.NO_OFFICIAL_LISTING && spec.githubReleaseUrl != null -> "GitHub Source"
            spec.listingAvailability == ListingAvailability.NO_OFFICIAL_LISTING && spec.id == "pixel" -> "Preinstalled"
            spec.listingAvailability == ListingAvailability.NO_OFFICIAL_LISTING -> "No Play Listing"
            spec.listingAvailability == ListingAvailability.DEVICE_RESTRICTED -> "Restricted"
            spec.listingAvailability == ListingAvailability.REGION_DEPENDENT -> "Region Dependent"
            else -> "Available"
        }

    val actionPresentation: LauncherActionPresentation
        get() {
            val sourceUrl = spec.playStoreUrl ?: spec.githubReleaseUrl
            val sourceActionLabel = when {
                spec.playStoreUrl != null && isInstalled -> "VIEW GOOGLE PLAY LISTING"
                spec.playStoreUrl != null && spec.listingAvailability == ListingAvailability.REGION_DEPENDENT ->
                    "CHECK GOOGLE PLAY AVAILABILITY"
                spec.playStoreUrl != null -> "OPEN GOOGLE PLAY LISTING"
                spec.githubReleaseUrl != null && isInstalled -> "VIEW OFFICIAL GITHUB SOURCE"
                spec.githubReleaseUrl != null -> "OPEN OFFICIAL GITHUB RELEASES"
                else -> null
            }
            val availabilityMessage = spec.unavailableMessage
                ?: spec.listingAvailability.explanation
            return LauncherActionPresentation(
                openActionLabel = if (isInstalled) "OPEN ${spec.name.uppercase()}" else null,
                sourceActionLabel = sourceActionLabel,
                sourceUrl = sourceUrl,
                sourceIsPlayStore = spec.playStoreUrl != null,
                deviceRestrictionWarning = spec.deviceRestrictionWarning,
                availabilityMessage = availabilityMessage
            )
        }

    val methodBadge: String
        get() = when (spec.applyMethod) {
            ApplyMethod.DIRECT -> "1-Tap Direct Apply"
            ApplyMethod.MANUAL -> "Manual Setup Required"
            ApplyMethod.ALTERNATIVE -> "Alternative Required"
            ApplyMethod.UNVERIFIED -> "Compatibility Unverified"
        }

    val confidenceLabel: String
        get() = spec.verification.confidence.shortLabel
}

/**
 * Current HOME package and the curated launcher integration states on this device.
 */
data class LauncherRuntimeSnapshot(
    val defaultHomePackage: String?,
    val launchers: List<LauncherRuntimeState>
)

/**
 * Central Maintainable Repository of Verified Launcher Compatibility Data.
 */
object LauncherRegistry {

    data class ConfidenceBreakdown(
        val counts: Map<ConfidenceLevel, Int>,
        val total: Int
    ) {
        fun summaryLabel(): String {
            if (total == 0) return "0 LAUNCHERS"
            val parts = counts.filter { it.value > 0 }
                .map { (level, count) -> "$count ${level.shortLabel.uppercase()}" }
            return parts.joinToString(" · ")
        }
    }

    fun getConfidenceBreakdown(launchers: List<LauncherRuntimeState>): ConfidenceBreakdown {
        val counts = linkedMapOf<ConfidenceLevel, Int>()
        ConfidenceLevel.entries.forEach { level ->
            counts[level] = launchers.count { it.spec.verification.confidence == level }
        }
        return ConfidenceBreakdown(counts, launchers.size)
    }

    val launchers: List<LauncherSpec> = listOf(
        // ── 1. Nova Launcher ──
        LauncherSpec(
            id = "nova",
            name = "Nova Launcher",
            developer = "Instabridge Sweden AB",
            packageNames = listOf("com.teslacoilsw.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.teslacoilsw.launcher",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Manual icon pack selection through Nova Settings",
            supportedVersions = "Nova Launcher v6.x, v7.x, v8.x+",
            requirements = listOf(
                "Nova Launcher installed on device",
                "ZINK installed and discoverable as a Nova-compatible icon theme"
            ),
            settingsPath = listOf("Open Nova Settings", "Look & Feel", "Icon Style", "Icon Theme", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Open Nova Settings.",
                "Navigate to Look & Feel.",
                "Open Icon Style.",
                "Select Icon Theme.",
                "Choose ZINK."
            ),
            limitations = listOf(
                "Nova's APPLY_ICON_THEME confirmation action is not documented as a supported third-party apply API; ZINK cannot confirm that the selection was applied.",
                "Individual custom icons chosen per-app take precedence over global theme changes.",
                "Adaptive icon reshaping may apply if 'Reshape legacy icons' is enabled in Nova Settings."
            ),
            troubleshooting = listOf(
                "If ZINK is not listed, confirm the ZINK app is installed, then reopen Nova Settings → Look & Feel → Icon Style → Icon Theme.",
                "To restore standard icons, repeat these steps and choose 'System' under Icon Theme."
            ),
            verification = VerificationSource(
                url = "https://github.com/teslacoil/Example_NovaTheme/blob/master/README.md",
                sourceType = "Official Nova Launcher theme developer documentation",
                dateChecked = "2026-11-22",
                evidence = "Nova's official theme example documents com.novalauncher.THEME discovery and selecting the pack in Nova Settings > Look & Feel > Icon Theme. It does not document APPLY_ICON_THEME as a public application API.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN NOVA THEME DEVELOPER EXAMPLE"
            ),
            listingAvailability = ListingAvailability.LISTING_FOUND
        ),

        // ── 2. Niagara Launcher ──
        LauncherSpec(
            id = "niagara",
            name = "Niagara Launcher",
            developer = "Mellowdrop Studio",
            packageNames = listOf("bitpit.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=bitpit.launcher",
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Official 1-tap apply API and list-based minimal UI theming",
            supportedVersions = "Niagara Launcher v1.5+",
            requirements = listOf(
                "Niagara Launcher installed",
                "Appfilter XML declaration for launcher theme discovery"
            ),
            settingsPath = listOf("Niagara Settings (Scroll to bottom or swipe up)", "Look & feel", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Tap 'Apply Icon Pack' in ZINK to invoke Niagara's official APPLY_ICONS intent.",
                "Confirm the icon pack selection when prompted by Niagara.",
                "Alternatively, swipe up or scroll to the bottom of Niagara's app list to open Niagara Settings.",
                "Tap 'Look & feel' → 'Icon pack' and select 'ZINK'.",
                "The minimalist vertical list will refresh with ZINK's monochrome iconography."
            ),
            limitations = listOf(
                "Niagara automatically detects icon shapes and fits them into its unified list design.",
                "Any custom icons assigned individually will override the global pack."
            ),
            troubleshooting = listOf(
                "If an icon appears unthemed, tap and hold the app in Niagara's list, tap 'Edit icon', and select ZINK.",
                "Niagara supports theming its internal buttons (Settings, Pro, Recently Installed) via appfilter.xml."
            ),
            verification = VerificationSource(
                url = "https://help.niagaralauncher.app/article/24-resources-for-icon-developers",
                sourceType = "Official Niagara icon-pack developer guide",
                dateChecked = "2026-11-22",
                evidence = "The developer guide documents bitpit.launcher.APPLY_ICONS with the packageName extra. This documents the integration method; ZINK application has not been device-tested in this audit.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN NIAGARA ICON-PACK GUIDE"
            ),
            listingAvailability = ListingAvailability.LISTING_FOUND,
            directApplyAction = "bitpit.launcher.APPLY_ICONS",
            directApplyExtras = mapOf("packageName" to "{PACKAGE}")
        ),

        // ── 3. Smart Launcher 6 ──
        LauncherSpec(
            id = "smart",
            name = "Smart Launcher 6",
            developer = "Smart Launcher Team",
            packageNames = listOf("ginlemon.flowerfree"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=ginlemon.flowerfree",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Select ZINK through Smart Launcher icon settings",
            supportedVersions = "Smart Launcher v5, v6+",
            requirements = listOf(
                "Smart Launcher 6 installed",
                "Android 7.0+"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Preferences", "Global appearance", "Icon appearance", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Long-press the home screen and choose Preferences.",
                "Go to 'Global appearance' → 'Icon appearance' → 'Icon pack'.",
                "Choose 'ZINK' from the list of detected packs."
            ),
            limitations = listOf(
                "Smart Launcher can apply icon masks to unthemed icons according to its own shape settings.",
                "Category tab icons can be customized separately under Smart Launcher preferences."
            ),
            troubleshooting = listOf(
                "To reset individual app icon customizations, open Smart Launcher preferences → Icons → Reset custom icons."
            ),
            verification = VerificationSource(
                url = "https://www.smartlauncher.net/blog/what-are-icon-packs",
                sourceType = "Official Smart Launcher icon-pack guide",
                dateChecked = "2026-11-22",
                evidence = "The developer guide describes selecting an icon pack through Global Appearance > Icon appearance. It does not establish a public direct-apply intent.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN SMART LAUNCHER ICON-PACK GUIDE"
            ),
            listingAvailability = ListingAvailability.LISTING_FOUND
        ),

        // ── 4. Action Launcher ──
        LauncherSpec(
            id = "action",
            name = "Action Launcher",
            developer = "Action Launcher",
            packageNames = listOf("com.actionlauncher.playstore"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.actionlauncher.playstore",
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Direct apply via Quicktheme engine & Quickdrawer integration",
            supportedVersions = "Action Launcher v40+",
            requirements = listOf(
                "Action Launcher installed"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Action Settings", "Appearance", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Tap 'Apply Icon Pack' to trigger Action Launcher's direct apply broadcast.",
                "Or manually long-press an empty home screen space and select 'Action Settings'.",
                "Tap 'Appearance' → 'Icon pack'.",
                "Select 'ZINK' to theme your home screen, Quickdrawer, and Covers."
            ),
            limitations = listOf(
                "Action Launcher's Quicktheme material palette may adjust accent colors alongside icon themes."
            ),
            troubleshooting = listOf(
                "If icons do not update immediately, toggle Quicktheme or restart the launcher from Action Settings → Advanced → Restart."
            ),
            verification = VerificationSource(
                url = "https://actionlauncher.com/release-notes",
                sourceType = "Official Action Launcher release notes; direct apply device report",
                dateChecked = "2026-11-22",
                evidence = "Release notes establish icon-pack support. The ZINK developer previously reported successful direct apply on a Xiaomi device; exact app build, launcher version, and test date were not recorded, and this audit did not repeat that device test.",
                confidence = ConfidenceLevel.COMMUNITY_VERIFIED,
                status = VerificationStatus.DEVICE_TESTED,
                linkLabel = "OPEN ACTION LAUNCHER RELEASE NOTES"
            ),
            listingAvailability = ListingAvailability.LISTING_FOUND,
            directApplyAction = "com.actionlauncher.HONOR_ICON_PACK",
            directApplyExtras = mapOf("com.actionlauncher.extra.ICON_PACK" to "{PACKAGE}")
        ),

        // ── 5. ADW Launcher 2 ──
        LauncherSpec(
            id = "adw",
            name = "ADW Launcher 2",
            developer = "AnderWeb",
            packageNames = listOf("org.adw.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?hl=ro&id=org.adw.launcher",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Legacy ADW icon-pack support; listing visibility varies by region",
            supportedVersions = "ADW Launcher 1.x, 2.x",
            requirements = listOf(
                "ADW Launcher installed"
            ),
            settingsPath = listOf("ADW Settings", "Theme", "Customize", "Icons", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Tap 'Apply Icon Pack' to trigger the standard SET_THEME intent.",
                "Or open ADW Settings → 'Theme'.",
                "Choose 'ZINK' from the theme carousel and tap 'Apply'."
            ),
            limitations = listOf(
                "The linked Play view is region-dependent; the application’s current device and OS compatibility were not verified."
            ),
            troubleshooting = listOf(
                "If ZINK is not listed, the installed ADW build may not expose a compatible theme picker."
            ),
            verification = VerificationSource(
                url = "https://www.adwthings.com/launcher/",
                sourceType = "Official ADW developer website",
                dateChecked = "2026-11-22",
                evidence = "The developer website identifies ADW Launcher 2 and describes themes/custom icons. A Play listing view was visible in Romania while the default/US view returned 404; this does not establish global removal or installation eligibility.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "VISIT ADW DEVELOPER WEBSITE"
            ),
            officialWebsite = "https://www.adwthings.com/launcher/",
            listingAvailability = ListingAvailability.REGION_DEPENDENT
        ),

        // ── 6. Original Apex Launcher ──
        LauncherSpec(
            id = "apex",
            name = "Apex Launcher",
            developer = "Android Does",
            packageNames = listOf("com.anddoes.launcher"),
            playStoreUrl = null,
            applyMethod = ApplyMethod.UNVERIFIED,
            tagline = "Original Apex Launcher by Android Does; Google Play listing delisted",
            supportedVersions = "Apex Launcher v4.x+",
            requirements = listOf(
                "Original Apex Launcher (com.anddoes.launcher) installed"
            ),
            settingsPath = listOf("Apex Settings", "Theme settings", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Open Apex Settings → 'Theme settings'.",
                "Select 'ZINK' and check the 'Icon pack' box, then tap 'Apply'."
            ),
            limitations = listOf(
                "The original Apex Launcher (com.anddoes.launcher) is no longer available on Google Play Store.",
                "Icon pack compatibility and direct apply behavior are unverified."
            ),
            troubleshooting = listOf(
                "Use a launcher with documented ZINK support if the installed Apex version does not expose an icon-pack selector."
            ),
            verification = VerificationSource(
                url = "https://www.apexlauncher.com/",
                sourceType = "Official Apex Launcher Website",
                dateChecked = "2026-11-22",
                evidence = "The official website belongs to the original Apex Launcher (com.anddoes.launcher) by Android Does / Amber Mobile Limited. The Google Play Store listing is no longer available (delisted).",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "VISIT OFFICIAL APEX WEBSITE"
            ),
            officialWebsite = "https://www.apexlauncher.com/",
            listingAvailability = ListingAvailability.NO_OFFICIAL_LISTING
        ),

        // ── 7. AirBorne ApexLauncher ──
        LauncherSpec(
            id = "apex_airborne",
            name = "ApexLauncher",
            developer = "AirBorne",
            packageNames = listOf("com.home.launcher.apexlauncher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.home.launcher.apexlauncher",
            applyMethod = ApplyMethod.UNVERIFIED,
            tagline = "ApexLauncher by AirBorne; standalone application",
            supportedVersions = "Current Google Play version",
            requirements = listOf(
                "AirBorne's ApexLauncher (com.home.launcher.apexlauncher) installed"
            ),
            settingsPath = listOf("ApexLauncher Settings", "Theme / Icon settings"),
            setupSteps = listOf(
                "If installed, open AirBorne's ApexLauncher settings.",
                "Check if an icon pack or theme selector is supported."
            ),
            limitations = listOf(
                "This is a separate application by AirBorne (com.home.launcher.apexlauncher), distinct from the original Apex Launcher.",
                "Official website and third-party ZINK icon-pack integration are unverified."
            ),
            troubleshooting = listOf(
                "If ZINK cannot be applied, use a launcher with documented ZINK icon-pack support."
            ),
            verification = VerificationSource(
                url = "https://play.google.com/store/apps/details?id=com.home.launcher.apexlauncher",
                sourceType = "Google Play Store Listing (AirBorne)",
                dateChecked = "2026-11-22",
                evidence = "Google Play listing for AirBorne's ApexLauncher (com.home.launcher.apexlauncher). Official website, developer documentation, and ZINK icon-pack integration are unverified.",
                confidence = ConfidenceLevel.UNVERIFIED,
                status = VerificationStatus.PACKAGE_VERIFIED,
                linkLabel = "OPEN AIRBORNE GOOGLE PLAY LISTING"
            ),
            officialWebsite = null,
            listingAvailability = ListingAvailability.LISTING_FOUND
        ),

        // ── 7. Nothing Launcher (Nothing OS) ──
        LauncherSpec(
            id = "nothing",
            name = "Nothing Launcher",
            developer = "NOTHING TECHNOLOGY LIMITED",
            packageNames = listOf("com.nothing.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.nothing.launcher",
            listingAvailability = ListingAvailability.DEVICE_RESTRICTED,
            deviceRestrictionWarning = "Device restricted – Requires Nothing OS 2.5.0 or later.",
            applyMethod = ApplyMethod.UNVERIFIED,
            tagline = "Nothing OS launcher; third-party ZINK pack support is unverified",
            supportedVersions = "Google Play listing requires Nothing OS 2.5.0 or later; device eligibility is controlled by Google Play",
            requirements = listOf(
                "Nothing OS 2.5.0 or later, subject to Google Play device eligibility",
                "Third-party ZINK icon-pack support is unverified"
            ),
            settingsPath = listOf("Home Screen (Long-press empty area)", "Customisation (or Personalisation)", "Icon pack", "Select ZINK"),
            setupSteps = listOf(
                "If installed, open Nothing Launcher settings and check whether an icon-pack option is available.",
                "Select ZINK only if it is explicitly listed; the official sources reviewed do not confirm a third-party icon-pack selector."
            ),
            limitations = listOf(
                "The Nothing OS 2.5.0+ Play restriction is not evidence of ZINK compatibility.",
                "ZINK has not verified a third-party icon-pack workflow for Nothing Launcher; discovery does not establish compatibility."
            ),
            troubleshooting = listOf(
                "If no ZINK option is available, use a launcher with documented third-party icon-pack support."
            ),
            verification = VerificationSource(
                url = "https://play.google.com/store/apps/details?id=com.nothing.launcher",
                sourceType = "Official Google Play listing; icon-pack compatibility not documented",
                dateChecked = "2026-11-22",
                evidence = "Google Play identifies the Nothing Launcher listing and its Nothing OS 2.5.0+ device restriction. The listing and official support source reviewed do not establish ZINK/appfilter compatibility.",
                confidence = ConfidenceLevel.UNVERIFIED,
                status = VerificationStatus.UNVERIFIED,
                linkLabel = "OPEN NOTHING LAUNCHER GOOGLE PLAY LISTING"
            )
        ),

        // ── 8. Lawnchair ──
        LauncherSpec(
            id = "lawnchair",
            name = "Lawnchair",
            developer = "Lawnchair Team",
            packageNames = listOf(
                "app.lawnchair.play",
                "app.lawnchair",
                "app.lawnchair.nightly",
                "ch.deletescape.lawnchair.ci",
                "ch.deletescape.lawnchair.plah",
                "ch.deletescape.lawnchair"
            ),
            playStoreUrl = "https://play.google.com/store/apps/details?id=app.lawnchair.play",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Open-source Pixel-style launcher with comprehensive icon theming",
            supportedVersions = "Lawnchair v12, v14+",
            requirements = listOf(
                "Lawnchair v12 or higher installed"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Home Settings", "General", "Icon style", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Long-press an empty space on your Lawnchair home screen.",
                "Tap 'Home Settings' (or open the Lawnchair app icon).",
                "Navigate to 'General' → 'Icon style'.",
                "Tap 'Icon pack' and select 'ZINK'.",
                "Lawnchair applies the icon pack seamlessly to home and app drawer."
            ),
            limitations = listOf(
                "Lawnchair versions may differ; ZINK documents the stable manual path and makes no direct-apply claim."
            ),
            troubleshooting = listOf(
                "If icons do not update, go to Home Settings → General → Icon style → reselect ZINK.",
                "Individual app icons can be edited by long-pressing an app icon → tap the pencil/edit icon."
            ),
            verification = VerificationSource(
                url = "https://github.com/LawnchairLauncher/docs/blob/main/help/core-features/theming-and-icons.md",
                sourceType = "Official Lawnchair user guide",
                dateChecked = "2026-11-22",
                evidence = "The current user guide documents Home settings > General > Icon style for icon selection. This audit uses that manual path as the supported user flow; it does not claim the direct-apply proposal is available in every released build.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN LAWNCHAIR ICON GUIDE"
            ),
            listingAvailability = ListingAvailability.LISTING_FOUND
        ),

        // ── 9. Hyperion Launcher ──
        LauncherSpec(
            id = "hyperion",
            name = "Hyperion Launcher",
            developer = "prjkt.io (Substratum Team)",
            packageNames = listOf("projekt.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=projekt.launcher",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Modern launcher with dedicated Iconography customization",
            supportedVersions = "Hyperion Launcher v1.x+",
            requirements = listOf(
                "Hyperion Launcher installed"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Home Settings", "Iconography", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Long-press an empty spot on the home screen.",
                "Select 'Home Settings' to enter Hyperion's settings.",
                "Tap 'Iconography' (or 'Interface' depending on version).",
                "Tap 'Icon pack' and pick 'ZINK' from the list.",
                "Return to your home screen to view your refined icons."
            ),
            limitations = listOf(
                "Hyperion does not expose a public 1-click external broadcast to apply icon packs automatically."
            ),
            troubleshooting = listOf(
                "If an icon looks stretched, check Hyperion Iconography settings for 'Icon shape' adjustments."
            ),
            verification = VerificationSource(
                url = "https://play.google.com/store/apps/details?id=projekt.launcher",
                sourceType = "Official Google Play listing",
                dateChecked = "2026-11-22",
                evidence = "The listing advertises icon-pack support. The specific settings route shown is a user guide and was not independently confirmed against a current launcher build; no direct-apply API is claimed.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN HYPERION GOOGLE PLAY LISTING"
            ),
            listingAvailability = ListingAvailability.LISTING_FOUND
        ),

        // ── 10. Microsoft Launcher ──
        LauncherSpec(
            id = "microsoft",
            name = "Microsoft Launcher",
            developer = "Microsoft Corporation",
            packageNames = listOf("com.microsoft.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.microsoft.launcher",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Enterprise & personal launcher with built-in icon appearance options",
            supportedVersions = "Microsoft Launcher v6.x+",
            requirements = listOf(
                "Microsoft Launcher installed",
                "Android 7.0+"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Launcher Settings", "Home screen", "Icon appearance", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Long-press an empty area on your home screen and tap 'Launcher Settings'.",
                "Tap 'Home screen'.",
                "Tap 'Icon appearance' (or 'Icon pack').",
                "Scroll through the list and select 'ZINK'.",
                "The home screen and app drawer will apply ZINK's icons."
            ),
            limitations = listOf(
                "Microsoft Launcher does not provide a public external intent to apply packs programmatically.",
                "Some proprietary Microsoft system shortcuts may override custom iconography."
            ),
            troubleshooting = listOf(
                "To customize an individual icon: long-press the icon → tap the pencil/edit icon → tap the icon image → choose ZINK."
            ),
            verification = VerificationSource(
                url = "https://play.google.com/store/apps/details?id=com.microsoft.launcher",
                sourceType = "Official Microsoft Launcher Google Play listing",
                dateChecked = "2026-11-22",
                evidence = "The listing states that Microsoft Launcher supports custom icon packs. The precise menu labels vary by version and are not independently documented by the linked listing.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN MICROSOFT LAUNCHER GOOGLE PLAY LISTING"
            ),
            listingAvailability = ListingAvailability.LISTING_FOUND
        ),

        // ── 11. Kvaesitso ──
        LauncherSpec(
            id = "kvaesitso",
            name = "Kvaesitso",
            developer = "MM20",
            packageNames = listOf("de.mm20.launcher2.release"),
            playStoreUrl = null,
            githubReleaseUrl = "https://github.com/MM2-0/Kvaesitso/releases",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "FOSS search-focused launcher with global icon pack theming",
            supportedVersions = "Kvaesitso v1.x+",
            requirements = listOf(
                "Kvaesitso installed",
                "Android 8.0+"
            ),
            settingsPath = listOf("Search Page (Swipe up)", "Settings (Gear Icon)", "Appearance", "Icons", "Icon pack", "Select ZINK as the icon pack."),
            setupSteps = listOf(
                "Swipe up to open Kvaesitso's search / app page.",
                "Tap the gear icon to open Kvaesitso Settings.",
                "Navigate to 'Appearance' → 'Icons'.",
                "Tap 'Icon pack' and select 'ZINK'.",
                "Both search results and favorite app tiles will use ZINK."
            ),
            limitations = listOf(
                "No external broadcast intent for 1-tap apply."
            ),
            troubleshooting = listOf(
                "Re-index apps in Kvaesitso if search icons don't refresh immediately."
            ),
            verification = VerificationSource(
                url = "https://github.com/MM2-0/Kvaesitso/blob/main/docs/docs/developer-guide/integrations/icon-packs.md",
                sourceType = "Official Kvaesitso icon-pack developer guide",
                dateChecked = "2026-11-22",
                evidence = "The official guide documents the ADW icon-pack format, which ZINK declares. It establishes format integration, not a real-device ZINK apply test.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN KVAESITSO ICON-PACK GUIDE"
            ),
            listingAvailability = ListingAvailability.NO_OFFICIAL_LISTING
        ),

        // ── 12. Samsung One UI Home ──
        LauncherSpec(
            id = "oneui",
            name = "Samsung One UI Home",
            developer = "Samsung Electronics Co., Ltd.",
            packageNames = listOf("com.sec.android.app.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.sec.android.app.launcher",
            listingAvailability = ListingAvailability.DEVICE_RESTRICTED,
            deviceRestrictionWarning = "Device restricted – Designed for Samsung Galaxy devices.",
            applyMethod = ApplyMethod.ALTERNATIVE,
            tagline = "Samsung theme alternative; ZINK icon-pack import is unverified",
            supportedVersions = "One UI 3.0, 4.x, 5.x, 6.x+",
            requirements = listOf(
                "Samsung Galaxy phone running One UI",
                "Galaxy Themes/Theme Park availability and importing ZINK are device- and region-dependent; ZINK import is unverified"
            ),
            settingsPath = listOf("Galaxy Themes", "Choose an available Samsung icon set", "Use a third-party launcher with documented ZINK support to apply ZINK"),
            setupSteps = listOf(
                "Samsung documents applying icon sets through Galaxy Themes; this does not establish importing ZINK's separately installed Android icon-pack APK.",
                "ZINK has not verified that Theme Park imports this installed icon pack.",
                "Use a third-party launcher with documented ZINK icon-pack support to apply ZINK."
            ),
            limitations = listOf(
                "Samsung's documented Galaxy Themes flow does not confirm that Theme Park can import a separately installed icon-pack APK such as ZINK."
            ),
            troubleshooting = listOf(
                "Galaxy Themes menu labels and available content vary by device, software version, and region."
            ),
            verification = VerificationSource(
                url = "https://www.samsung.com/us/support/answer/ANS10001905/",
                sourceType = "Official Samsung theming support",
                dateChecked = "2026-11-22",
                evidence = "Samsung documents Galaxy Themes icon sets and notes that menus vary by model/software. It does not confirm importing ZINK's separately installed Android icon-pack APK through Theme Park.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN SAMSUNG THEMING SUPPORT"
            )
        ),

        // ── Xiaomi HyperOS System Launcher ──
        LauncherSpec(
            id = "xiaomi_system",
            name = "Xiaomi HyperOS System Launcher",
            developer = "Xiaomi",
            packageNames = listOf("com.miui.home"),
            playStoreUrl = null,
            applyMethod = ApplyMethod.ALTERNATIVE,
            tagline = "No verified standard ZINK icon-pack integration",
            supportedVersions = "Xiaomi MIUI / HyperOS system launcher",
            requirements = listOf("Xiaomi System Launcher selected as the device HOME app"),
            settingsPath = listOf("Use a compatible third-party launcher", "Set it as the default HOME app", "Select ZINK in that launcher's icon settings"),
            setupSteps = listOf(
                "ZINK detects the Xiaomi System Launcher, but has no verified standard icon-pack integration for it.",
                "Install a launcher listed by ZINK as supporting icon packs, such as Nova, Niagara, Smart Launcher, or Lawnchair.",
                "Set that launcher as the default HOME app in Android Settings.",
                "Open the selected launcher's icon settings and choose ZINK."
            ),
            limitations = listOf(
                "ZINK's standard appfilter.xml pack is not a Xiaomi Themes .mtz theme and cannot be applied directly through the stock HyperOS launcher.",
                "Xiaomi Themes may offer theme-provided icons; those are separate from ZINK's launcher icon-pack integration."
            ),
            troubleshooting = listOf(
                "If the HOME screen still uses Xiaomi System Launcher, ZINK icons will not be applied by this integration."
            ),
            unavailableMessage = "No verified ZINK icon-pack integration for Xiaomi System Launcher. Use a compatible third-party launcher to apply ZINK.",
            verification = VerificationSource(
                url = null,
                sourceType = "PackageManager HOME query on developer Xiaomi device (historical; exact date not recorded)",
                dateChecked = "Not recorded",
                evidence = "A prior PackageManager check identified com.miui.home/.launcher.Launcher as the selected HOME activity. This verifies package identity on that device only; it does not verify ZINK icon-pack compatibility.",
                confidence = ConfidenceLevel.UNVERIFIED,
                status = VerificationStatus.PACKAGE_VERIFIED,
                linkLabel = null
            )
        ),

        // ── 14. Google Pixel Launcher ──
        LauncherSpec(
            id = "pixel",
            name = "Google Pixel Launcher",
            developer = "Google LLC",
            packageNames = listOf("com.google.android.apps.nexuslauncher"),
            playStoreUrl = null,
            listingAvailability = ListingAvailability.NO_OFFICIAL_LISTING,
            deviceRestrictionWarning = "Device restricted – Preinstalled on supported Google Pixel devices. Not available as a standalone installation for other devices.",
            applyMethod = ApplyMethod.ALTERNATIVE,
            tagline = "No verified standard ZINK integration for Pixel Launcher",
            supportedVersions = "Android 12, 13, 14, 15",
            requirements = listOf(
                "Google Pixel device running stock Pixel Launcher; device-specific availability applies"
            ),
            settingsPath = listOf("Pixel Launcher (no verified standard ZINK pack integration)", "Install Lawnchair or Nova from Play Store", "Set as Default Home App", "Select ZINK"),
            setupSteps = listOf(
                "Google documents Pixel's built-in Themed icons feature for supported app icons.",
                "The reviewed Google documentation does not establish whether a standard third-party appfilter pack such as ZINK can be applied.",
                "To enjoy ZINK on a Google Pixel device, install a Pixel-like customizable launcher such as 'Lawnchair' or 'Nova Launcher'.",
                "Set the new launcher as your Default Home App under Android Settings → Apps → Default apps → Home app.",
                "Apply ZINK directly within the new launcher."
            ),
            limitations = listOf(
                "ZINK has not verified a standard icon-pack integration for Pixel Launcher; this is not proof that no device/version-specific method exists."
            ),
            troubleshooting = listOf(
                "A third-party launcher with documented appfilter support may be used as an alternative."
            ),
            verification = VerificationSource(
                url = "https://support.google.com/pixelphone/answer/6111557?hl=en",
                sourceType = "Official Google Pixel themed-icons help",
                dateChecked = "2026-11-22",
                evidence = "Google documents the built-in Themed icons control for supported icons. The page does not address third-party appfilter pack integration.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS,
                status = VerificationStatus.OFFICIALLY_DOCUMENTED,
                linkLabel = "OPEN GOOGLE PIXEL THEMED-ICONS HELP"
            )
        )
    )

    /**
     * Formats the verified intent action and parameters into a clean, reproducible code string.
     */
    fun formatIntentDetails(context: Context, spec: LauncherSpec): String {
        val appPackage = context.packageName
        if (spec.directApplyAction == null) {
            return when (spec.applyMethod) {
                ApplyMethod.MANUAL -> """
                    // No programmatic apply broadcast/intent exposed by this launcher.
                    // Discovery: AndroidManifest.xml (ADW & Nova theme filters)
                    // Application: In-app icon pack selector via Launcher Settings
                """.trimIndent()
                ApplyMethod.ALTERNATIVE -> """
                    // System launcher does not natively support third-party icon packs.
                    // Samsung One UI: Applied via Samsung Good Lock > Theme Park module.
                    // Google Pixel: Requires customizable launcher (e.g. Lawnchair, Nova).
                """.trimIndent()
                else -> "// Manual configuration required."
            }
        }

        val primaryPkg = spec.packageNames.firstOrNull() ?: ""
        val builder = StringBuilder()
        builder.append("Action:  ${spec.directApplyAction}\n")
        builder.append("Package: $primaryPkg\n")
        if (spec.directApplyExtras.isNotEmpty()) {
            builder.append("Extras:\n")
            spec.directApplyExtras.forEach { (key, value) ->
                val resolvedVal = if (value == "{PACKAGE}") appPackage else value
                builder.append("  • $key = \"$resolvedVal\"\n")
            }
        }
        return builder.toString().trimEnd()
    }

    /** A concrete activity returned by Android as a HOME handler. */
    internal data class HomeLauncherCandidate(
        val packageName: String,
        val activityName: String,
        val label: String
    )

    /**
     * Removes Android's setup fallback activity and duplicate HOME activities from the package
     * list. Resource overlay packages cannot appear here because they do not resolve CATEGORY_HOME.
     */
    internal fun normalizeHomeCandidates(candidates: List<HomeLauncherCandidate>): List<HomeLauncherCandidate> =
        candidates
            .filterNot {
                val activitySimpleName = it.activityName.substringAfterLast('.')
                activitySimpleName == "FallbackHome" ||
                    (activitySimpleName == "ResolverActivity" &&
                        it.packageName in setOf("android", "com.android.settings"))
            }
            .distinctBy { it.packageName }

    /** Only accept a selected component when it is one of the concrete HOME activities. */
    internal fun selectedHomePackage(
        defaultPackage: String?,
        defaultActivity: String?,
        candidates: List<HomeLauncherCandidate>
    ): String? = defaultPackage
        ?.takeIf { packageName ->
            candidates.any {
                it.packageName == packageName && it.activityName == defaultActivity
            }
        }

    internal fun unregisteredHomeSpec(candidate: HomeLauncherCandidate): LauncherSpec = LauncherSpec(
        id = "installed_${candidate.packageName.replace('.', '_')}",
        name = candidate.label.ifBlank { candidate.packageName },
        developer = "Installed HOME app",
        packageNames = listOf(candidate.packageName),
        playStoreUrl = null,
        applyMethod = ApplyMethod.ALTERNATIVE,
        tagline = "Installed HOME app; ZINK icon-pack compatibility is unverified",
        supportedVersions = "Current device version",
        requirements = listOf("This app is available as a HOME activity on this device"),
        settingsPath = listOf("Open the launcher settings", "Look for icon or theme settings"),
        setupSteps = listOf(
            "${candidate.label} is installed and can handle the HOME role.",
            "ZINK has no verified icon-pack integration for this launcher.",
            "Use a launcher with a listed ZINK integration to apply the icon pack."
        ),
        limitations = listOf("Compatibility is unverified; ZINK does not offer direct apply for this launcher."),
        troubleshooting = emptyList(),
        unavailableMessage = "No verified ZINK icon-pack integration is available for ${candidate.label}.",
        verification = VerificationSource(
            url = null,
            sourceType = "Runtime PackageManager HOME query",
            dateChecked = "Current session only",
            evidence = "PackageManager returned this package as an Android HOME activity on the current device. Discovery does not establish ZINK icon-pack compatibility.",
            confidence = ConfidenceLevel.UNVERIFIED,
            status = VerificationStatus.PACKAGE_VERIFIED,
            linkLabel = null
        )
    )

    /**
     * Resolves the user's selected HOME app and enumerates every HOME-capable application.
     * Android's chooser/fallback is not treated as the selected launcher.
     */
    private fun queryHomeLaunchers(context: Context): Pair<String?, List<HomeLauncherCandidate>> {
        val packageManager = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = packageManager.queryIntentActivities(homeIntent, 0)
        val resolvedActivity = packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo
        val candidates = normalizeHomeCandidates(resolveInfos.mapNotNull { resolveInfo ->
            val activity = resolveInfo.activityInfo ?: return@mapNotNull null
            HomeLauncherCandidate(
                packageName = activity.packageName,
                activityName = activity.name,
                label = runCatching {
                    packageManager.getApplicationLabel(activity.applicationInfo).toString()
                }.getOrElse { resolveInfo.loadLabel(packageManager).toString() }
                    .ifBlank { activity.packageName }
            )
        })
        return selectedHomePackage(
            resolvedActivity?.packageName,
            resolvedActivity?.name,
            candidates
        ) to candidates
    }

    fun detectDefaultLauncherPackage(context: Context): String? = queryHomeLaunchers(context).first

    fun getApplicationLabel(context: Context, packageName: String): String =
        runCatching {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(packageName)

    /**
     * Builds runtime launcher records from actual HOME activities, then adds curated compatibility
     * metadata where a package is known. Installed state, default selection and apply method remain
     * independent properties.
     */
    fun getRuntimeSnapshot(context: Context): LauncherRuntimeSnapshot {
        val (defaultPackage, homeCandidates) = queryHomeLaunchers(context)
        val homePackages = homeCandidates.mapTo(mutableSetOf()) { it.packageName }
        val knownPackages = launchers.flatMapTo(mutableSetOf()) { it.packageNames }
        val registeredStates = launchers.map { spec ->
            val installedPackage = spec.packageNames.firstOrNull(homePackages::contains)
            LauncherRuntimeState(
                spec = spec,
                isInstalled = installedPackage != null,
                isDefault = installedPackage != null && installedPackage == defaultPackage,
                installedPackage = installedPackage
            )
        }
        val discoveredStates = homeCandidates
            .filterNot { it.packageName in knownPackages }
            .map { candidate ->
                LauncherRuntimeState(
                    spec = unregisteredHomeSpec(candidate),
                    isInstalled = true,
                    isDefault = candidate.packageName == defaultPackage,
                    installedPackage = candidate.packageName
                )
            }
        val launcherStates = (registeredStates + discoveredStates).sortedWith(
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
                .thenBy { it.spec.name }
        )
        return LauncherRuntimeSnapshot(defaultPackage, launcherStates)
    }

    fun getLauncherStates(context: Context): List<LauncherRuntimeState> =
        getRuntimeSnapshot(context).launchers

    /**
     * Dispatches the configured apply request for a direct-integration launcher.
     * Returns true when Android starts the intent; it does not mean the launcher confirmed the pack.
     */
    fun executeDirectApply(context: Context, state: LauncherRuntimeState): Boolean {
        val spec = state.spec
        val pkg = state.installedPackage ?: spec.packageNames.firstOrNull() ?: return false
        val action = spec.directApplyAction ?: return false

        try {
            val intent = Intent(action).apply {
                setPackage(pkg)
                spec.directApplyExtras.forEach { (key, value) ->
                    val resolvedValue = if (value == "{PACKAGE}") context.packageName else value
                    putExtra(key, resolvedValue)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(context, directApplyFeedbackMessage(spec.name), Toast.LENGTH_SHORT).show()
            return true
        } catch (_: ActivityNotFoundException) {
            // Fallback: try opening the launcher app
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    Toast.makeText(context, "Please select ZINK in ${spec.name} settings", Toast.LENGTH_LONG).show()
                    return true
                }
            } catch (_: Exception) { }
            return false
        } catch (_: Exception) {
            return false
        }
    }
}
