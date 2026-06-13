package app.kreate.android.me.knighthat.component.menu.album

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
import app.it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.innertube.requests.albumPage
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.utils.*
import app.kreate.android.me.knighthat.component.menu.GridMenu
import app.kreate.android.me.knighthat.component.menu.ListMenu
import app.kreate.android.me.knighthat.utils.Toaster
import app.it.fast4x.rimusic.ui.components.themed.InputTextDialog
import app.kreate.android.me.knighthat.component.tab.DownloadAllSongsDialog
import app.kreate.android.me.knighthat.component.tab.DeleteAllDownloadedSongsDialog
import app.it.fast4x.rimusic.models.Album
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.R
import app.n_zik.android.appContext
import app.n_zik.android.colorPalette
import app.n_zik.android.core.coil.ImageCacheFactory
import app.n_zik.android.core.database.Database
import app.n_zik.android.thumbnailShape
import app.n_zik.android.typography
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@OptIn(ExperimentalFoundationApi::class)
class OnlineAlbumItemMenu private constructor(
    private val navController: NavController,
    private val album: Innertube.AlbumItem,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
) : Menu {

    companion object {
        @Composable
        operator fun invoke(navController: NavController, album: Innertube.AlbumItem): OnlineAlbumItemMenu =
            OnlineAlbumItemMenu(
                navController = navController,
                album = album,
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
    private fun AlbumItemDisplay(
        title: String?,
        authorsText: String?,
        year: String?,
        thumbnailUrl: String?,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

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
                        vertical = Dimensions.itemsVerticalPadding,
                        horizontal = 16.dp
                    )
            ) {
                // Album's thumbnail
                Box(
                    Modifier.size(Dimensions.thumbnails.album / 2)
                ) {
                    ImageCacheFactory.Thumbnail(
                        thumbnailUrl = thumbnailUrl,
                        modifier = Modifier
                            .size(Dimensions.thumbnails.album / 2)
                            .clip(thumbnailShape())
                    )
                }

                // Album's information
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = cleanPrefix(title ?: ""),
                        style = typography().xs.semiBold.copy(
                            color = colorPalette().text,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )

                    authorsText?.let { authors ->
                        if (authors.isNotBlank()) {
                            BasicText(
                                text = cleanPrefix(authors),
                                style = typography().xs.semiBold.secondary.copy(
                                    color = colorPalette().textSecondary,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                            )
                        }
                    }

                    year?.let {
                        if (it.isNotBlank()) {
                            BasicText(
                                text = it,
                                style = typography().xxs.semiBold.secondary.copy(
                                    color = colorPalette().textSecondary,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Trailing content (Bookmark & Share)
                val isBookmarked by remember(album.key) {
                    Database.albumTable
                        .isBookmarked(album.key)
                        .distinctUntilChanged()
                }.collectAsState(false, Dispatchers.IO)

                Column(
                    Modifier.width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        icon = if (isBookmarked) R.drawable.bookmark else R.drawable.bookmark_outline,
                        color = colorPalette().favoritesIcon,
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                Database.albumTable.toggleBookmark(album.key)
                            }
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )

                    IconButton(
                        icon = R.drawable.share_social,
                        color = colorPalette().text,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/browse/${album.key}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )
                }
            }

            HorizontalDivider(Modifier.height(1.dp))
        }
    }

    @Composable
    override fun MenuComponent() {
        val binder = LocalPlayerServiceBinder.current
        val context = LocalContext.current

        var songs by remember { mutableStateOf<List<Song>?>(null) }

        var showChangeTitleDialog by remember { mutableStateOf(false) }
        var showChangeAuthorsDialog by remember { mutableStateOf(false) }
        var showChangeCoverDialog by remember { mutableStateOf(false) }

        var displayTitle by remember { mutableStateOf(album.title ?: album.info?.name) }
        var displayAuthors by remember { mutableStateOf(album.authors?.joinToString(", ") { it.name ?: "" }) }
        var displayYear by remember { mutableStateOf(album.year) }
        var displayThumbnailUrl by remember { mutableStateOf(album.thumbnail?.url) }

        if (showChangeTitleDialog) {
            InputTextDialog(
                onDismiss = { showChangeTitleDialog = false },
                title = stringResource(R.string.update_title),
                value = displayTitle ?: "",
                placeholder = stringResource(R.string.title),
                setValue = { newValue ->
                    CoroutineScope(Dispatchers.IO).launch {
                        Database.albumTable.insertIgnore(Album(
                            id = album.key,
                            title = displayTitle,
                            thumbnailUrl = displayThumbnailUrl,
                            year = displayYear,
                            authorsText = displayAuthors,
                            shareUrl = "https://music.youtube.com/browse/${album.key}",
                            timestamp = System.currentTimeMillis()
                        ))
                        Database.albumTable.updateTitle(album.key, newValue)
                    }
                    showChangeTitleDialog = false
                    menuState.hide()
                }
            )
        }

        if (showChangeAuthorsDialog) {
            InputTextDialog(
                onDismiss = { showChangeAuthorsDialog = false },
                title = stringResource(R.string.update_authors),
                value = displayAuthors ?: "",
                placeholder = stringResource(R.string.artists),
                setValue = { newValue ->
                    CoroutineScope(Dispatchers.IO).launch {
                        Database.albumTable.insertIgnore(Album(
                            id = album.key,
                            title = displayTitle,
                            thumbnailUrl = displayThumbnailUrl,
                            year = displayYear,
                            authorsText = displayAuthors,
                            shareUrl = "https://music.youtube.com/browse/${album.key}",
                            timestamp = System.currentTimeMillis()
                        ))
                        Database.albumTable.updateAuthors(album.key, newValue)
                    }
                    showChangeAuthorsDialog = false
                    menuState.hide()
                }
            )
        }

        if (showChangeCoverDialog) {
            InputTextDialog(
                onDismiss = { showChangeCoverDialog = false },
                title = stringResource(R.string.update_cover),
                value = displayThumbnailUrl ?: "",
                placeholder = stringResource(R.string.cover),
                setValue = { newValue ->
                    CoroutineScope(Dispatchers.IO).launch {
                        Database.albumTable.insertIgnore(Album(
                            id = album.key,
                            title = displayTitle,
                            thumbnailUrl = displayThumbnailUrl,
                            year = displayYear,
                            authorsText = displayAuthors,
                            shareUrl = "https://music.youtube.com/browse/${album.key}",
                            timestamp = System.currentTimeMillis()
                        ))
                        Database.albumTable.updateCover(album.key, newValue)
                    }
                    showChangeCoverDialog = false
                    menuState.hide()
                }
            )
        }

        LaunchedEffect(album.key) {
            withContext(Dispatchers.IO) {
                val result = it.fast4x.innertube.Innertube.albumPage(it.fast4x.innertube.models.bodies.BrowseBody(browseId = album.key))?.getOrNull()
                if (result != null) {
                    displayTitle = result.title ?: displayTitle
                    displayAuthors = result.authors?.joinToString(", ") { it.name ?: "" } ?: displayAuthors
                    displayYear = result.year ?: displayYear
                    displayThumbnailUrl = result.thumbnail?.url ?: displayThumbnailUrl
                    songs = result.songsPage?.items?.mapNotNull { it.asSong } ?: emptyList()
                    app.n_zik.android.core.database.Database.asyncTransaction {
                        app.n_zik.android.core.database.Database.albumTable.insertIgnore(app.it.fast4x.rimusic.models.Album(
                            id = album.key,
                            title = displayTitle,
                            thumbnailUrl = displayThumbnailUrl,
                            year = displayYear,
                            authorsText = displayAuthors,
                            shareUrl = result.url,
                            timestamp = System.currentTimeMillis()
                        ))
                        songs?.forEachIndexed { index, song ->
                            app.n_zik.android.core.database.Database.insertIgnore(song.asMediaItem)
                            app.n_zik.android.core.database.Database.songAlbumMapTable.upsert(
                                listOf(app.it.fast4x.rimusic.models.SongAlbumMap(songId = song.id, albumId = album.key, position = index))
                            )
                        }
                    }
                } else {
                    songs = emptyList() // Failed to fetch or no songs
                }
            }
        }

        val playNext = app.it.fast4x.rimusic.ui.components.themed.PlayNext {
            if (songs == null) {
                Toaster.w(R.string.opening_url)
            } else if (songs!!.isNotEmpty()) {
                binder?.player?.addNext(songs!!.map { it.asMediaItem }, appContext())
                menuState.hide()
            } else {
                Toaster.e(R.string.no_song_found)
            }
        }

        val enqueue = app.it.fast4x.rimusic.ui.components.themed.Enqueue {
            if (songs == null) {
                Toaster.w(R.string.opening_url)
            } else if (songs!!.isNotEmpty()) {
                binder?.player?.enqueue(songs!!.map { it.asMediaItem }, appContext())
                menuState.hide()
            } else {
                Toaster.e(R.string.no_song_found)
            }
        }

        val playlistsMenu = PlaylistsMenu.init(
            navController = navController,
            mediaItems = { songs?.map { it.asMediaItem } ?: emptyList() },
            onFailure = { _, _ -> },
            finalAction = { menuState.hide() }
        )

        val addToPlaylist = object : MenuIcon by playlistsMenu, Descriptive by playlistsMenu, Clickable {
            override fun onShortClick() {
                if (songs == null) {
                    Toaster.w(R.string.opening_url)
                } else if (songs!!.isNotEmpty()) {
                    playlistsMenu.onShortClick()
                } else {
                    Toaster.e(R.string.no_song_found)
                }
            }
            override fun onLongClick() {}
        }

        val downloadAllDialog = DownloadAllSongsDialog { songs ?: emptyList() }
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

        val deleteAllDialog = DeleteAllDownloadedSongsDialog { songs ?: emptyList() }
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

        val changeTitle = object : MenuIcon, Descriptive, Clickable {
            override val iconId: Int = R.drawable.title_edit
            override val messageId: Int = R.string.update_title
            @get:Composable override val menuIconTitle: String get() = stringResource(messageId)
            override fun onShortClick() { showChangeTitleDialog = true }
            override fun onLongClick() {}
        }

        val changeAuthors = object : MenuIcon, Descriptive, Clickable {
            override val iconId: Int = R.drawable.artists_edit
            override val messageId: Int = R.string.update_authors
            @get:Composable override val menuIconTitle: String get() = stringResource(messageId)
            override fun onShortClick() { showChangeAuthorsDialog = true }
            override fun onLongClick() {}
        }

        val changeCover = object : MenuIcon, Descriptive, Clickable {
            override val iconId: Int = R.drawable.cover_edit
            override val messageId: Int = R.string.update_cover
            @get:Composable override val menuIconTitle: String get() = stringResource(messageId)
            override fun onShortClick() { showChangeCoverDialog = true }
            override fun onLongClick() {}
        }

        buttons = mutableListOf<Button>().apply {
            add(playNext)
            add(enqueue)
            add(addToPlaylist)
            add(downloadAll)
            add(deleteAll)

            album.authors?.forEach { artist ->
                val artistName = artist.name
                val browseId = artist.endpoint?.browseId
                if (!artistName.isNullOrBlank() && !browseId.isNullOrBlank()) {
                    add(object : MenuIcon, Descriptive, Clickable {
                        override val iconId: Int = R.drawable.people
                        override val messageId: Int = R.string.artists
                        @get:Composable override val menuIconTitle: String get() = stringResource(R.string.more_of) + " $artistName"
                        override fun onShortClick() {
                            menuState.hide()
                            val path = "$browseId?params=${artist.endpoint?.params.orEmpty()}"
                            app.it.fast4x.rimusic.enums.NavRoutes.artist.navigateHere(navController, path)
                        }
                        override fun onLongClick() {}
                    })
                }
            }

            add(changeTitle)
            add(changeAuthors)
            add(changeCover)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            downloadAllDialog.Render()
            deleteAllDialog.Render()
            AlbumItemDisplay(
                title = displayTitle,
                authorsText = displayAuthors,
                year = displayYear,
                thumbnailUrl = displayThumbnailUrl
            )

            if (menuStyle == MenuStyle.List)
                ListMenu()
            else
                GridMenu()
        }
    }
}
