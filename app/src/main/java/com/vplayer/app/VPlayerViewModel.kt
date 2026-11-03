package com.vplayer.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.vplayer.app.data.VPlayerRepository
import com.vplayer.app.data.VideoItem

class VPlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VPlayerRepository(app)

    val favorites = repo.favorites
    val history = repo.history

    var nowPlaying by mutableStateOf<VideoItem?>(null)
        private set
    var startPositionMs by mutableLongStateOf(0L)
        private set

    fun play(item: VideoItem) {
        startPositionMs = repo.resumePositionMs(item.uri)
        repo.recordProgress(item, startPositionMs, 0L)
        nowPlaying = item
    }

    fun closePlayer() {
        nowPlaying = null
    }

    fun openLocal(uri: Uri) {
        try {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            // Some providers don't allow persistable grants; playback still works this session.
        }
        val title = queryDisplayName(uri) ?: uri.lastPathSegment ?: "Local video"
        play(VideoItem(uri = uri.toString(), title = title, isLocal = true))
    }

    /** Returns an error message, or null if the URL was accepted and playback started. */
    fun openUrl(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return "Enter a video URL."
        val uri = Uri.parse(text)
        val scheme = uri.scheme?.lowercase()
        if ((scheme != "http" && scheme != "https") || uri.host.isNullOrEmpty()) {
            return "Enter a valid http:// or https:// link."
        }
        val title = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: uri.host!!
        play(VideoItem(uri = text, title = title, isLocal = false))
        return null
    }

    fun toggleFavorite(item: VideoItem) = repo.toggleFavorite(item)

    fun saveProgress(item: VideoItem, positionMs: Long, durationMs: Long) =
        repo.recordProgress(item, positionMs, durationMs)

    fun removeHistory(uri: String) = repo.removeHistory(uri)

    fun clearHistory() = repo.clearHistory()

    private fun queryDisplayName(uri: Uri): String? = try {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }
}
