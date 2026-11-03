package com.vplayer.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Stores favorites and watch history in SharedPreferences as JSON. */
class VPlayerRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vplayer_store", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(readFavorites())
    val favorites: StateFlow<List<VideoItem>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow(readHistory())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    fun toggleFavorite(item: VideoItem) {
        val current = _favorites.value
        val next = if (current.any { it.uri == item.uri }) {
            current.filterNot { it.uri == item.uri }
        } else {
            listOf(item) + current
        }
        _favorites.value = next
        prefs.edit().putString(KEY_FAVORITES, itemsToJson(next)).apply()
    }

    fun resumePositionMs(uri: String): Long {
        val entry = _history.value.firstOrNull { it.item.uri == uri } ?: return 0L
        if (entry.durationMs > 0 && entry.positionMs > entry.durationMs - 5_000) return 0L
        return entry.positionMs
    }

    fun recordProgress(item: VideoItem, positionMs: Long, durationMs: Long) {
        val old = _history.value.firstOrNull { it.item.uri == item.uri }
        val entry = HistoryEntry(
            item = item,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = if (durationMs > 0) durationMs else old?.durationMs ?: 0L,
            lastPlayedAt = System.currentTimeMillis()
        )
        val next = (listOf(entry) + _history.value.filterNot { it.item.uri == item.uri }).take(MAX_HISTORY)
        _history.value = next
        saveHistory(next)
    }

    fun removeHistory(uri: String) {
        val next = _history.value.filterNot { it.item.uri == uri }
        _history.value = next
        saveHistory(next)
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory(emptyList())
    }

    // ---- JSON helpers ----

    private fun itemToJson(item: VideoItem) = JSONObject().apply {
        put("uri", item.uri)
        put("title", item.title)
        put("local", item.isLocal)
    }

    private fun itemFromJson(o: JSONObject) = VideoItem(
        uri = o.getString("uri"),
        title = o.optString("title", "Video"),
        isLocal = o.optBoolean("local", false)
    )

    private fun itemsToJson(items: List<VideoItem>): String =
        JSONArray().apply { items.forEach { put(itemToJson(it)) } }.toString()

    private fun saveHistory(list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("item", itemToJson(it.item))
                put("pos", it.positionMs)
                put("dur", it.durationMs)
                put("at", it.lastPlayedAt)
            })
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun readFavorites(): List<VideoItem> = try {
        val arr = JSONArray(prefs.getString(KEY_FAVORITES, "[]"))
        (0 until arr.length()).map { itemFromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun readHistory(): List<HistoryEntry> = try {
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            HistoryEntry(
                item = itemFromJson(o.getJSONObject("item")),
                positionMs = o.optLong("pos", 0L),
                durationMs = o.optLong("dur", 0L),
                lastPlayedAt = o.optLong("at", 0L)
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_HISTORY = "history"
        const val MAX_HISTORY = 100
    }
}
