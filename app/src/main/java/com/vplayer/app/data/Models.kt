package com.vplayer.app.data

data class VideoItem(
    val uri: String,
    val title: String,
    val isLocal: Boolean
)

data class HistoryEntry(
    val item: VideoItem,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long
)
