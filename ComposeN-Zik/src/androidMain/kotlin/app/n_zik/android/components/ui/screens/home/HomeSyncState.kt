package app.n_zik.android.components.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object HomeSyncState {
    var isSyncingArtists by mutableStateOf(false)
    var artistSyncProgress by mutableStateOf(0f)
    var artistSyncCurrentName by mutableStateOf("")
    var artistSyncCurrentIndex by mutableStateOf(0)
    var artistSyncTotal by mutableStateOf(0)
    var artistSyncFailed by mutableStateOf(0)

    var isSyncingAlbums by mutableStateOf(false)
    var albumSyncProgress by mutableStateOf(0f)
    var albumSyncCurrentName by mutableStateOf("")
    var albumSyncCurrentIndex by mutableStateOf(0)
    var albumSyncTotal by mutableStateOf(0)
    var albumSyncFailed by mutableStateOf(0)

    var isSyncingPlaylists by mutableStateOf(false)
    var playlistSyncProgress by mutableStateOf(0f)
    var playlistSyncCurrentName by mutableStateOf("")
    var playlistSyncCurrentIndex by mutableStateOf(0)
    var playlistSyncTotal by mutableStateOf(0)
    var playlistSyncFailed by mutableStateOf(0)
}
