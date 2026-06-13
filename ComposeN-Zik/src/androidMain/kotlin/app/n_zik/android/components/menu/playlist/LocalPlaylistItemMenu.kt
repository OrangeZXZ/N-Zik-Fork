package app.n_zik.android.components.menu.playlist

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.*
import app.n_zik.android.components.menu.GridMenu
import app.n_zik.android.components.menu.ListMenu
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.core.database.Database
import app.n_zik.android.thumbnailShape
import app.n_zik.android.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import app.it.fast4x.rimusic.models.PlaylistPreview
import app.it.fast4x.rimusic.ui.items.PlaylistItem
import app.it.fast4x.rimusic.enums.NavRoutes

@UnstableApi
@OptIn(ExperimentalFoundationApi::class)
class LocalPlaylistItemMenu private constructor(
    private val navController: NavController,
    private val playlistPreview: PlaylistPreview,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
) : Menu {

    companion object {
        @Composable
        operator fun invoke(navController: NavController, playlistPreview: PlaylistPreview): LocalPlaylistItemMenu =
            LocalPlaylistItemMenu(
                navController = navController,
                playlistPreview = playlistPreview,
                menuState = LocalMenuState.current,
                styleState = rememberPreference(menuStyleKey, MenuStyle.List)
            )
    }

    lateinit var buttons: List<Button>
    override var menuStyle: MenuStyle by styleState

    @Composable
    override fun ListMenu() = ListMenu.Menu {
        buttons.forEach {
            if (it is MenuIcon) it.ListMenuItem()
        }
    }

    @Composable
    override fun GridMenu() = GridMenu.Menu {
        items(buttons, Button::hashCode) {
            if (it is MenuIcon) it.GridMenuItem()
        }
    }

    @Composable
    private fun PlaylistItemDisplay(
        playlistPreview: PlaylistPreview,
        modifier: Modifier = Modifier
    ) {
        val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
        val context = LocalContext.current
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .background(colorPalette().background1)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Arrow Down",
                tint = colorPalette().textSecondary,
                modifier = Modifier.size(24.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = app.it.fast4x.rimusic.ui.styling.Dimensions.itemsVerticalPadding,
                        horizontal = 16.dp
                    )
            ) {
                // Playlist's thumbnail
                Box(
                    Modifier.size(app.it.fast4x.rimusic.ui.styling.Dimensions.thumbnails.album / 2)
                ) {
                    val thumbnails by remember {
                        val customThumbnail = app.it.fast4x.rimusic.utils.checkFileExists( context, "thumbnail/playlist_${playlistPreview.playlist.id}" )

                        if( customThumbnail != null )
                            kotlinx.coroutines.flow.flowOf( listOf( customThumbnail ) )
                        else
                            Database.songPlaylistMapTable
                                    .sortSongsByPlayTime( playlistPreview.playlist.id )
                                    .distinctUntilChanged()
                                    .map { list: List<app.it.fast4x.rimusic.models.Song> ->
                                        list.mapNotNull( app.it.fast4x.rimusic.models.Song::thumbnailUrl ).takeLast( 4 )
                                    }
                    }.collectAsState( emptyList(), kotlinx.coroutines.Dispatchers.IO )

                    if (thumbnails.isEmpty()) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(app.n_zik.android.R.drawable.library),
                            contentDescription = null,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorPalette().textSecondary),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(app.it.fast4x.rimusic.ui.styling.Dimensions.thumbnails.album / 4)
                        )
                    } else if (thumbnails.size == 1) {
                        app.n_zik.android.core.coil.ImageCacheFactory.Thumbnail(
                            thumbnailUrl = thumbnails[0],
                            modifier = Modifier
                                .size(app.it.fast4x.rimusic.ui.styling.Dimensions.thumbnails.album / 2)
                                .clip(thumbnailShape())
                        )
                    } else {
                        // 4 grid
                        Row(modifier = Modifier.size(app.it.fast4x.rimusic.ui.styling.Dimensions.thumbnails.album / 2).clip(thumbnailShape())) {
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                app.n_zik.android.core.coil.ImageCacheFactory.Thumbnail(thumbnailUrl = thumbnails[0], modifier = Modifier.weight(1f).fillMaxWidth())
                                if (thumbnails.size > 2) {
                                    app.n_zik.android.core.coil.ImageCacheFactory.Thumbnail(thumbnailUrl = thumbnails[2], modifier = Modifier.weight(1f).fillMaxWidth())
                                }
                            }
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (thumbnails.size > 1) {
                                    app.n_zik.android.core.coil.ImageCacheFactory.Thumbnail(thumbnailUrl = thumbnails[1], modifier = Modifier.weight(1f).fillMaxWidth())
                                }
                                if (thumbnails.size > 3) {
                                    app.n_zik.android.core.coil.ImageCacheFactory.Thumbnail(thumbnailUrl = thumbnails[3], modifier = Modifier.weight(1f).fillMaxWidth())
                                }
                            }
                        }
                    }
                }

                // Playlist's information
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val disableScrollingText by app.it.fast4x.rimusic.utils.rememberPreference(app.it.fast4x.rimusic.utils.disableScrollingTextKey, false)
                    BasicText(
                        text = playlistPreview.playlist.name,
                        style = typography().xs.semiBold.copy(color = colorPalette().text),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )
                    BasicText(
                        text = "${playlistPreview.songCount} songs",
                        style = typography().xs.semiBold.secondary.copy(color = colorPalette().textSecondary),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )
                }

                // Trailing content
                IconButton(
                    icon = app.n_zik.android.R.drawable.open,
                    color = colorPalette().text,
                    onClick = {
                        menuState.hide()
                        navController.navigate(route = "${app.it.fast4x.rimusic.enums.NavRoutes.localPlaylist.name}/${playlistPreview.playlist.id}")
                    },
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            HorizontalDivider(Modifier.height(1.dp))
        }
    }

    @Composable
    override fun MenuComponent() {
        val context = LocalContext.current
        val binder = LocalPlayerServiceBinder.current
        val coroutineScope = rememberCoroutineScope()
        
        // Options like Play Next, Enqueue, Rename, Delete...
        var showRenameDialog by remember { mutableStateOf(false) }
        
        var songs by remember { mutableStateOf<List<app.it.fast4x.rimusic.models.Song>?>(null) }
        
        LaunchedEffect(playlistPreview.playlist.id) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                songs = Database.songPlaylistMapTable.allSongsOf(playlistPreview.playlist.id).firstOrNull() ?: emptyList()
            }
        }
        
        val downloadAllDialog = app.kreate.android.me.knighthat.component.tab.DownloadAllSongsDialog { songs ?: emptyList() }
        val downloadAll = object : MenuIcon by downloadAllDialog, Descriptive by downloadAllDialog, Clickable {
            override fun onShortClick() {
                if (songs == null) {
                    Toaster.w(R.string.opening_url)
                } else if (songs!!.isNotEmpty()) {
                    downloadAllDialog.onShortClick()
                } else {
                    Toaster.e(R.string.no_song_found)
                }
            }
            override fun onLongClick() {}
        }

        val deleteAllDialog = app.kreate.android.me.knighthat.component.tab.DeleteAllDownloadedSongsDialog { songs ?: emptyList() }
        val deleteAll = object : MenuIcon by deleteAllDialog, Descriptive by deleteAllDialog, Clickable {
            override fun onShortClick() {
                if (songs == null) {
                    Toaster.w(R.string.opening_url)
                } else if (songs!!.isNotEmpty()) {
                    deleteAllDialog.onShortClick()
                } else {
                    Toaster.e(R.string.no_song_found)
                }
            }
            override fun onLongClick() {}
        }

        
        if (showRenameDialog) {
            app.it.fast4x.rimusic.ui.components.themed.InputTextDialog(
                onDismiss = { showRenameDialog = false },
                title = stringResource(R.string.enter_the_playlist_name),
                value = cleanPrefix(playlistPreview.playlist.name),
                placeholder = stringResource(R.string.enter_the_playlist_name),
                setValue = { text ->
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        Database.playlistTable.update(playlistPreview.playlist.copy(name = text))
                    }
                    showRenameDialog = false
                    menuState.hide()
                    Toaster.done()
                }
            )
        }
        
        val playNext = app.it.fast4x.rimusic.ui.components.themed.PlayNext {
            if (songs == null) {
                Toaster.w(R.string.opening_url)
            } else if (songs!!.isNotEmpty()) {
                binder?.player?.addNext(songs!!.map { it.asMediaItem }, context)
                menuState.hide()
            } else {
                Toaster.e(R.string.no_song_found)
            }
        }

        val enqueue = app.it.fast4x.rimusic.ui.components.themed.Enqueue {
            if (songs == null) {
                Toaster.w(R.string.opening_url)
            } else if (songs!!.isNotEmpty()) {
                binder?.player?.enqueue(songs!!.map { it.asMediaItem }, context)
                menuState.hide()
            } else {
                Toaster.e(R.string.no_song_found)
            }
        }

        // Define buttons
        buttons = remember(playlistPreview) {
            val list = mutableListOf<Button>()
            
            list.add(playNext)
            list.add(enqueue)
            list.add(downloadAll)
            list.add(deleteAll)
            
            if (playlistPreview.playlist.isEditable) {
                list.add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.title_edit
                    override val messageId: Int = R.string.rename
                    @get:Composable override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() { showRenameDialog = true }
                    override fun onLongClick() {}
                })
                
                list.add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.trash
                    override val messageId: Int = R.string.delete
                    @get:Composable override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.hide()
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            Database.playlistTable.delete(playlistPreview.playlist)
                        }
                        Toaster.done()
                    }
                    override fun onLongClick() {}
                })
            }
            
            if (playlistPreview.playlist.isYoutubePlaylist) {
                list.add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.play
                    override val messageId: Int = R.string.listen_on_youtube
                    @get:Composable override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.hide()
                        val browseId = playlistPreview.playlist.browseId ?: return
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://music.youtube.com/playlist?list=$browseId")
                        }
                        context.startActivity(intent)
                    }
                    override fun onLongClick() {}
                })
            }

            list
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            downloadAllDialog.Render()
            deleteAllDialog.Render()
            PlaylistItemDisplay(playlistPreview)

            if (menuStyle == MenuStyle.List)
                ListMenu()
            else
                GridMenu()
        }
    }
}

