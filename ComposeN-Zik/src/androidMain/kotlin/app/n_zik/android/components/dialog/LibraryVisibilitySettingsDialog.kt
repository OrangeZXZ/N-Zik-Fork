package app.n_zik.android.components.dialog

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.it.fast4x.rimusic.utils.showFavoritesPlaylistKey
import app.it.fast4x.rimusic.utils.showCachedPlaylistKey
import app.it.fast4x.rimusic.utils.showDownloadedPlaylistKey
import app.it.fast4x.rimusic.utils.showOnDevicePlaylistKey
import app.it.fast4x.rimusic.utils.showPipedPlaylistsKey
import app.it.fast4x.rimusic.utils.showPinnedPlaylistsKey

object LibraryVisibilitySettingsDialog : Dialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.library_visibility)

    override var isActive: Boolean by mutableStateOf(false)

    @Composable
    override fun DialogBody() {
        val items = listOf(
            ToggleItem(
                id = "favorites",
                iconRes = R.drawable.heart,
                label = stringResource(R.string.favorites),
                preferenceKey = showFavoritesPlaylistKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "cached",
                iconRes = R.drawable.server,
                label = stringResource(R.string.cached),
                preferenceKey = showCachedPlaylistKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "downloaded",
                iconRes = R.drawable.downloaded,
                label = stringResource(R.string.downloaded),
                preferenceKey = showDownloadedPlaylistKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "on_device",
                iconRes = R.drawable.folder,
                label = stringResource(R.string.on_device),
                preferenceKey = showOnDevicePlaylistKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "piped_playlists",
                iconRes = R.drawable.piped_logo,
                label = stringResource(R.string.piped_playlists),
                preferenceKey = showPipedPlaylistsKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "pinned_playlists",
                iconRes = R.drawable.pin_filled,
                label = stringResource(R.string.pinned_playlists),
                preferenceKey = showPinnedPlaylistsKey,
                defaultValue = true
            )
        )

        ToggleListDialog(
            items = items,
            contentHeight = 400.dp
        )
    }
}
