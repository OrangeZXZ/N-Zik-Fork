package app.n_zik.android.components.player.lyrics

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.models.Lyrics
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.core.database.Database
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.themed.DefaultDialog
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.Menu
import app.it.fast4x.rimusic.ui.components.themed.MenuEntry
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.uiRoundnessShape
import it.fast4x.lrclib.LrcLib
import it.fast4x.lrclib.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

private val trackSelectorTextFieldColors: TextFieldColors
    @Composable
    get() = TextFieldDefaults.colors(
        unfocusedTextColor = colorPalette().text,
        focusedTextColor = colorPalette().text,
        unfocusedIndicatorColor = colorPalette().text,
        focusedIndicatorColor = colorPalette().text
    )

@Composable
fun LyricsTrackSelector(
    mediaId: String,
    lyrics: Lyrics?,
    initialTitle: String,
    initialArtistName: String,
    onTitleChange: (String) -> Unit,
    onArtistNameChange: (String) -> Unit,
    playerEnableLyricsPopupMessage: Boolean,
    coroutineScope: CoroutineScope,
    onSearchRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val menuState = LocalMenuState.current
    var loading by remember { mutableStateOf(true) }
    val tracks = remember { mutableStateListOf<Track>() }
    var error by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(initialTitle) }
    var artistName by remember { mutableStateOf(initialArtistName) }

    LaunchedEffect(title, artistName) {
        onTitleChange(title)
        onArtistNameChange(artistName)
    }

    LaunchedEffect(Unit) {
        kotlin.runCatching {
            LrcLib.lyrics(
                artist = artistName,
                title = title
            )?.onSuccess {
                if (it.isNotEmpty() && playerEnableLyricsPopupMessage)
                    coroutineScope.launch {
                        Toaster.s(
                            R.string.info_lyrics_tracks_found_on_s,
                            "LrcLib.net",
                            duration = Toast.LENGTH_LONG
                        )
                    }
                else
                    if (playerEnableLyricsPopupMessage)
                        coroutineScope.launch {
                            Toaster.e(
                                R.string.info_lyrics_not_found_on_s,
                                "LrcLib.net",
                                duration = Toast.LENGTH_LONG
                            )
                        }
                if (it.isEmpty()){
                    menuState.display {
                        Menu {
                            MenuEntry(
                                icon = R.drawable.chevron_back,
                                text = stringResource(R.string.cancel),
                                onClick = { 
                                    menuState.hide() 
                                    onDismiss()
                                }
                            )
                            Row {
                                TextField(
                                    value = title,
                                    onValueChange = { newTitle ->
                                        title = newTitle
                                    },
                                    singleLine = true,
                                    colors = trackSelectorTextFieldColors,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .weight(1f)
                                )
                                TextField(
                                    value = artistName,
                                    onValueChange = { newArtistName ->
                                        artistName = newArtistName
                                    },
                                    singleLine = true,
                                    colors = trackSelectorTextFieldColors,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .weight(1f)
                                )
                                IconButton(
                                    icon = R.drawable.search,
                                    color = Color.Black,
                                    onClick = {
                                        menuState.hide()
                                        onSearchRetry()
                                    },
                                    modifier = Modifier
                                        .background(
                                            shape = uiRoundnessShape(),
                                            color = Color.White
                                        )
                                        .padding(all = 4.dp)
                                        .size(24.dp)
                                        .align(Alignment.CenterVertically)
                                        .weight(0.2f)
                                )
                            }
                        }
                    }
                }

                tracks.clear()
                tracks.addAll(it)
                loading = false
                error = false
            }?.onFailure {
                if (playerEnableLyricsPopupMessage)
                    coroutineScope.launch {
                        Toaster.e(
                            R.string.an_error_has_occurred_while_fetching_the_lyrics,
                            "KuGou.com",
                            duration = Toast.LENGTH_LONG
                        )
                    }

                loading = false
                error = true
            } ?: run { loading = false }
        }.onFailure {
            Timber.tag("LyricsTrackSelector").e("get error 1 ${it.stackTraceToString()}")
        }
    }

    if (loading) {
        DefaultDialog(
            onDismiss = {
                onDismiss()
            }
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    LaunchedEffect(tracks.size, title, artistName) {
        if (tracks.isNotEmpty()) {
            menuState.display {
                Menu {
                    MenuEntry(
                        icon = R.drawable.chevron_back,
                        text = stringResource(R.string.cancel),
                        onClick = { 
                            menuState.hide()
                            onDismiss()
                        }
                    )
                    Row{
                        TextField(
                            value = title,
                            onValueChange = { newTitle ->
                                title = newTitle
                            },
                            singleLine = true,
                            colors = trackSelectorTextFieldColors,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .weight(1f)
                        )
                        TextField(
                            value = artistName,
                            onValueChange = { newArtistName ->
                                artistName = newArtistName
                            },
                            singleLine = true,
                            colors = trackSelectorTextFieldColors,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .weight(1f)
                        )
                        IconButton(
                            icon = R.drawable.search,
                            color = Color.Black,
                            onClick = {
                                menuState.hide()
                                onSearchRetry()
                            },
                            modifier = Modifier
                                .background(shape = uiRoundnessShape(), color = Color.White)
                                .padding(all = 4.dp)
                                .size(24.dp)
                                .align(Alignment.CenterVertically)
                                .weight(0.2f)
                        )
                    }
                    tracks.forEach {
                        MenuEntry(
                            icon = R.drawable.text,
                            text = "${it.artistName} - ${it.trackName}",
                            secondaryText = "(${stringResource(R.string.sort_duration)} ${
                                it.duration.seconds.toComponents { minutes, seconds, _ ->
                                    "$minutes:${seconds.toString().padStart(2, '0')}"
                                }
                            } ${stringResource(R.string.id)} ${it.id}) ",
                            onClick = {
                                menuState.hide()
                                onDismiss()
                                Database.asyncTransaction {
                                    lyricsTable.upsert(
                                        Lyrics(
                                            songId = mediaId,
                                            fixed = lyrics?.fixed,
                                            synced = it.syncedLyrics.orEmpty()
                                        )
                                    )
                                }
                            }
                        )
                    }
                    MenuEntry(
                        icon = R.drawable.chevron_back,
                        text = stringResource(R.string.cancel),
                        onClick = { 
                            menuState.hide() 
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

