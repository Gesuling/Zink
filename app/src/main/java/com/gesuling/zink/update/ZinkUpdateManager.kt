package com.gesuling.zink.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.*
import com.gesuling.zink.R
import com.gesuling.zink.ui.settings.compareVersions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val apkUrl: String?,
    val prerelease: Boolean,
    val draft: Boolean
)

object ZinkUpdateManager {
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/Gesuling/Zink/releases/latest"
    private const val CHANNEL_ID = "zink_updates"
    private const val PREFS_NAME = "zink_update_prefs"
    private const val KEY_SUPPRESSED_VERSION = "suppressed_version"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        val name = "ZINK Updates"
        val descriptionText = "Notifications for new ZINK releases"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    suspend fun fetchLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", tagName)
                val body = json.optString("body", "New ZINK update available.")
                val prerelease = json.optBoolean("prerelease", false)
                val draft = json.optBoolean("draft", false)

                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val browserDownloadUrl = asset.optString("browser_download_url", "")
                        if (browserDownloadUrl.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = browserDownloadUrl
                            break
                        }
                    }
                }
                GitHubRelease(tagName, name, body, apkUrl, prerelease, draft)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isNewVersionAvailable(context: Context, remoteTag: String): Boolean {
        val installedVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1.3"
        }.getOrDefault("0.1.3")
        return compareVersions(remoteTag, installedVersion) > 0
    }

    fun isNewVersionAvailableWithVersion(remoteTag: String, installedVersion: String): Boolean {
        return compareVersions(remoteTag, installedVersion) > 0
    }

    fun isSuppressed(context: Context, version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SUPPRESSED_VERSION, null) == version
    }

    fun suppressVersion(context: Context, version: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_SUPPRESSED_VERSION, version)
        }
    }

    fun showUpdateNotification(context: Context, release: GitHubRelease) {
        createNotificationChannel(context)

        val updateIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = "ACTION_UPDATE"
            putExtra("apk_url", release.apkUrl ?: "https://github.com/Gesuling/Zink/releases")
        }
        val updatePendingIntent = PendingIntent.getBroadcast(
            context, 0, updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val laterIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = "ACTION_LATER"
            putExtra("version", release.tagName)
        }
        val laterPendingIntent = PendingIntent.getBroadcast(
            context, 1, laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_mark)
            .setContentTitle("ZINK ${release.tagName} Available")
            .setContentText(release.body.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(release.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_mark, "Update", updatePendingIntent)
            .addAction(R.drawable.ic_launcher_mark, "Later", laterPendingIntent)
            .build()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Handled gracefully if notification permission is denied on Android 13+
        }
    }

    fun schedulePeriodicCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ZinkUpdateWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ZinkUpdateCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

class ZinkUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val release = ZinkUpdateManager.fetchLatestRelease() ?: return Result.retry()
        if (!release.draft && !release.prerelease) {
            if (ZinkUpdateManager.isNewVersionAvailable(applicationContext, release.tagName)) {
                if (!ZinkUpdateManager.isSuppressed(applicationContext, release.tagName)) {
                    ZinkUpdateManager.showUpdateNotification(applicationContext, release)
                }
            }
        }
        return Result.success()
    }
}

class UpdateActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_LATER" -> {
                val version = intent.getStringExtra("version")
                if (version != null) {
                    ZinkUpdateManager.suppressVersion(context, version)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(1001)
            }
            "ACTION_UPDATE" -> {
                val urlString = intent.getStringExtra("apk_url") ?: "https://github.com/Gesuling/Zink/releases"
                if (urlString.startsWith("http")) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
