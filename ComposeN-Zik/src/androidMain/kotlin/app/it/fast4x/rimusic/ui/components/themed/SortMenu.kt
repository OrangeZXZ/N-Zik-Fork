package app.it.fast4x.rimusic.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.uiRoundnessShape
import app.n_zik.android.components.menu.ListMenu
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource


@Composable
fun SortMenu (
    title: String? = null,
    onDismiss: () -> Unit,
    onTitle: (() -> Unit)? = null,
    onDatePlayed: (() -> Unit)? = null,
    onPlayTime: (() -> Unit)? = null,
    onRelativePlayTime: (() -> Unit)? = null,
    onName: (() -> Unit)? = null,
    onSongNumber: (() -> Unit)? = null,
    onPosition: (() -> Unit)? = null,
    onArtist: (() -> Unit)? = null,
    onArtistAndAlbum: (() -> Unit)? = null,
    onAlbum: (() -> Unit)? = null,
    onAlbumYear: (() -> Unit)? = null,
    onYear: (() -> Unit)? = null,
    onDateAdded: (() -> Unit)? = null,
    onDateLiked: (() -> Unit)? = null,
    onDuration: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun SettingIcon(@DrawableRes icon: Int) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = colorPalette().accent.copy(alpha = 0.1f),
                    shape = uiRoundnessShape()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                tint = colorPalette().accent,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    
    ListMenu.Menu(title = title) {

        onTitle?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_title),
                icon = { SettingIcon(R.drawable.text) },
                onClick = {
                    onDismiss()
                    onTitle()
                }
            )
        }
        onDatePlayed?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_date_played),
                icon = { SettingIcon(R.drawable.up_right_arrow) },
                onClick = {
                    onDismiss()
                    onDatePlayed()
                }
            )
        }
        onDateLiked?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_date_liked),
                icon = { SettingIcon(R.drawable.heart) },
                onClick = {
                    onDismiss()
                    onDateLiked()
                }
            )
        }
        onPlayTime?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_listening_time),
                icon = { SettingIcon(R.drawable.trending) },
                onClick = {
                    onDismiss()
                    onPlayTime()
                }
            )
        }
        onRelativePlayTime?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_relative_listening_time),
                icon = { SettingIcon(R.drawable.trending) },
                onClick = {
                    onDismiss()
                    onRelativePlayTime()
                }
            )
        }
        onName?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_name),
                icon = { SettingIcon(R.drawable.text) },
                onClick = {
                    onDismiss()
                    onName()
                }
            )
        }
        onSongNumber?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_songs_number),
                icon = { SettingIcon(R.drawable.medical) },
                onClick = {
                    onDismiss()
                    onSongNumber()
                }
            )
        }
        onPosition?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_position),
                icon = { SettingIcon(R.drawable.position) },
                onClick = {
                    onDismiss()
                    onPosition()
                }
            )
        }
        onArtist?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_artist),
                icon = { SettingIcon(R.drawable.artist) },
                onClick = {
                    onDismiss()
                    onArtist()
                }
            )
        }
        onAlbum?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_album),
                icon = { SettingIcon(R.drawable.album) },
                onClick = {
                    onDismiss()
                    onAlbum()
                }
            )
        }
        onArtistAndAlbum?.let {
            ListMenu.Entry(
                text = "${stringResource(R.string.sort_artist)}, ${stringResource(R.string.sort_album)}",
                icon = { SettingIcon(R.drawable.artist) },
                onClick = {
                    onDismiss()
                    onArtistAndAlbum()
                }
            )
        }
        onAlbumYear?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_album_year),
                icon = { SettingIcon(R.drawable.calendar) },
                onClick = {
                    onDismiss()
                    onAlbumYear()
                }
            )
        }
        onYear?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_year),
                icon = { SettingIcon(R.drawable.calendar) },
                onClick = {
                    onDismiss()
                    onYear()
                }
            )
        }
        onDateAdded?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_date_added),
                icon = { SettingIcon(R.drawable.time) },
                onClick = {
                    onDismiss()
                    onDateAdded()
                }
            )
        }

        onDuration?.let {
            ListMenu.Entry(
                text = stringResource(R.string.sort_duration),
                icon = { SettingIcon(R.drawable.time) },
                onClick = {
                    onDismiss()
                    onDuration()
                }
            )
        }
    }
}

@Composable
fun FilterMenu (
    title: String? = null,
    onDismiss: () -> Unit,
    onAll: (() -> Unit)? = null,
    onOnlineSongs: (() -> Unit)? = null,
    onYoutubeLibrary: (() -> Unit)? = null,
    onVideos: (() -> Unit)? = null,
    onLocal: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
    onUnmatched: (() -> Unit)? = null,
    onDownloaded: (() -> Unit)? = null,
    onCached: (() -> Unit)? = null,
    onExplicit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun SettingIcon(@DrawableRes icon: Int) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = colorPalette().accent.copy(alpha = 0.1f),
                    shape = uiRoundnessShape()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                tint = colorPalette().accent,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    
    ListMenu.Menu(title = title) {

        onAll?.let {
            ListMenu.Entry(
                text = stringResource(R.string.all),
                icon = { SettingIcon(R.drawable.musical_notes) },
                onClick = {
                    onDismiss()
                    onAll()
                }
            )
        }
        onOnlineSongs?.let {
            ListMenu.Entry(
                text = stringResource(R.string.online_songs),
                icon = { SettingIcon(R.drawable.globe) },
                onClick = {
                    onDismiss()
                    onOnlineSongs()
                }
            )
        }
        onYoutubeLibrary?.let {
            ListMenu.Entry(
                text = stringResource(R.string.ytm_library),
                icon = { SettingIcon(R.drawable.ytmusic) },
                onClick = {
                    onDismiss()
                    onYoutubeLibrary()
                }
            )
        }

        onVideos?.let {
            ListMenu.Entry(
                text = stringResource(R.string.videos),
                icon = { SettingIcon(R.drawable.video) },
                onClick = {
                    onDismiss()
                    onVideos()
                }
            )
        }
        onUnmatched?.let {
            ListMenu.Entry(
                text = stringResource(R.string.unmatched),
                icon = { SettingIcon(R.drawable.alert) },
                onClick = {
                    onDismiss()
                    onUnmatched()
                }
            )
        }
        onFavorites?.let {
            ListMenu.Entry(
                text = stringResource(R.string.favorites),
                icon = { SettingIcon(R.drawable.heart) },
                onClick = {
                    onDismiss()
                    onFavorites()
                }
            )
        }
        onLocal?.let {
            ListMenu.Entry(
                text = stringResource(R.string.on_device),
                icon = { SettingIcon(R.drawable.devices) },
                onClick = {
                    onDismiss()
                    onLocal()
                }
            )
        }
        onDownloaded?.let {
            ListMenu.Entry(
                text = stringResource(R.string.downloaded),
                icon = { SettingIcon(R.drawable.downloaded) },
                onClick = {
                    onDismiss()
                    onDownloaded()
                }
            )
        }
        onCached?.let {
            ListMenu.Entry(
                text = stringResource(R.string.cached),
                icon = { SettingIcon(R.drawable.download) },
                onClick = {
                    onDismiss()
                    onCached()
                }
            )
        }
        onExplicit?.let {
            ListMenu.Entry(
                text = stringResource(R.string.explicit),
                icon = { SettingIcon(R.drawable.explicit) },
                onClick = {
                    onDismiss()
                    onExplicit()
                }
            )
        }
    }
}


