package app.kreate.android.themed.rimusic.screen.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.n_zik.android.R
import app.kreate.android.themed.rimusic.screen.home.onDevice.OnDeviceSong
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.appContext
import app.n_zik.android.colorPalette
import app.it.fast4x.rimusic.enums.BuiltInPlaylist
import app.it.fast4x.rimusic.enums.CacheType
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.TabHeader
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.themed.CacheSpaceIndicator
import app.it.fast4x.rimusic.ui.components.themed.Enqueue
import app.it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.it.fast4x.rimusic.ui.components.themed.HeaderInfo
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.components.themed.PlayNext
import app.it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.builtInPlaylistKey
import app.it.fast4x.rimusic.utils.enqueue
import app.it.fast4x.rimusic.utils.Preference
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.enums.SongSortBy
import app.it.fast4x.rimusic.utils.showCachedPlaylistKey
import app.it.fast4x.rimusic.utils.showDownloadedPlaylistKey
import app.it.fast4x.rimusic.utils.showFavoritesPlaylistKey
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import app.it.fast4x.rimusic.utils.showMyTopPlaylistKey
import app.n_zik.android.components.tab.SmartTrash
import app.n_zik.android.components.tab.ImportSongsFromCSV
import app.n_zik.android.components.tab.ItemSelector
import app.n_zik.android.components.tab.LikeComponent
import app.n_zik.android.components.tab.Locator
import app.n_zik.android.components.tab.Search
import app.n_zik.android.components.tab.SongShuffler
import app.n_zik.android.components.tab.SmartShuffle
import timber.log.Timber
import app.it.fast4x.rimusic.utils.showOnDevicePlaylistKey
import app.it.fast4x.rimusic.enums.RecommendationsNumber
import app.it.fast4x.rimusic.utils.recommendationsNumberKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import app.n_zik.android.typography
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.ui.components.themed.TextFieldDialog
import androidx.core.net.toUri
import app.it.fast4x.rimusic.utils.asSong
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.playlistPage
import app.n_zik.android.core.database.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import app.it.fast4x.rimusic.models.Playlist
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.components.tab.ExportSongsToCSVDialog
import app.n_zik.android.utils.getAlbumVersionFromVideoGlobal
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import app.n_zik.android.download.utils.MyDownloadHelper
import kotlin.to
import app.it.fast4x.rimusic.utils.asSong as toSong
@RequiresApi(Build.VERSION_CODES.O)
@UnstableApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeSongsScreen(navController: NavController ) {
    // Essentials
    val binder = LocalPlayerServiceBinder.current
    val lazyListState = rememberLazyListState()

    var builtInPlaylist by rememberPreference( builtInPlaylistKey, BuiltInPlaylist.Favorites )
    var isRecommendationEnabled by remember { mutableStateOf(false) }
    val recommendationsNumber by rememberPreference( recommendationsNumberKey, RecommendationsNumber.Adaptive )
    var recommendationCount by remember { mutableStateOf(0) }
    var isRecommendationsLoading by remember { mutableStateOf(false) }

    // Match dialog state
    var showConfirmMatchAllDialog by remember { mutableStateOf(false) }
    var showMatchingProgressDialog by remember { mutableStateOf(false) }
    var cancelMatch by remember { mutableStateOf(false) }
    var totalSongsToMatch by remember { mutableStateOf(0) }
    var songsMatched by remember { mutableStateOf(0) }
    var retryMatchMode by remember { mutableStateOf(false) }
    var retryMatchSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Match results dialog state
    var showMatchResultsDialog by remember { mutableStateOf(false) }
    var matchResultsMatched by remember { mutableStateOf(0) }
    var matchResultsFailed by remember { mutableStateOf(0) }
    var matchResultsMerged by remember { mutableStateOf(0) }
    var matchResultsFailedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var matchRefreshKey by remember { mutableIntStateOf(0) }

    // Delete dialog state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteDialogTitle by remember { mutableStateOf("") }
    var deleteDialogAction by remember { mutableStateOf({}) }

    val itemsOnDisplayState = remember { mutableStateListOf<Song>() }

    val itemSelector = ItemSelector<Song>()
    fun getSongs() = itemSelector.ifEmpty { itemsOnDisplayState }.toList()
    fun getMediaItems() = getSongs().map( Song::asMediaItem )

    val search = Search(lazyListState)
    val locator = Locator( lazyListState, ::getSongs )
    val import = ImportSongsFromCSV(sourceSuffix = "HOMESONGS", likeImported = builtInPlaylist == BuiltInPlaylist.Favorites, onImportComplete = {
        val prefs = appContext().preferences
        val key = if (builtInPlaylist == BuiltInPlaylist.Favorites) Preference.HOME_SONGS_FAVORITES_SORT_BY.key else Preference.HOME_SONGS_SORT_BY.key
        prefs.edit().putString(key, SongSortBy.Custom.name).apply()
    })
    val importSpotify = app.n_zik.android.components.tab.ImportSongsFromServices.init(source = "SPOTIFY_IMPORT_HOMESONGS", likeImported = builtInPlaylist == BuiltInPlaylist.Favorites, onImportComplete = {
        val prefs = appContext().preferences
        val key = if (builtInPlaylist == BuiltInPlaylist.Favorites) Preference.HOME_SONGS_FAVORITES_SORT_BY.key else Preference.HOME_SONGS_SORT_BY.key
        prefs.edit().putString(key, SongSortBy.Custom.name).apply()
    })
    val importRiplay = app.n_zik.android.components.tab.ImportSongsFromServices.init(source = "RIPLAY_IMPORT_HOMESONGS", likeImported = builtInPlaylist == BuiltInPlaylist.Favorites, onImportComplete = {
        val prefs = appContext().preferences
        val key = if (builtInPlaylist == BuiltInPlaylist.Favorites) Preference.HOME_SONGS_FAVORITES_SORT_BY.key else Preference.HOME_SONGS_SORT_BY.key
        prefs.edit().putString(key, SongSortBy.Custom.name).apply()
    })
    val exportDialog = ExportSongsToCSVDialog(
        playlistBrowseId = "",
        playlistName = builtInPlaylist.name,
        songs = ::getSongs
    )
    val coroutineScope = rememberCoroutineScope()

    var showYouTubeLinkDialog by remember { mutableStateOf(false) }
    if (showYouTubeLinkDialog) {
        app.n_zik.android.components.dialog.YouTubeLinkImportDialog(
            onImport = { urlPlaylistId ->
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val browseId = if (urlPlaylistId.startsWith("VL")) urlPlaylistId else "VL$urlPlaylistId"
                    Innertube.playlistPage(BrowseBody(browseId = browseId))?.getOrNull()?.let { playlistPage ->
                        val playlistName = playlistPage.title ?: appContext().getString(R.string.youtube_playlist)
                        val playlist = Playlist(name = playlistName, browseId = browseId)
                        val playlistRowId = Database.playlistTable.insert(playlist)
                        val isFavoriteTab = builtInPlaylist == BuiltInPlaylist.Favorites
                        val songs = playlistPage.songsPage?.items?.mapNotNull {
                            it.asSong.copy(
                                totalPlayTimeMs = 1L,
                                likedAt = if (isFavoriteTab) System.currentTimeMillis() else null
                            )
                        }
                        if (songs != null) {
                            val basePos = Database.songPlaylistMapTable.getMaxPosition(playlistRowId)
                            Database.asyncTransaction {
                                songs.forEachIndexed { index, song ->
                                    songTable.upsert(listOf(song))
                                    songPlaylistMapTable.mapAtPosition(song.id, playlistRowId, basePos + 1 + index)
                                }
                            }
                            val prefs = appContext().preferences
                            val key = if (builtInPlaylist == BuiltInPlaylist.Favorites) Preference.HOME_SONGS_FAVORITES_SORT_BY.key else Preference.HOME_SONGS_SORT_BY.key
                            prefs.edit().putString(key, SongSortBy.Custom.name).apply()
                            Toaster.done()
                        }
                    }
                }
            },
            onDismiss = { showYouTubeLinkDialog = false }
        )
    }

    // Match confirmation dialog
    if (showConfirmMatchAllDialog) {
        ConfirmationDialog(
            text = stringResource(R.string.match_all_confirmation, getSongs().count { it.id.length != 11 || (it.durationText == "00:00" && it.totalPlayTimeMs == 1L) }),
            onDismiss = { showConfirmMatchAllDialog = false },
            onConfirm = {
                showConfirmMatchAllDialog = false
                retryMatchMode = false
                retryMatchSongs = emptyList()
                showMatchingProgressDialog = true
                cancelMatch = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            text = deleteDialogTitle,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                deleteDialogAction()
            }
        )
    }

    // Match progress dialog
    if (showMatchingProgressDialog) {
        app.it.fast4x.rimusic.ui.components.themed.InProgressDialog(
            total = totalSongsToMatch,
            done = songsMatched,
            text = stringResource(R.string.matching_songs),
            onDismiss = {
                cancelMatch = true
                showMatchingProgressDialog = false
            }
        )
    }

    // Match results dialog
    if (showMatchResultsDialog) {
        app.n_zik.android.components.dialog.MatchResultsDialog(
            matched = matchResultsMatched,
            failed = matchResultsFailed,
            merged = matchResultsMerged,
            failedSongs = matchResultsFailedSongs,
            onRetry = if (matchResultsFailed > 0) {{
                showMatchResultsDialog = false
                retryMatchMode = true
                retryMatchSongs = matchResultsFailedSongs
                showMatchingProgressDialog = true
                cancelMatch = false
            }} else null,
            onDismiss = { showMatchResultsDialog = false }
        )
    }

    // Global match LaunchedEffect
    if (showMatchingProgressDialog && !cancelMatch) {
        LaunchedEffect(showMatchingProgressDialog) {
            withContext(Dispatchers.IO) {
                val unmatched = if (retryMatchMode && retryMatchSongs.isNotEmpty()) {
                    // Retry mode: only match the previously failed songs
                    retryMatchSongs
                } else {
                    // Normal mode: match all unmatched songs
                    itemsOnDisplayState.filter { (it.id.length != 11 || (it.durationText == "00:00" && it.totalPlayTimeMs == 1L)) && !it.id.startsWith(app.n_zik.android.playback.services.LOCAL_KEY_PREFIX) }
                }
                totalSongsToMatch = unmatched.size
                songsMatched = 0
                val mergedCounter = java.util.concurrent.atomic.AtomicInteger(0)

                val jobs = mutableListOf<kotlinx.coroutines.Job>()
                unmatched.forEachIndexed { index, song ->
                    jobs.add(launch(Dispatchers.IO) {
                        try {
                            if (cancelMatch) return@launch
                            getAlbumVersionFromVideoGlobal(song, mergedCounter)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            songsMatched++
                        }
                    })
                    delay(800)
                }
                jobs.forEach { it.join() }

                // Wait for database Flow to emit updated list
                delay(500)

                // Clean up ImportSong entries for matched songs
                withContext(Dispatchers.IO) {
                    for (song in unmatched) {
                        Database.importSongTable.deleteByOriginalId(song.id)
                    }
                }

                // Check for songs that still couldn't be matched
                val stillUnmatched = itemsOnDisplayState.filter {
                    (it.id.length != 11 || (it.durationText == "00:00" && it.totalPlayTimeMs == 1L)) && !it.id.startsWith(app.n_zik.android.playback.services.LOCAL_KEY_PREFIX)
                }

                showMatchingProgressDialog = false
                retryMatchMode = false
                retryMatchSongs = emptyList()
                matchRefreshKey++

                // Show results dialog if there were unmatched songs
                if (unmatched.isNotEmpty()) {
                    matchResultsMatched = unmatched.size - stillUnmatched.size
                    matchResultsFailed = stillUnmatched.size
                    matchResultsMerged = mergedCounter.get()
                    matchResultsFailedSongs = stillUnmatched
                    showMatchResultsDialog = true
                }
            }
        }
    }

    val matchAlbumButton = remember {
        object : app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon,
                 app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive {
            override val iconId: Int = R.drawable.alert
            override val messageId: Int = R.string.match_album_audio_version
            @get:Composable override val menuIconTitle: String get() = stringResource(messageId)
            override fun onShortClick() { showConfirmMatchAllDialog = true }
            override fun onLongClick() { cancelMatch = true; showMatchingProgressDialog = false }
        }
    }

    val importMenu = remember(builtInPlaylist) {
        app.n_zik.android.components.tab.ImportPlaylistsMenu(
            onImportNzik = { import.onShortClick() },
            onImportSpotify = { importSpotify.onShortClick() },
            onImportRiplay = { importRiplay.onShortClick() },
            onImportYoutubeLink = { showYouTubeLinkDialog = true }
        )
    }

    val shuffle = SongShuffler(::getSongs)
    val smartShuffle = SmartShuffle(
        isRecommendationEnabled = { isRecommendationEnabled },
        isRecommendationsLoading = { isRecommendationsLoading },
        onToggleRecommendation = { isRecommendationEnabled = !isRecommendationEnabled }
    )
    val playNext = PlayNext {
        binder?.player?.addNext( getMediaItems(), appContext() )
        itemSelector.isActive = false
    }
    val enqueue = Enqueue {
        binder?.player?.enqueue( getMediaItems(), appContext() )
        itemSelector.isActive = false
    }
    val addToFavorite = LikeComponent(::getSongs)
    val addToPlaylist = PlaylistsMenu.init(
        navController = navController,
        mediaItems = { _ -> getMediaItems() },
        onFailure = { throwable, preview ->
            Timber.e( "Failed to add songs to playlist ${preview.playlist.name} on HomeSongs" )
            throwable.printStackTrace()
        },
        finalAction = { itemSelector.isActive = false }
    )
    val smartTrash = SmartTrash(
        builtInPlaylist = { builtInPlaylist },
        getSongs = ::getSongs,
        itemsOnDisplay = { itemsOnDisplayState }
    )

    val buttons = remember( builtInPlaylist ) {
        itemSelector.isActive = false

        mutableStateListOf<Button>().apply {
            this.add( search )
            this.add( locator )
            this.add( shuffle )
            this.add( smartShuffle )
            this.add( itemSelector )
            this.add( playNext )
            this.add( enqueue )
            this.add( addToFavorite )
            this.add( addToPlaylist )
            // Import only on All and Favorites
            if (builtInPlaylist == BuiltInPlaylist.All || builtInPlaylist == BuiltInPlaylist.Favorites)
                this.add( importMenu )
            // Export on all except OnDevice
            if (builtInPlaylist != BuiltInPlaylist.OnDevice)
                this.add( exportDialog )
            // Smart trash on non-OnDevice
            if (builtInPlaylist != BuiltInPlaylist.OnDevice)
                this.add( smartTrash )
        }
    }

    Box(
        modifier = Modifier.background( colorPalette().background0 )
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Column( Modifier.fillMaxSize() ) {
            // Sticky tab's title
            TabHeader( R.string.songs ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeaderInfo( itemsOnDisplayState.size.toString(), R.drawable.musical_notes )
                    }
                    if (isRecommendationEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.smart_shuffle),
                                contentDescription = null,
                                tint = colorPalette().textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            if (isRecommendationsLoading) {
                                Spacer(modifier = Modifier.width(4.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = colorPalette().textSecondary
                                )
                            } else if (recommendationCount > 0) {
                                BasicText(
                                    text = recommendationCount.toString(),
                                    style = TextStyle(
                                        color = colorPalette().textSecondary,
                                        fontStyle = typography().xxxs.semiBold.fontStyle,
                                        fontWeight = typography().xxxs.semiBold.fontWeight,
                                        fontSize = typography().xxxs.semiBold.fontSize
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding( start = 4.dp )
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                    }
                }
            }

            importMenu.Render()
            exportDialog.Render()
            smartTrash.Render()

            // Sticky tab's tool bar
            TabToolBar.Buttons( buttons )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding( horizontal = 12.dp )
                    .padding( bottom = 8.dp )
                    .fillMaxWidth()
            ) {
                Column {
                    //<editor-fold defaultstate="collapsed" desc="Chips">
                    val showFavoritesPlaylist by rememberPreference( showFavoritesPlaylistKey, true )
                    val showCachedPlaylist by rememberPreference( showCachedPlaylistKey, true )
                    val showMyTopPlaylist by rememberPreference( showMyTopPlaylistKey, true )
                    val showDownloadedPlaylist by rememberPreference( showDownloadedPlaylistKey, true )
                    val showOnDeviceChip by rememberPreference( showOnDevicePlaylistKey, true )
                    val chips = remember( showFavoritesPlaylist, showCachedPlaylist, showMyTopPlaylist, showDownloadedPlaylist) {
                        buildList {
                            add( BuiltInPlaylist.All )
                            if( showFavoritesPlaylist )
                                add( BuiltInPlaylist.Favorites )
                            if( showCachedPlaylist )
                                add( BuiltInPlaylist.Offline )
                            if( showDownloadedPlaylist )
                                add( BuiltInPlaylist.Downloaded )
                            if( showMyTopPlaylist )
                                add( BuiltInPlaylist.Top )
                            if( showOnDeviceChip )
                                add( BuiltInPlaylist.OnDevice )
                        }
                    }
                    //</editor-fold>

                    ButtonsRow(
                        chips = chips,
                        currentValue = builtInPlaylist,
                        onValueUpdate = { builtInPlaylist = it }
                    )

                    when (builtInPlaylist) {
                        BuiltInPlaylist.Downloaded, BuiltInPlaylist.Offline -> {
                            CacheSpaceIndicator(
                                cacheType = when (builtInPlaylist) {
                                    BuiltInPlaylist.Downloaded -> CacheType.DownloadedSongs
                                    BuiltInPlaylist.Offline -> CacheType.CachedSongs
                                    else -> CacheType.CachedSongs
                                }
                            )
                        }
                        else -> {}
                    }

                }
            }

            // Sticky search bar
            search.SearchBar( this )

            when( builtInPlaylist ) {
                BuiltInPlaylist.OnDevice -> OnDeviceSong( navController, lazyListState, itemSelector, search, buttons, itemsOnDisplayState, ::getSongs )
                else                     -> HomeSongs( navController, builtInPlaylist, lazyListState, itemSelector, search, buttons, itemsOnDisplayState, ::getSongs, matchButton = null, onRecommendationCountChange = { count -> recommendationCount = count }, onRecommendationsLoadingChange = { loading -> isRecommendationsLoading = loading }, isRecommendationEnabled = isRecommendationEnabled, refreshKey = matchRefreshKey, onMatchClick = { showConfirmMatchAllDialog = true } )
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

        val showFloatingIcon by rememberPreference( showFloatingIconKey, false )
        if( UiType.ViMusic.isCurrent() && showFloatingIcon )
            MultiFloatingActionsContainer(
                iconId = R.drawable.search,
                onClick = {
                    navController.navigate(NavRoutes.search.name)
                },
                onClickSettings = {
                    navController.navigate(NavRoutes.settings.name)
                },
                onClickSearch = {
                    navController.navigate(NavRoutes.search.name)
                }
            )
    }
}

