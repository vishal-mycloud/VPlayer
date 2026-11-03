package com.vplayer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vplayer.app.VPlayerViewModel
import com.vplayer.app.data.HistoryEntry
import com.vplayer.app.data.VideoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: VPlayerViewModel, onPickLocal: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showUrlDialog by remember { mutableStateOf(false) }
    val favorites by vm.favorites.collectAsState()
    val history by vm.history.collectAsState()
    val favoriteUris = favorites.map { it.uri }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("V Player") },
                actions = {
                    if (tab == 2 && history.isNotEmpty()) {
                        IconButton(onClick = { vm.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear history")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Favorites") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.History, null) }, label = { Text("History") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeTab(
                    history = history,
                    favoriteUris = favoriteUris,
                    onPickLocal = onPickLocal,
                    onOpenUrl = { showUrlDialog = true },
                    onPlay = vm::play,
                    onToggleFavorite = vm::toggleFavorite
                )
                1 -> if (favorites.isEmpty()) {
                    EmptyHint("No favorites yet. Tap the heart while watching a video.")
                } else {
                    LazyColumn {
                        items(favorites, key = { it.uri }) { item ->
                            VideoRow(
                                item = item,
                                subtitle = if (item.isLocal) "Local file" else item.uri,
                                isFavorite = true,
                                onClick = { vm.play(item) },
                                onToggleFavorite = { vm.toggleFavorite(item) }
                            )
                        }
                    }
                }
                else -> if (history.isEmpty()) {
                    EmptyHint("Nothing watched yet.")
                } else {
                    LazyColumn {
                        items(history, key = { it.item.uri }) { entry ->
                            VideoRow(
                                item = entry.item,
                                subtitle = historySubtitle(entry),
                                isFavorite = entry.item.uri in favoriteUris,
                                onClick = { vm.play(entry.item) },
                                onToggleFavorite = { vm.toggleFavorite(entry.item) },
                                onRemove = { vm.removeHistory(entry.item.uri) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUrlDialog) {
        UrlDialog(
            onDismiss = { showUrlDialog = false },
            onSubmit = { text ->
                val error = vm.openUrl(text)
                if (error == null) showUrlDialog = false
                error
            }
        )
    }
}

@Composable
private fun HomeTab(
    history: List<HistoryEntry>,
    favoriteUris: Set<String>,
    onPickLocal: () -> Unit,
    onOpenUrl: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPickLocal, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FolderOpen, null)
                Text("  Open local video")
            }
            OutlinedButton(onClick = onOpenUrl, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Link, null)
                Text("  Play from URL")
            }
        }
        if (history.isEmpty()) {
            EmptyHint("Open a video from your device or paste a direct video link.")
        } else {
            Text(
                "Continue watching",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn {
                items(history.take(5), key = { it.item.uri }) { entry ->
                    VideoRow(
                        item = entry.item,
                        subtitle = historySubtitle(entry),
                        isFavorite = entry.item.uri in favoriteUris,
                        onClick = { onPlay(entry.item) },
                        onToggleFavorite = { onToggleFavorite(entry.item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoRow(
    item: VideoItem,
    subtitle: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(Icons.Default.PlayCircle, null) },
        headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from history")
                    }
                }
            }
        }
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun UrlDialog(onDismiss: () -> Unit, onSubmit: (String) -> String?) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Play from URL") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    singleLine = true,
                    placeholder = { Text("https://example.com/video.mp4") },
                    isError = error != null
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { error = onSubmit(text) }) { Text("Play") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun historySubtitle(entry: HistoryEntry): String {
    val source = if (entry.item.isLocal) "Local file" else "Online"
    return if (entry.durationMs > 0) {
        "$source • ${formatTime(entry.positionMs)} / ${formatTime(entry.durationMs)}"
    } else {
        source
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
