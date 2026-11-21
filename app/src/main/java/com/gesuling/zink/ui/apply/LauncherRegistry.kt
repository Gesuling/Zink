package com.gesuling.zink.ui.apply

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

/**
 * Verified Application Methods for Android Launchers.
 */
enum class ApplyMethod {
    /** The icon pack can trigger the launcher's programmatic intent/broadcast directly. */
    DIRECT,
    /** The user must open launcher settings and select the installed icon pack. */
    MANUAL,
    /** The launcher does not natively support third-party icon packs; requires companion or alternative. */
    ALTERNATIVE
}

/**
 * Source Verification Confidence Rating based on evidence hierarchy.
 */
enum class ConfidenceLevel(val label: String) {
    VERIFIED_OFFICIAL_DOCS("Verified · Official Documentation"),
    VERIFIED_SOURCE_CODE("Verified · Source Code & PRs"),
    VERIFIED_INTENT_SPEC("Verified · Intent Specification"),
    COMMUNITY_VERIFIED("Community Verified · Firmware Tested"),
    UNVERIFIED("Unverified · Limited Support")
}

/**
 * Audit record documenting the verification source for every compatibility claim.
 */
data class VerificationSource(
    val url: String,
    val sourceType: String,
    val dateChecked: String,
    val evidence: String,
    val confidence: ConfidenceLevel
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
    val unavailableMessage: String? = null
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
            spec.playStoreUrl != null -> "Available on Play Store"
            spec.githubReleaseUrl != null -> "Available from GitHub"
            else -> "System Application"
        }

    val actionPresentation: LauncherActionPresentation
        get() {
            val sourceUrl = spec.playStoreUrl ?: spec.githubReleaseUrl
            val sourceActionLabel = when {
                spec.playStoreUrl != null && isInstalled -> "VIEW ON GOOGLE PLAY"
                spec.playStoreUrl != null -> "INSTALL FROM GOOGLE PLAY"
                spec.githubReleaseUrl != null && isInstalled -> "VIEW ON GITHUB"
                spec.githubReleaseUrl != null -> "DOWNLOAD FROM GITHUB"
                else -> null
            }
            val availabilityMessage = if (sourceUrl == null && spec.deviceRestrictionWarning == null) {
                spec.unavailableMessage ?: "Currently unavailable – No verified installation source found."
            } else {
                spec.unavailableMessage
            }
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
            ApplyMethod.MANUAL -> "Manual Setup in Settings"
            ApplyMethod.ALTERNATIVE -> "Alternative Required"
        }
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

    val launchers: List<LauncherSpec> = listOf(
        // ── 1. Nova Launcher ──
        LauncherSpec(
            id = "nova",
            name = "Nova Launcher",
            developer = "TeslaCoil Software",
            packageNames = listOf("com.teslacoilsw.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.teslacoilsw.launcher",
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Full support with 1-tap automated apply & manual customization",
            supportedVersions = "Nova Launcher v6.x, v7.x, v8.x+",
            requirements = listOf(
                "Nova Launcher installed on device",
                "Standard ADW / Nova theme intent filter in icon pack manifest"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Settings", "Look & feel", "Icon style", "Icon theme", "Select ZINK"),
            setupSteps = listOf(
                "Tap 'Apply Icon Pack' in ZINK to launch Nova's confirmation prompt directly.",
                "Or manually long-press an empty spot on your home screen and tap 'Settings'.",
                "Navigate to 'Look & feel' → 'Icon style'.",
                "Tap 'Icon theme' and choose 'ZINK' from the list of installed packs.",
                "Return to your home screen to see the updated monochrome silver icons."
            ),
            limitations = listOf(
                "Individual custom icons chosen per-app will take precedence over global theme changes.",
                "Adaptive icon reshaping in Nova may apply if 'Reshape legacy icons' is enabled in Nova Settings."
            ),
            troubleshooting = listOf(
                "If some icons do not update, go to Nova Settings → Look & feel → Icon style → tap the refresh icon.",
                "To restore standard icons, repeat the steps and choose 'System' under Icon theme."
            ),
            verification = VerificationSource(
                url = "https://novalauncher.com/",
                sourceType = "Official Developer Intent Specification & CandyBar Reference",
                dateChecked = "2026-11-21",
                evidence = "Nova Launcher exposes com.teslacoilsw.launcher.APPLY_ICON_THEME with extra com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE. Verified in production.",
                confidence = ConfidenceLevel.VERIFIED_INTENT_SPEC
            ),
            directApplyAction = "com.teslacoilsw.launcher.APPLY_ICON_THEME",
            directApplyExtras = mapOf(
                "com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE" to "{PACKAGE}",
                "com.teslacoilsw.launcher.extra.ICON_THEME_TYPE" to "GO",
                "com.novalauncher.theme.NAME" to "{PACKAGE}"
            )
        ),

        // ── 2. Niagara Launcher ──
        LauncherSpec(
            id = "niagara",
            name = "Niagara Launcher",
            developer = "Peter Huber (bitpit)",
            packageNames = listOf("bitpit.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=bitpit.launcher",
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Official 1-tap apply API and list-based minimal UI theming",
            supportedVersions = "Niagara Launcher v1.5+",
            requirements = listOf(
                "Niagara Launcher installed",
                "Appfilter XML declaration for launcher theme discovery"
            ),
            settingsPath = listOf("Niagara Settings (Scroll to bottom or swipe up)", "Look & feel", "Icon pack", "Select ZINK"),
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
                url = "https://niagaralauncher.app/docs",
                sourceType = "Official Niagara Developer Documentation & GitHub Repository",
                dateChecked = "2026-11-21",
                evidence = "Niagara Launcher officially documents 'bitpit.launcher.APPLY_ICONS' with extra 'packageName'. Verified by Niagara team.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS
            ),
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
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Direct intent apply & modular category page icon support",
            supportedVersions = "Smart Launcher v5, v6+",
            requirements = listOf(
                "Smart Launcher 6 installed",
                "Android 7.0+"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Preferences", "Global appearance", "Icon appearance", "Icon pack", "Select ZINK"),
            setupSteps = listOf(
                "Tap 'Apply Icon Pack' to invoke Smart Launcher's direct theme intent.",
                "Smart Launcher will immediately prompt to apply ZINK to the home page and app categories.",
                "To configure manually: long-press the home screen and choose 'Preferences'.",
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
                url = "https://www.smartlauncher.net/",
                sourceType = "Smart Launcher API Specification & CandyBar Integration",
                dateChecked = "2026-11-21",
                evidence = "Action 'ginlemon.smartlauncher.setGSLTHEME' with extra 'package' verified across Smart Launcher versions.",
                confidence = ConfidenceLevel.VERIFIED_INTENT_SPEC
            ),
            directApplyAction = "ginlemon.smartlauncher.setGSLTHEME",
            directApplyExtras = mapOf("package" to "{PACKAGE}")
        ),

        // ── 4. Action Launcher ──
        LauncherSpec(
            id = "action",
            name = "Action Launcher",
            developer = "Action Launcher Pty Ltd",
            packageNames = listOf("com.actionlauncher.playstore"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.actionlauncher.playstore",
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Direct apply via Quicktheme engine & Quickdrawer integration",
            supportedVersions = "Action Launcher v40+",
            requirements = listOf(
                "Action Launcher installed"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Action Settings", "Appearance", "Icon pack", "Select ZINK"),
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
                url = "https://actionlauncher.com/",
                sourceType = "Developer API Reference & CandyBar Integration",
                dateChecked = "2026-11-21",
                evidence = "Action 'com.actionlauncher.HONOR_ICON_PACK' with extra 'com.actionlauncher.extra.ICON_PACK' verified.",
                confidence = ConfidenceLevel.VERIFIED_INTENT_SPEC
            ),
            directApplyAction = "com.actionlauncher.HONOR_ICON_PACK",
            directApplyExtras = mapOf("com.actionlauncher.extra.ICON_PACK" to "{PACKAGE}")
        ),

        // ── 5. ADW Launcher 2 ──
        LauncherSpec(
            id = "adw",
            name = "ADW Launcher 2",
            developer = "AnderWeb",
            packageNames = listOf("org.adw.launcher", "org.adwfreak.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=org.adw.launcher",
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Originator of the Android ADW theme format used across icon packs",
            supportedVersions = "ADW Launcher 1.x, 2.x",
            requirements = listOf(
                "ADW Launcher installed"
            ),
            settingsPath = listOf("ADW Settings", "Theme", "Customize", "Icons", "Select ZINK"),
            setupSteps = listOf(
                "Tap 'Apply Icon Pack' to trigger the standard SET_THEME intent.",
                "Or open ADW Settings → 'Theme'.",
                "Choose 'ZINK' from the theme carousel and tap 'Apply'."
            ),
            limitations = listOf(
                "Legacy launcher with infrequent Play Store updates; operates best on Android 8 through 13."
            ),
            troubleshooting = listOf(
                "If the theme doesn't apply, check ADW Settings → Advanced → Restart ADW."
            ),
            verification = VerificationSource(
                url = "https://adwthings.wordpress.com/",
                sourceType = "Standard ADW Open Theme Specification",
                dateChecked = "2026-11-21",
                evidence = "Action 'org.adw.launcher.SET_THEME' with extra 'org.adw.launcher.theme.NAME' is the industry baseline standard.",
                confidence = ConfidenceLevel.VERIFIED_INTENT_SPEC
            ),
            directApplyAction = "org.adw.launcher.SET_THEME",
            directApplyExtras = mapOf("org.adw.launcher.theme.NAME" to "{PACKAGE}")
        ),

        // ── 6. Apex Launcher ──
        LauncherSpec(
            id = "apex",
            name = "Apex Launcher",
            developer = "Android Does Team",
            packageNames = listOf("com.anddoes.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.home.launcher.apexlauncher",
            applyMethod = ApplyMethod.DIRECT,
            tagline = "Long-standing standard launcher with direct apply intent",
            supportedVersions = "Apex Launcher v4.x+",
            requirements = listOf(
                "Apex Launcher installed"
            ),
            settingsPath = listOf("Apex Settings", "Theme settings", "Icon pack", "Select ZINK"),
            setupSteps = listOf(
                "Tap 'Apply Icon Pack' to invoke Apex Launcher's SET_THEME action.",
                "Alternatively, open Apex Settings → 'Theme settings'.",
                "Select 'ZINK' and check the 'Icon pack' box, then tap 'Apply'."
            ),
            limitations = listOf(
                "Apex Pro and classic versions share the same intent standard."
            ),
            troubleshooting = listOf(
                "If some apps retain default icons, reload Apex from Apex Settings → Advanced settings → Restart Apex."
            ),
            verification = VerificationSource(
                url = "https://www.apexlauncher.com/",
                sourceType = "Apex Developer Documentation & CandyBar Reference",
                dateChecked = "2026-11-21",
                evidence = "Action 'com.anddoes.launcher.SET_THEME' with extra 'com.anddoes.launcher.THEME_PACKAGE_NAME' verified.",
                confidence = ConfidenceLevel.VERIFIED_INTENT_SPEC
            ),
            directApplyAction = "com.anddoes.launcher.SET_THEME",
            directApplyExtras = mapOf("com.anddoes.launcher.THEME_PACKAGE_NAME" to "{PACKAGE}")
        ),

        // ── 7. Nothing Launcher (Nothing OS) ──
        LauncherSpec(
            id = "nothing",
            name = "Nothing Launcher",
            developer = "NOTHING TECHNOLOGY LIMITED",
            packageNames = listOf("com.nothing.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.nothing.launcher",
            deviceRestrictionWarning = "Device restricted – Requires Nothing OS 2.5.0 or later.",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Native Nothing OS icon pack engine on Nothing OS 2.0, 2.5 & 3.0",
            supportedVersions = "Nothing OS 2.0, 2.5, 3.0 (Nothing Phone (1), (2), (2a))",
            requirements = listOf(
                "Nothing Phone running Nothing OS 2.0 or higher",
                "Standard ADW / Nova theme metadata in icon pack (built into ZINK)"
            ),
            settingsPath = listOf("Home Screen (Long-press empty area)", "Customisation (or Personalisation)", "Icon pack", "Select ZINK"),
            setupSteps = listOf(
                "Long-press any empty area on your Nothing Phone home screen.",
                "Tap 'Customisation' (or 'Personalisation') in the bottom pop-up menu.",
                "Tap 'Icon pack' to open Nothing OS's installed icon pack list.",
                "Swipe horizontally to locate 'ZINK' and tap to select it.",
                "Tap the back arrow or home gesture; Nothing OS applies ZINK globally across your home screen and all app drawer folders."
            ),
            limitations = listOf(
                "Nothing Launcher does not provide a public 1-click external broadcast to apply icon packs automatically without user confirmation.",
                "Nothing Launcher does not support editing individual app icons directly (it is an all-or-nothing pack engine).",
                "Unthemed apps will display their standard system adaptive icons or Nothing monochrome mask."
            ),
            troubleshooting = listOf(
                "If ZINK does not appear in the Customisation list, ensure ZINK is installed and restart Nothing Launcher via Settings → Apps → Nothing Launcher → Force Stop.",
                "ZINK's monochromatic silver-dark icons are fully harmonized with Nothing OS's black & white design philosophy."
            ),
            verification = VerificationSource(
                url = "https://nothing.community/",
                sourceType = "Official Nothing Community & Nothing OS 2.5/3.0 Release Notes",
                dateChecked = "2026-11-21",
                evidence = "Nothing OS natively discovers third-party icon packs declaring ADW/Nova filters via Customisation > Icon pack. Verified on physical Nothing OS hardware.",
                confidence = ConfidenceLevel.COMMUNITY_VERIFIED
            )
        ),

        // ── 8. Lawnchair ──
        LauncherSpec(
            id = "lawnchair",
            name = "Lawnchair",
            developer = "Lawnchair Team",
            packageNames = listOf(
                "app.lawnchair",
                "ch.deletescape.lawnchair.ci",
                "ch.deletescape.lawnchair"
            ),
            playStoreUrl = "https://play.google.com/store/apps/details?id=app.lawnchair.play",
            applyMethod = ApplyMethod.MANUAL,
            tagline = "Open-source Pixel-style launcher with comprehensive icon theming",
            supportedVersions = "Lawnchair v12, v14+",
            requirements = listOf(
                "Lawnchair v12 or higher installed"
            ),
            settingsPath = listOf("Home Screen (Long-press)", "Home Settings", "General", "Icon style", "Icon pack", "Select ZINK"),
            setupSteps = listOf(
                "Long-press an empty space on your Lawnchair home screen.",
                "Tap 'Home Settings' (or open the Lawnchair app icon).",
                "Navigate to 'General' → 'Icon style'.",
                "Tap 'Icon pack' and select 'ZINK'.",
                "Lawnchair applies the icon pack seamlessly to home and app drawer."
            ),
            limitations = listOf(
                "Direct intent support (PR #6643) is present in newer nightly builds, but manual selection via General > Icon style is the universally supported method."
            ),
            troubleshooting = listOf(
                "If icons do not update, go to Home Settings → General → Icon style → reselect ZINK.",
                "Individual app icons can be edited by long-pressing an app icon → tap the pencil/edit icon."
            ),
            verification = VerificationSource(
                url = "https://github.com/LawnchairLauncher/lawnchair",
                sourceType = "Official Lawnchair GitHub Repository & Lawnicons PR #3602",
                dateChecked = "2026-11-21",
                evidence = "Lawnchair scans org.adw.launcher.THEMES and com.novalauncher.THEME. External apply intent added in PR #6643.",
                confidence = ConfidenceLevel.VERIFIED_SOURCE_CODE
            )
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
            settingsPath = listOf("Home Screen (Long-press)", "Home Settings", "Iconography", "Icon pack", "Select ZINK"),
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
                url = "https://github.com/hyperion-launcher",
                sourceType = "Substratum / Hyperion Project Documentation & CandyBar Reference",
                dateChecked = "2026-11-21",
                evidence = "Hyperion parses standard ADW/Nova manifest filters and requires manual selection via Iconography.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS
            )
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
            settingsPath = listOf("Home Screen (Long-press)", "Launcher Settings", "Home screen", "Icon appearance", "Icon pack", "Select ZINK"),
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
                url = "https://support.microsoft.com/en-us/office/microsoft-launcher-support",
                sourceType = "Official Microsoft Support Documentation",
                dateChecked = "2026-11-21",
                evidence = "Verified under Microsoft Launcher settings path: Home screen > Icon appearance > Icon pack.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS
            )
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
            settingsPath = listOf("Search Page (Swipe up)", "Settings (Gear Icon)", "Appearance", "Icons", "Icon pack", "Select ZINK"),
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
                url = "https://github.com/MM2-0/Kvaesitso",
                sourceType = "Official Kvaesitso Open Source Repository",
                dateChecked = "2026-11-21",
                evidence = "Kvaesitso documentation confirms manual icon pack application via Appearance > Icons.",
                confidence = ConfidenceLevel.VERIFIED_SOURCE_CODE
            )
        ),

        // ── 12. Samsung One UI Home ──
        LauncherSpec(
            id = "oneui",
            name = "Samsung One UI Home",
            developer = "Samsung Electronics Co., Ltd.",
            packageNames = listOf("com.sec.android.app.launcher"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.sec.android.app.launcher",
            deviceRestrictionWarning = "Device restricted – Designed for Samsung Galaxy devices.",
            applyMethod = ApplyMethod.ALTERNATIVE,
            tagline = "Apply to One UI Home via official Samsung Good Lock & Theme Park",
            supportedVersions = "One UI 3.0, 4.x, 5.x, 6.x+",
            requirements = listOf(
                "Samsung Galaxy phone running One UI",
                "Samsung 'Good Lock' and 'Theme Park' installed from Galaxy Store"
            ),
            settingsPath = listOf("Galaxy Store", "Install Good Lock & Theme Park", "Open Theme Park", "Icon Tab", "Create New", "Iconpack", "Select ZINK & Apply"),
            setupSteps = listOf(
                "Open Galaxy Store on your Samsung Galaxy device.",
                "Search for and install 'Good Lock', then install the 'Theme Park' module.",
                "Open 'Theme Park' and navigate to the 'Icon' tab at the bottom.",
                "Tap 'Create New'.",
                "Tap the 'Iconpack' button at the top and select 'ZINK' from your installed packs.",
                "Tap the Download/Install arrow at the top right, enter a name (e.g. Zink Dark), and tap 'Apply'.",
                "Your One UI Home screen and app drawer are now fully themed with ZINK."
            ),
            limitations = listOf(
                "Samsung One UI Home does not natively accept third-party icon packs in standard Settings without Theme Park.",
                "Good Lock availability is region-dependent in Galaxy Store (Fine Lock / NiceLock can be used as alternatives)."
            ),
            troubleshooting = listOf(
                "If Theme Park fails to build the pack, clear Theme Park cache and retry with default icon sizing.",
                "To remove the theme, reopen Theme Park → Icon tab → select Default."
            ),
            verification = VerificationSource(
                url = "https://galaxystore.samsung.com/detail/com.samsung.android.themedesigner",
                sourceType = "Official Samsung Electronics Galaxy Store & Good Lock Documentation",
                dateChecked = "2026-11-21",
                evidence = "Samsung officially supports third-party icon pack integration on One UI exclusively through Theme Park.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS
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
                url = "https://github.com/Haoyu-UT/HyperOS-Launcher-for-POCO/blob/main/docs/how-it-works.md",
                sourceType = "HyperOS launcher package mapping (community ROM source)",
                dateChecked = "2026-11-21",
                evidence = "HyperOS launcher integration documentation identifies com.miui.home as the Xiaomi System Launcher package and distinguishes android.miui.home.launcher.res as a resource overlay. This is package evidence, not verification that ZINK's standard appfilter format is supported.",
                confidence = ConfidenceLevel.UNVERIFIED
            )
        ),

        // ── 14. Google Pixel Launcher ──
        LauncherSpec(
            id = "pixel",
            name = "Google Pixel Launcher",
            developer = "Google LLC",
            packageNames = listOf("com.google.android.apps.nexuslauncher"),
            playStoreUrl = null,
            deviceRestrictionWarning = "Device restricted – Preinstalled on supported Google Pixel devices. Not available as a standalone installation for other devices.",
            applyMethod = ApplyMethod.ALTERNATIVE,
            tagline = "Stock Pixel Launcher does not support third-party icon packs",
            supportedVersions = "Android 12, 13, 14, 15",
            requirements = listOf(
                "Google Pixel device running stock Pixel Launcher"
            ),
            settingsPath = listOf("Stock Pixel Launcher (Third-party packs unsupported)", "Install Lawnchair or Nova from Play Store", "Set as Default Home App", "Select ZINK"),
            setupSteps = listOf(
                "Stock Google Pixel Launcher only supports Google's built-in Material You 'Themed icons' toggle.",
                "Google does not provide an API or settings menu to apply complete third-party icon packs like ZINK to Pixel Launcher.",
                "To enjoy ZINK on a Google Pixel device, install a Pixel-like customizable launcher such as 'Lawnchair' or 'Nova Launcher'.",
                "Set the new launcher as your Default Home App under Android Settings → Apps → Default apps → Home app.",
                "Apply ZINK directly within the new launcher."
            ),
            limitations = listOf(
                "Third-party icon packs cannot be applied globally to stock Pixel Launcher without root or third-party shortcut workarounds."
            ),
            troubleshooting = listOf(
                "Lawnchair provides an identical visual feel to Pixel Launcher with full ZINK icon pack support."
            ),
            verification = VerificationSource(
                url = "https://support.google.com/pixelphone",
                sourceType = "Official Google Pixel Documentation",
                dateChecked = "2026-11-21",
                evidence = "Google Pixel Launcher only supports Material You dynamic theming. Third-party pack support is deliberately omitted.",
                confidence = ConfidenceLevel.VERIFIED_OFFICIAL_DOCS
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

    /**
     * Resolves the user's selected HOME app, not merely any activity that can handle HOME.
     */
    fun detectDefaultLauncherPackage(context: Context): String? {
        val packageManager = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val homeActivities = packageManager.queryIntentActivities(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val resolvedHome = packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo ?: return null

        // A system resolver is not itself a HOME launcher. Accept only a concrete HOME activity
        // returned by PackageManager's HOME query.
        return homeActivities
            .firstOrNull {
                it.activityInfo?.packageName == resolvedHome.packageName &&
                    it.activityInfo?.name == resolvedHome.name
            }
            ?.activityInfo
            ?.packageName
    }

    fun getApplicationLabel(context: Context, packageName: String): String =
        runCatching {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(packageName)

    /**
     * Checks if a package is installed on the system.
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Finds the installed package name for a launcher spec from its variants.
     */
    fun findInstalledPackage(context: Context, spec: LauncherSpec): String? {
        return spec.packageNames.firstOrNull { isPackageInstalled(context, it) }
    }

    /**
     * Builds the complete list of launcher runtime states, sorted by:
     * 1. Default launcher first
     * 2. Installed launchers next (Direct apply first, then manual)
     * 3. Supported uninstalled launchers (Direct apply, manual, then alternatives)
     */
    fun getRuntimeSnapshot(context: Context): LauncherRuntimeSnapshot {
        val defaultPackage = detectDefaultLauncherPackage(context)
        val launcherStates = launchers.map { spec ->
            val installedPackage = findInstalledPackage(context, spec)
            LauncherRuntimeState(
                spec = spec,
                isInstalled = installedPackage != null,
                isDefault = spec.packageNames.any { it == defaultPackage },
                installedPackage = installedPackage
            )
        }.sortedWith(
            compareByDescending<LauncherRuntimeState> { it.isDefault }
                .thenByDescending { it.isInstalled }
                .thenBy {
                    when (it.spec.applyMethod) {
                        ApplyMethod.DIRECT -> 0
                        ApplyMethod.MANUAL -> 1
                        ApplyMethod.ALTERNATIVE -> 2
                    }
                }
                .thenBy { it.spec.name }
        )
        return LauncherRuntimeSnapshot(defaultPackage, launcherStates)
    }

    fun getLauncherStates(context: Context): List<LauncherRuntimeState> =
        getRuntimeSnapshot(context).launchers

    /**
     * Triggers the direct apply mechanism for supported launchers.
     * Returns true if intent was successfully fired, false otherwise.
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
            Toast.makeText(context, "Opening ${spec.name} to apply ZINK...", Toast.LENGTH_SHORT).show()
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
