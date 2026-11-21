# Launcher icon sources

These bundled fallback images support launcher rows when the corresponding launcher is not installed. On a device with the launcher installed, ZINK reads that package's current application icon first.

| Registry ID | Source |
| --- | --- |
| nova | [Google Play listing](https://play.google.com/store/apps/details?id=com.teslacoilsw.launcher) |
| niagara | [Google Play listing](https://play.google.com/store/apps/details?id=bitpit.launcher) |
| smart | [Google Play listing](https://play.google.com/store/apps/details?id=ginlemon.flowerfree) |
| action | [Google Play listing](https://play.google.com/store/apps/details?id=com.actionlauncher.playstore) |
| nothing | [Google Play listing](https://play.google.com/store/apps/details?id=com.nothing.launcher) |
| hyperion | [Google Play listing](https://play.google.com/store/apps/details?id=projekt.launcher) |
| microsoft | [Google Play listing](https://play.google.com/store/apps/details?id=com.microsoft.launcher) |
| oneui | [Google Play listing](https://play.google.com/store/apps/details?id=com.sec.android.app.launcher) |
| lawnchair | [Official Lawnchair repository icon](https://github.com/LawnchairLauncher/lawnchair/blob/16-dev/ic_launcher_home-playstore.png) |
| kvaesitso | [Official Kvaesitso repository icon](https://github.com/MM2-0/Kvaesitso/blob/main/assets/icons/ic_launcher.png) |
| adw | [MaterialOS icon pack mapping/artwork](https://github.com/materialos/android-icon-pack/blob/master/app/src/main/res/drawable-nodpi/___adw_launcher.png) |
| apex | [MaterialOS icon pack mapping/artwork](https://github.com/materialos/android-icon-pack/blob/master/app/src/main/res/drawable-nodpi/apex_launcher.png) |
| pixel | [APKMirror Pixel Launcher 17 package listing/icon](https://www.apkmirror.com/apk/google-inc/pixel-launcher/pixel-launcher-17-release/pixel-launcher-17-6-android-apk-download/) |

Google Play assets are used where their listing exposed the matching package's app icon. The current ADW, Apex, Lawnchair, Kvaesitso, and Pixel package IDs did not have usable current Play listing assets during sourcing; repository artwork is used for the first four listed repositories, while Pixel uses an archived package-listing icon as a fallback. Installed package icons take precedence for all entries.

The ADW and Apex bundled images are derived from the MaterialOS Android Icon Pack repository, which identifies its graphics as CC BY 4.0. Preserve attribution to [MaterialOS](https://github.com/materialos/android-icon-pack) if redistributing those image assets outside this app.
