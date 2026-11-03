package com.vplayer.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vplayer.app.VPlayerViewModel

@Composable
fun VPlayerRoot(vm: VPlayerViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.openLocal(uri)
    }
    val favorites by vm.favorites.collectAsState()
    val current = vm.nowPlaying

    if (current != null) {
        PlayerScreen(
            item = current,
            startPositionMs = vm.startPositionMs,
            isFavorite = favorites.any { it.uri == current.uri },
            onToggleFavorite = { vm.toggleFavorite(current) },
            onProgress = { pos, dur -> vm.saveProgress(current, pos, dur) },
            onBack = { vm.closePlayer() }
        )
    } else {
        HomeScreen(
            vm = vm,
            onPickLocal = { picker.launch(arrayOf("video/*")) }
        )
    }
}
