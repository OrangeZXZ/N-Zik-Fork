package app.n_zik.android.components.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.rimusic.enums.*
import app.it.fast4x.rimusic.models.Playlist
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.TabHeader
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.themed.*
import app.it.fast4x.rimusic.utils.*
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.components.ui.screens.home.onDevice.OnDeviceSong
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.R
import app.n_zik.android.appContext
import app.n_zik.android.colorPalette
import app.n_zik.android.components.tab.*
import app.n_zik.android.core.database.Database
import app.n_zik.android.typography
import app.n_zik.android.utils.getAlbumVersionFromVideoGlobal
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.playlistPage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.O)
@UnstableApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeSongsScreen(navController: NavController ) {
    val binder = LocalPlayerServiceBinder.current
    val lazyListState = rememberLazyListState()

    var builtInPlaylist by rememberPreference( builtInPlaylistKey, BuiltInPlaylist.Favorites )
    var isRecommendationEnabled by remember { mutableStateOf(false) }
    var recommendationCount by remember { mutableStateOf(0) }
    var isRecommendationsLoading by remember { mutableStateOf(false) }

    var showConfirmMatchAllDialog by remember { mutableStateOf(false) }
    var showMatchingProgressDialog by remember { mutableStateOf(false) }
    var cancelMatch by remember { mutableStateOf(false) }
    var matchRunning by remember { mutableStateOf(false) }
    var matchJob by remember { mutableStateOf<Job?>(null) }
    var totalSongsToMatch by remember { mutableStateOf(0) }
    var songsMatched by remember { mutableStateOf(0) }
    var retryMatchMode by remember { mutableStateOf(false) }
    var retryMatchSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    var showMatchResultsDialog by remember { mutableStateOf(false) }
    var matchResultsMatched by remember { mutableStateOf(0) }
    var matchResultsFailed by remember { mutableStateOf(0) }
    var matchResultsMerged by remember { mutableStateOf(0) }
    var matchResultsFailedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var matchRefreshKey by remember { mutableIntStateOf(0) }

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
                coroutineScope.launch(Dispatchers.IO) {
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
                matchRunning = true
            }
        )
    }

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

    if (showMatchingProgressDialog) {
        app.it.fast4x.rimusic.ui.components.themed.InProgressDialog(
            total = totalSongsToMatch,
            done = songsMatched,
            text = stringResource(R.string.matching_songs),
            onDismiss = {
                cancelMatch = true
                showMatchingProgressDialog = false
                matchJob?.cancel()
            }
        )
    }

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
                matchRunning = true
            }} else null,
            onDismiss = { showMatchResultsDialog = false }
        )
    }

    LaunchedEffect(matchRunning) {
        if (!matchRunning) return@LaunchedEffect
        val mergedCounter = java.util.concurrent.atomic.AtomicInteger(0)
        val job = launch(Dispatchers.IO) {
            try {
                val unmatched = if (retryMatchMode && retryMatchSongs.isNotEmpty()) {
                    retryMatchSongs
                } else {
                    itemsOnDisplayState.filter { (it.id.length != 11 || (it.durationText == "00:00" && it.totalPlayTimeMs == 1L)) && !it.id.startsWith(app.n_zik.android.playback.services.LOCAL_KEY_PREFIX) }
                }
                totalSongsToMatch = unmatched.size
                songsMatched = 0

                val jobs = mutableListOf<Job>()
                unmatched.forEachIndexed { index, song ->
                    ensureActive()
                    jobs.add(launch(Dispatchers.IO) {
                        var wasCancelled = false
                        try {
                            if (cancelMatch) return@launch
                            getAlbumVersionFromVideoGlobal(song, mergedCounter)
                        } catch (e: CancellationException) {
                            wasCancelled = true
                            throw e
                        } catch (e: Exception) {
                            Timber.tag("HomeSongsScreen").e(e, "Failed to match song to album version")
                        } finally {
                            if (!wasCancelled) songsMatched++
                        }
                    })
                    delay(800)
                }
                jobs.forEach { it.join() }
            } catch (e: CancellationException) {
            } finally {
                withContext(NonCancellable) {
                    delay(1500)
                    var failedCount = 0
                    val allEntries = Database.importSongTable.getAllEntries()
                    val failedEntries = mutableListOf<app.n_zik.android.core.database.ImportSong>()
                    for (entry in allEntries) {
                        val count = Database.songTable.countById(entry.originalId)
                        val isYouTubeId = entry.originalId.length == 11 && !entry.originalId.startsWith(app.n_zik.android.playback.services.LOCAL_KEY_PREFIX)
                        if (count > 0) {
                            if (isYouTubeId) {
                                val song = Database.songTable.findById(entry.originalId).first()
                                if (song != null && song.durationText == "00:00") {
                                    failedCount++
                                    failedEntries.add(entry)
                                } else {
                                    Database.importSongTable.deleteByOriginalId(entry.originalId)
                                }
                            } else {
                                failedCount++
                                failedEntries.add(entry)
                            }
                        } else {
                            Database.importSongTable.deleteByOriginalId(entry.originalId)
                        }
                    }

                    matchRefreshKey++
                    val matchedCount = maxOf(0, totalSongsToMatch - failedCount)
                    matchResultsMatched = matchedCount
                    matchResultsFailed = failedCount
                    matchResultsMerged = mergedCounter.get()
                    val failedOriginalIds = failedEntries.map { it.originalId }.toSet()
                    val failedSongsList = itemsOnDisplayState.filter { it.id in failedOriginalIds }
                    matchResultsFailedSongs = failedSongsList
                    showMatchResultsDialog = true
                }
                showMatchingProgressDialog = false
                retryMatchMode = false
                retryMatchSongs = emptyList()
                matchRunning = false
                cancelMatch = false
                matchJob = null
            }
        }
        matchJob = job
        job.join()
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
            Timber.tag("HomeSongsScreen").e(throwable, "Failed to add songs to playlist ${preview.playlist.name}")
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
            add( search )
            add( locator )
            add( shuffle )
            add( smartShuffle )
            add( itemSelector )
            add( playNext )
            add( enqueue )
            add( addToFavorite )
            add( addToPlaylist )
            if (builtInPlaylist == BuiltInPlaylist.All || builtInPlaylist == BuiltInPlaylist.Favorites)
                add( importMenu )
            if (builtInPlaylist != BuiltInPlaylist.OnDevice)
                add( exportDialog )
            if (builtInPlaylist != BuiltInPlaylist.OnDevice)
                add( smartTrash )
        }
    }

    Box(
        modifier = Modifier.background( colorPalette().background0 )
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Column( Modifier.fillMaxSize() ) {
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

            TabToolBar.Buttons( buttons )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding( horizontal = 12.dp )
                    .padding( bottom = 8.dp )
                    .fillMaxWidth()
            ) {
                Column {
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

            search.SearchBar( columnScope = this )

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
                onClick = { navController.navigate(NavRoutes.search.name) },
                onClickSettings = { navController.navigate(NavRoutes.settings.name) },
                onClickSearch = { navController.navigate(NavRoutes.search.name) }
            )
    }
}
