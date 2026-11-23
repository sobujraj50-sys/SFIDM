package com.sfidm.downloader.data

import android.content.Context
import com.sfidm.downloader.model.DownloadItem
import com.sfidm.downloader.model.DownloadStatus
import com.sfidm.downloader.ui.strings.AppLanguage
import com.sfidm.downloader.ui.theme.AppTheme
import org.json.JSONArray
import org.json.JSONObject

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("sfidm_prefs", Context.MODE_PRIVATE)

    fun getTheme(): AppTheme = AppTheme.fromId(prefs.getString("theme_id", AppTheme.MIDNIGHT_CYBER.id))

    fun saveTheme(theme: AppTheme) {
        prefs.edit().putString("theme_id", theme.id).apply()
    }

    fun getLanguage(): AppLanguage = AppLanguage.fromCode(prefs.getString("lang_code", AppLanguage.BN.code))

    fun saveLanguage(lang: AppLanguage) {
        prefs.edit().putString("lang_code", lang.code).apply()
    }

    fun saveDownloads(list: List<DownloadItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("fileName", item.fileName)
                put("url", item.url)
                put("totalBytes", item.totalBytes)
                put("downloadedBytes", item.downloadedBytes)
                put("threads", item.threads)
                // If app was closed while downloading, restore as PAUSED
                val persistedStatus = if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED) {
                    DownloadStatus.PAUSED
                } else {
                    item.status
                }
                put("status", persistedStatus.name)
                put("filePath", item.filePath)
                put("errorMessage", item.errorMessage)
            }
            array.put(obj)
        }
        prefs.edit().putString("downloads_json", array.toString()).apply()
    }

    fun loadDownloads(): List<DownloadItem> {
        val raw = prefs.getString("downloads_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val statusName = obj.optString("status", DownloadStatus.PAUSED.name)
                    val status = runCatching { DownloadStatus.valueOf(statusName) }
                        .getOrDefault(DownloadStatus.PAUSED)
                    add(
                        DownloadItem(
                            id = obj.getString("id"),
                            fileName = obj.getString("fileName"),
                            url = obj.getString("url"),
                            totalBytes = obj.optLong("totalBytes", 0L),
                            downloadedBytes = obj.optLong("downloadedBytes", 0L),
                            speedBytesPerSec = 0L,
                            threads = obj.optInt("threads", 8),
                            status = status,
                            filePath = obj.optString("filePath", ""),
                            errorMessage = obj.optString("errorMessage", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
