package com.shohan.pro.downloader.util

import android.content.Context
import com.shohan.pro.downloader.data.model.MediaCategory
import com.shohan.pro.downloader.ui.CopiedLinkItem
import org.json.JSONArray
import org.json.JSONObject

object CopiedLinksStorage {
    private const val PREF_NAME = "link_downloader_copied_links_pref"
    private const val KEY_LINKS = "saved_copied_links"

    fun loadLinks(context: Context): List<CopiedLinkItem> {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_LINKS, null) ?: return emptyList()
            val array = JSONArray(jsonString)
            val list = mutableListOf<CopiedLinkItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val url = obj.getString("url")
                val platform = obj.optString("platform", "Web Link")
                val catStr = obj.optString("category", MediaCategory.OTHER.name)
                val category = try {
                    MediaCategory.valueOf(catStr)
                } catch (_: Exception) {
                    MediaCategory.OTHER
                }
                val ts = obj.optLong("timestamp", System.currentTimeMillis())
                list.add(CopiedLinkItem(id, url, platform, category, ts))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLinks(context: Context, links: List<CopiedLinkItem>) {
        try {
            val array = JSONArray()
            links.take(25).forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("url", item.url)
                    put("platform", item.platformName)
                    put("category", item.category.name)
                    put("timestamp", item.timestamp)
                }
                array.put(obj)
            }
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LINKS, array.toString())
                .apply()
        } catch (_: Exception) {}
    }
}
