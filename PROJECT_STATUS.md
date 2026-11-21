# ZINK project status

Snapshot of the current source; update alongside behavior changes. See AGENTS.md for architecture and implementation details.

## Completed features

- Compose app shell with Home, Icons, Wallpapers, Apply, and Requests tabs plus full-screen Settings. Settings title/subtitle remain fixed over independently scrolling sections; the fixed glass bottom navigation stays available while Settings is open. Settings and Apply scroll areas leave space for system bars and bottom navigation.
- 84-entry bundled icon catalog with search, category filtering, details, and package-name copy.
- Launcher icon-pack XML/resources and manifest discovery hooks. Settings accurately displays the static 0.74 unthemed-icon fallback scale; it is not a dynamic masking toggle.
- 43 bundled wallpapers with All Wallpapers/Favourites filtering, immediate saved-favorite updates, a no-favourites empty state, preview transforms, download, and Home/Lock/Both application. Download quality persists with 4K Original as the default; FHD 1080p downsizes actual JPEG output within 1920×1080 landscape / 1080×1920 portrait bounds.
- Apply launcher catalog with runtime installed/default detection through PackageManager HOME intent resolution; resolved activities are checked against HOME candidates to avoid reporting an unresolved chooser as a selected launcher. Installed packages, current HOME selection, and DIRECT/MANUAL/ALTERNATIVE integration methods remain separate. The detected app label and package appear on the default card and Settings row. Detection refreshes when Apply composes, Settings opens the Apply destination, and ZINK resumes. There are 14 launcher records, including Xiaomi System Launcher (com.miui.home) as alternative-only; android.miui.home.launcher.res is not treated as the launcher. Unverified launchers show no direct apply and are directed to compatible alternatives. The fixed Apply header/default card and scrolling launcher list remain in place. The connected device resolves to Nothing Launcher; Xiaomi physical-device verification is still outstanding.
- Requests screen with unthemed app discovery, themed catalog and manual-entry modes; unthemed apps begin unselected and require explicit user selection. Selected catalog requests include app names and package names in the email. The themed-app mode has a selected-count submit button, and failed email launch copies the request to the clipboard when available.
- Persisted Pure AMOLED Black, wallpaper download quality, favourite wallpaper IDs, and Home Header Artwork selection. Favourites and header art restore from SharedPreferences; unknown saved IDs fall back safely without exposing missing catalog items. Settings displays an underlined, tappable “Created by Gesuling” attribution that opens https://github.com/Gesuling/Zink.

## Known bugs and misleading UI

- Settings > Clear Icon Cache only shows a toast; no cache is cleared.
- Settings version label says v1.0.0 (API 37), while Gradle declares versionName 1.0 and versionCode 1.
- Apply calls the launcher list “VERIFIED” across mixed confidence levels. Registry claims and checked dates are static and may be stale.

## Incomplete work

- Request form/selection state, filters, and other transient screen state are not persisted across process death.
- Launcher compatibility records/guides need device/version revalidation; manifest discovery filters do not prove compatibility. Dynamic HOME detection and refresh compile and unit tests pass, but were not manually verified on Xiaomi HyperOS; the connected device resolves to com.nothing.launcher.
- Icon-pack XML is duplicated in assets and res/xml and needs parity when updated. The static 0.74 fallback scale only works in launchers that implement generated fallback rules; ZINK cannot toggle it at runtime. Kvaesitso documents fallback scale support; current Lawnchair docs say generated fallbacks are not implemented. Other launchers should be checked by version.
- Automated coverage remains limited: wallpaper dimensions and favorite ID/header artwork fallback rules have unit tests; saved quality, Android bitmap decoding, SharedPreferences persistence/restoration, stale favorite/header values, and packaged Lawnchair/Kvaesitso icon decoding have instrumentation tests. The latest instrumentation run passed on connected device A069P (Android 17). Lawnchair and Kvaesitso fallback icons were also visually confirmed in the Apply list on that device. Most other feature flows remain uncovered.
- No backend for icon requests or remote icon/wallpaper catalog exists.

## Recommended next tasks

1. Test HOME detection on a physical Xiaomi HyperOS device and confirm the resolved package is com.miui.home, the app label is shown, and android.miui.home.launcher.res is not misidentified. Also recheck Nothing, Samsung, and third-party launcher switching/resume flows.
2. Replace the Apply “VERIFIED” total with wording/counts that reflect registry confidence, and revalidate compatibility claims on target launcher versions.
3. Wire Settings rows to their stated behavior or relabel/remove controls that are intentionally informational.
4. Add focused tests for request serialization, launcher registry ordering/method behavior, and wallpaper application state; wallpaper quality, favorites, and header-art persistence already have targeted coverage.
5. Keep app version/build target labels derived from package metadata rather than hard-coded text.
6. Audit launcher XML copies and icon catalog/resource mappings together whenever pack content changes.

