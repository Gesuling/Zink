package com.gesuling.zink.ui.icons

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads published catalogue entries from Firestore and keeps a small metadata
 * cache so the last successful remote catalogue remains available offline.
 */
object RemoteIconRepository {
    private const val TAG = "RemoteIconRepository"
    private const val PREFS = "zink_remote_icons"
    private const val KEY_CACHED_ICONS = "published_icons"

    suspend fun sync(context: Context): List<AppIconItem> = withContext(Dispatchers.IO) {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("icons")
            .whereEqualTo("published", true)
            .get(Source.SERVER)
            .await()

        val icons = snapshot.documents.mapNotNull { document ->
            val name = document.getString("name")?.trim().orEmpty()
            val category = document.getString("category")?.trim().orEmpty()
            val imageUrl = document.getString("imageUrl")?.trim().orEmpty()
            if (name.isBlank() || category.isBlank() || !imageUrl.startsWith("https://") && !imageUrl.startsWith("http://")) {
                Log.w(TAG, "Skipping invalid published icon document: ${document.id}")
                return@mapNotNull null
            }

            AppIconItem(
                id = 100_000 + (document.id.hashCode() and 0x3fffffff),
                appName = name,
                packageName = document.getString("packageName")?.trim().orEmpty(),
                category = category,
                drawableResId = com.gesuling.zink.R.drawable.icon_zink_app,
                imageUrl = imageUrl,
                remoteVersion = document.getString("version") ?: "1.0",
                remoteDocumentId = document.id
            )
        }.distinctBy { it.remoteDocumentId }

        saveCachedIcons(context, icons)
        icons
    }

    fun readCached(context: Context): List<AppIconItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CACHED_ICONS, null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val documentId = item.optString("documentId")
                    val imageUrl = item.optString("imageUrl")
                    if (documentId.isBlank() || imageUrl.isBlank()) continue
                    add(
                        AppIconItem(
                            id = 100_000 + (documentId.hashCode() and 0x3fffffff),
                            appName = item.optString("name"),
                            packageName = item.optString("packageName"),
                            category = item.optString("category"),
                            drawableResId = com.gesuling.zink.R.drawable.icon_zink_app,
                            imageUrl = imageUrl,
                            remoteVersion = item.optString("version", "1.0"),
                            remoteDocumentId = documentId
                        )
                    )
                }
            }
        }.getOrElse {
            Log.w(TAG, "Could not read cached icon catalogue", it)
            emptyList()
        }
    }

    private fun saveCachedIcons(context: Context, icons: List<AppIconItem>) {
        val array = JSONArray()
        icons.forEach { icon ->
            array.put(
                JSONObject()
                    .put("documentId", icon.remoteDocumentId)
                    .put("name", icon.appName)
                    .put("packageName", icon.packageName)
                    .put("category", icon.category)
                    .put("imageUrl", icon.imageUrl)
                    .put("version", icon.remoteVersion)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CACHED_ICONS, array.toString())
            .apply()
    }

    fun mergeWithBundled(remoteIcons: List<AppIconItem>): List<AppIconItem> {
        val remotePackages = remoteIcons.mapNotNull { it.packageName.takeIf(String::isNotBlank) }.toSet()
        val bundled = sampleIconsList.filterNot { it.packageName in remotePackages }
        return (bundled + remoteIcons).sortedWith(compareBy(NaturalOrderComparator) { it.appName })
    }
}
