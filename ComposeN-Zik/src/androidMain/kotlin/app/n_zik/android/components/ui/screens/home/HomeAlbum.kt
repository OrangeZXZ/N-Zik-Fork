package app.n_zik.android.components.ui.screens.home

import app.n_zik.android.uiRoundnessShape

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.size
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.ReorderableItem
import app.kreate.android.themed.rimusic.component.playlist.PositionLock
import app.it.fast4x.rimusic.enums.AlbumSortBy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.n_zik.android.R
import app.it.fast4x.compose.persist.persistList
import app.n_zik.android.core.database.Database
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.colorPalette
import app.it.fast4x.rimusic.enums.AlbumsType
import app.it.fast4x.rimusic.enums.FilterBy
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.models.Album
import app.it.fast4x.rimusic.models.Song
import app.n_zik.android.thumbnailShape
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.ItemSize
import app.it.fast4x.rimusic.ui.components.tab.TabHeader
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Randomizer
import app.n_zik.android.components.menu.album.AlbumItemMenu
import app.it.fast4x.rimusic.ui.components.themed.FilterMenu
import app.it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.it.fast4x.rimusic.ui.components.themed.HeaderIconButton
import app.it.fast4x.rimusic.ui.components.themed.HeaderInfo
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.items.AlbumItem
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.utils.Preference.HOME_ALBUMS_FAVORITES_SORT_BY
import app.it.fast4x.rimusic.utils.Preference.HOME_ALBUMS_FAVORITES_SORT_ORDER
import app.it.fast4x.rimusic.utils.Preference.HOME_ALBUMS_LIBRARY_SORT_BY
import app.it.fast4x.rimusic.utils.Preference.HOME_ALBUMS_LIBRARY_SORT_ORDER
import app.it.fast4x.rimusic.utils.Preference.HOME_ALBUM_ITEM_SIZE
import app.it.fast4x.rimusic.utils.albumTypeKey
import app.it.fast4x.rimusic.utils.autoSyncToolbutton
import app.it.fast4x.rimusic.utils.autosyncKey
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.filterByKey
import app.it.fast4x.rimusic.utils.importYTMLikedAlbums
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import app.n_zik.android.components.Sort
import app.n_zik.android.components.tab.Search
import app.n_zik.android.components.tab.SongShuffler
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.albumPage
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import app.kreate.android.me.knighthat.utils.PropUtils
import app.it.fast4x.rimusic.utils.parseArtists
import app.it.fast4x.rimusic.models.SongAlbumMap
import app.it.fast4x.rimusic.utils.asMediaItem
import kotlinx.coroutines.CoroutineScope
import app.n_zik.android.appContext
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeAlbums(
    navController: NavController,
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Essentials
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    val lazyGridState = rememberLazyGridState()

    // Settings
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    var albumType by rememberPreference(albumTypeKey, AlbumsType.Favorites )

    var items by persistList<Album>( "home/albums/items" )
    var itemsToFilter by persistList<Album>( "home/albums/itemsToFilter" )
    var filterBy by rememberPreference(filterByKey, FilterBy.All)
    val (colorPalette, typography) = LocalAppearance.current

    var itemsOnDisplay by persistList<Album>( "home/albums/on_display" )

    val search = Search(lazyGridState)

    val sort = when( albumType ) {
        AlbumsType.Favorites -> Sort( HOME_ALBUMS_FAVORITES_SORT_BY, HOME_ALBUMS_FAVORITES_SORT_ORDER )
        AlbumsType.Library -> Sort( HOME_ALBUMS_LIBRARY_SORT_BY, HOME_ALBUMS_LIBRARY_SORT_ORDER )
    }
    val positionLock = remember( sort.sortOrder ) { PositionLock(sort.sortOrder) }

    val itemSize = ItemSize.init( HOME_ALBUM_ITEM_SIZE )

    val randomizer = object: Randomizer<Album> {
        override fun getItems(): List<Album> = itemsOnDisplay
        override fun onClick(index: Int) = onAlbumClick( itemsOnDisplay[index] )
    }
    val shuffle = SongShuffler(
        databaseCall = Database.albumTable::allSongsInBookmarked,
        key = arrayOf( albumType )
    )

    val buttonsList = AlbumsType.entries.map { it to it.text }

    if (!isYouTubeSyncEnabled()) {
        filterBy = FilterBy.All
    }

    LaunchedEffect( sort.sortBy, sort.sortOrder, albumType ) {
        when ( albumType ) {
            AlbumsType.Favorites -> Database.albumTable.sortBookmarked( sort.sortBy, sort.sortOrder )
            AlbumsType.Library -> Database.albumTable.sortInLibrary( sort.sortBy, sort.sortOrder )
        }.collect { itemsToFilter = it }
    }
    LaunchedEffect( Unit, itemsToFilter, filterBy ) {
        items = when(filterBy) {
            FilterBy.All -> itemsToFilter
            FilterBy.YoutubeLibrary -> itemsToFilter.filter { it.isYoutubeAlbum }
            FilterBy.Local -> itemsToFilter.filterNot { it.isYoutubeAlbum }
        }

    }
    LaunchedEffect( items, search.inputValue ) {
        itemsOnDisplay = items.filter {
            it.title?.contains( search.inputValue, true) ?: false
                    || it.year?.contains( search.inputValue, true) ?: false
                    || it.authorsText?.contains( search.inputValue, true) ?: false
        }
    }

    LaunchedEffect( Unit ) {
        // TODO Convert to fetch from the internet
        Database.asyncTransaction {
            // Only occurs when album doesn't have thumbnailUrl assigned
            items.filter { it.thumbnailUrl == null }
                 .forEach { album ->
                     /**
                      * Topology:
                      *
                      * Return the most frequently occurring [Song.thumbnailUrl]
                      * among all songs of this album.
                      *
                      * Explanation:
                      *
                      * [Song.thumbnailUrl] can be changed by user.
                      * If 1 song has its thumbnail changed, the result
                      * remains the same because all others have the same url.
                      *
                      * Even when most changed to different urls, it only needs
                      * 2 songs to have the same [Song.thumbnailUrl] to return
                      * the same result.
                      */
                     runBlocking {
                         songAlbumMapTable.allSongsOf( album.id )
                                          .first()
                                          .groupingBy( Song::thumbnailUrl )
                                          .eachCount()
                                          .maxByOrNull { it.value }
                                          ?.key
                     }?.let { albumTable.updateCover( album.id, it ) }
                 }
        }
    }

    val sync = autoSyncToolbutton(R.string.autosync_albums)

    val doAutoSync by rememberPreference(autosyncKey, false)
    var justSynced by rememberSaveable { mutableStateOf(!doAutoSync) }


    var refreshing by remember { mutableStateOf(false) }

    fun refresh() {
        if (refreshing || HomeSyncState.isSyncingAlbums) {
            app.kreate.android.me.knighthat.utils.Toaster.e(appContext().getString(R.string.already_syncing))
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            refreshing = true
            HomeSyncState.isSyncingAlbums = true
            HomeSyncState.albumSyncProgress = 0f
            justSynced = false
            
            val ytAlbums = itemsOnDisplay.filter { it.isYoutubeAlbum || it.id.startsWith("MPRE") || it.id.startsWith("OLAK") }
            val localAlbums = itemsOnDisplay.filterNot { it.isYoutubeAlbum || it.id.startsWith("MPRE") || it.id.startsWith("OLAK") }
            val totalAlbums = ytAlbums.size + localAlbums.size
            
            Timber.tag("HomeAlbum").d("=== REFRESH START === Total: $totalAlbums (YT: ${ytAlbums.size}, Local: ${localAlbums.size})")
            ytAlbums.forEach { Timber.tag("HomeAlbum").d("  [YT] id=${it.id} title=${it.title} isYoutube=${it.isYoutubeAlbum}") }
            localAlbums.forEach { Timber.tag("HomeAlbum").d("  [LOCAL] id=${it.id} title=${it.title} authors=${it.authorsText}") }

            withContext(Dispatchers.Main) {
                if (totalAlbums > 0) app.kreate.android.me.knighthat.utils.Toaster.i(appContext().getString(R.string.refreshing_albums, totalAlbums))
            }
            
            var failedCount = 0
            HomeSyncState.albumSyncFailed = 0
            HomeSyncState.albumSyncTotal = totalAlbums

            ytAlbums.forEachIndexed { index, album ->
                HomeSyncState.albumSyncCurrentIndex = index + 1
                HomeSyncState.albumSyncCurrentName = album.title ?: ""
                HomeSyncState.albumSyncProgress = index.toFloat() / ytAlbums.size
                kotlinx.coroutines.delay((2000L..5000L).random())
                Timber.tag("HomeAlbum").d("[YT] Fetching by ID: ${album.id} for '${album.title}'")
                var status = 0 // 0=retry, 1=success
                for (attempt in 1..3) {
                    YtMusic.getAlbum(album.id, true).onSuccess { online ->
                        val onlineAlbum = online.album
                        Timber.tag("HomeAlbum").d("[YT] Got response for '${album.title}': onlineTitle='${onlineAlbum.title}', thumbnail='${onlineAlbum.thumbnail?.url}', year=${onlineAlbum.year}")
                        val authorsText: String? = onlineAlbum.authors.parseArtists().joinToString(", ")
                        Database.asyncTransaction {
                            albumTable.upsert(Album(
                                id = album.id,
                                title = PropUtils.retainIfModified(album.title, onlineAlbum.title),
                                thumbnailUrl = onlineAlbum.thumbnail?.url ?: album.thumbnailUrl,
                                year = onlineAlbum.year ?: album.year,
                                authorsText = PropUtils.retainIfModified(album.authorsText, authorsText),
                                shareUrl = online.url,
                                timestamp = album.timestamp,
                                bookmarkedAt = album.bookmarkedAt,
                                isYoutubeAlbum = album.isYoutubeAlbum,
                                position = album.position
                            ))
                            online.songs.map { it.asMediaItem }.onEach { insertIgnore(it) }
                                .mapIndexed { pos, mediaItem ->
                                    SongAlbumMap(songId = mediaItem.mediaId, albumId = album.id, position = pos)
                                }.also { songAlbumMapTable.upsert(it) }
                        }
                        Timber.tag("HomeAlbum").d("Successfully refreshed album: ${album.title}")
                        status = 1
                    }.onFailure {
                        Timber.tag("HomeAlbum").e(it, "Failed to refresh album (attempt $attempt): ${album.title}")
                    }
                    if (status != 0) break
                }
                if (status != 1) {
                    failedCount++
                    HomeSyncState.albumSyncFailed = failedCount
                }
            }
            
            localAlbums.forEachIndexed { index, album ->
                HomeSyncState.albumSyncCurrentIndex = ytAlbums.size + index + 1
                HomeSyncState.albumSyncCurrentName = album.title ?: ""
                HomeSyncState.albumSyncProgress = (ytAlbums.size + index).toFloat() / totalAlbums
                val query = "${album.title} ${album.authorsText ?: ""}".trim()
                if (query.isNotBlank()) {
                    kotlinx.coroutines.delay((2000L..5000L).random())
                    Timber.tag("HomeAlbum").d("[LOCAL] Searching YouTube: query='$query' for album id=${album.id}")
                    var status = 0 // 0=retry, 1=success, 2=not found
                    for (attempt in 1..3) {
                        val request = Innertube.searchPage<Innertube.AlbumItem>(
                            body = SearchBody(query = query, params = Innertube.SearchFilter.Album.value),
                            fromMusicShelfRendererContent = { content -> Innertube.AlbumItem.from(content) }
                        )
                        if (request == null) {
                            Timber.tag("HomeAlbum").d("[LOCAL] Search returned null for '$query'")
                            status = 2
                            break
                        }
                        request.onSuccess { searchResult ->
                            val resultCount = searchResult?.items?.size ?: 0
                            Timber.tag("HomeAlbum").d("[LOCAL] Search returned $resultCount results for '$query'")
                            searchResult?.items?.forEachIndexed { i, item ->
                                Timber.tag("HomeAlbum").d("[LOCAL]   Result[$i]: id=${item.key} title='${item.info?.name}' year=${item.year} thumbnail='${item.thumbnail?.url}'")
                            }
                            val localTitle = album.title?.lowercase()?.trim() ?: ""
                            val bestMatch = searchResult?.items?.firstOrNull { result ->
                                val resultId = result.key
                                val resultTitle = result.info?.name?.lowercase()?.trim() ?: return@firstOrNull false
                                val matchById = (album.id.startsWith("MPRE") || album.id.startsWith("OLAK")) && resultId == album.id
                                val matchByTitle = resultTitle == localTitle || resultTitle.contains(localTitle) || localTitle.contains(resultTitle)
                                Timber.tag("HomeAlbum").d("[LOCAL]   Checking result id=$resultId title='${result.info?.name}': matchById=$matchById matchByTitle=$matchByTitle")
                                matchById || matchByTitle
                            }
                            if (bestMatch != null) {
                                Timber.tag("HomeAlbum").d("[LOCAL] MATCH FOUND: '${album.title}' -> '${bestMatch.info?.name}' (id=${bestMatch.key}, thumbnail=${bestMatch.thumbnail?.url}, year=${bestMatch.year})")
                                Database.asyncTransaction {
                                    albumTable.upsert(album.copy(
                                        thumbnailUrl = bestMatch.thumbnail?.url ?: album.thumbnailUrl,
                                        year = bestMatch.year ?: album.year
                                    ))
                                }
                                Timber.tag("HomeAlbum").d("Updated local album '${album.title}' with metadata from '${bestMatch.info?.name}'")
                                status = 1
                            } else {
                                Timber.tag("HomeAlbum").d("No matching album found on YouTube for '${album.title}'")
                                status = 2
                            }
                        }.onFailure {
                            Timber.tag("HomeAlbum").e(it, "Failed to search metadata for local album (attempt $attempt): $query")
                        }
                        if (status != 0) break
                    }
                    if (status != 1) {
                        failedCount++
                        HomeSyncState.albumSyncFailed = failedCount
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                if (failedCount > 0) {
                    app.kreate.android.me.knighthat.utils.Toaster.e(appContext().getString(R.string.failed_albums, failedCount))
                } else if (totalAlbums > 0) {
                    app.kreate.android.me.knighthat.utils.Toaster.s(appContext().getString(R.string.found_all_albums))
                }
            }
            
            refreshing = false
            HomeSyncState.albumSyncProgress = 1f
            HomeSyncState.isSyncingAlbums = false
            Timber.tag("HomeAlbum").d("=== REFRESH END === Total: $totalAlbums, Failed: $failedCount")
        }
    }

    // START: Import YTM subscribed channels
    LaunchedEffect(justSynced, doAutoSync) {
        if (!justSynced && importYTMLikedAlbums())
            justSynced = true
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = ::refresh
    ) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {

            Column( Modifier.fillMaxSize() ) {
                // Sticky tab's title
                TabHeader(R.string.albums) {
                    HeaderInfo(items.size.toString(), R.drawable.album)
                }

                val toolbarButtons = remember { mutableStateListOf<Button>() }

                LaunchedEffect(sort.sortBy, sort.sortOrder) {
                    toolbarButtons.clear()
                    toolbarButtons.add(sort)
                    if (sort.sortBy == AlbumSortBy.Custom)
                        toolbarButtons.add(positionLock)
                    toolbarButtons.add(sync)
                    toolbarButtons.add(search)
                    toolbarButtons.add(randomizer)
                    toolbarButtons.add(shuffle)
                    toolbarButtons.add(itemSize)
                }

                TabToolBar.Buttons( toolbarButtons )

                val hapticFeedback = LocalHapticFeedback.current
                val reorderableLazyGridState = rememberReorderableLazyGridState(
                    lazyGridState = lazyGridState
                ) { from, to ->
                    val mutableItems = itemsOnDisplay.toMutableList()
                    val fromIndex = mutableItems.indexOfFirst { it.id == from.key }
                    val toIndex = mutableItems.indexOfFirst { it.id == to.key }

                    if (fromIndex != -1 && toIndex != -1) {
                        val movedItem = mutableItems.removeAt(fromIndex)
                        mutableItems.add(toIndex, movedItem)
                        itemsOnDisplay = mutableItems
                    }
                }


                // Sticky search bar
                search.SearchBar( this )

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = GridCells.Adaptive( itemSize.size.dp ),
                    modifier = Modifier.background( colorPalette().background0 )
                                       .fillMaxSize(),
                    contentPadding = PaddingValues( bottom = Dimensions.bottomSpacer )
                ) {
                    item(
                        key = "separator",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Column {
                            Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Box {
                                ButtonsRow(
                                    chips = buttonsList,
                                    currentValue = albumType,
                                    onValueUpdate = { albumType = it },
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                if (isYouTubeSyncEnabled()) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                    ) {
                                        BasicText(
                                            text = when (filterBy) {
                                                FilterBy.All -> stringResource(R.string.all)
                                                FilterBy.Local -> stringResource(R.string.on_device)
                                                FilterBy.YoutubeLibrary -> stringResource(R.string.ytm_library)
                                            },
                                            style = typography.xs.semiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .padding(end = 5.dp)
                                                .clip(uiRoundnessShape()).clickable {
                                                    menuState.display {
                                                        FilterMenu(
                                                            title = stringResource(R.string.filter_by),
                                                            onDismiss = menuState::hide,
                                                            onAll = { filterBy = FilterBy.All },
                                                            onYoutubeLibrary = {
                                                                filterBy = FilterBy.YoutubeLibrary
                                                            },
                                                            onLocal = { filterBy = FilterBy.Local }
                                                        )
                                                    }

                                                }
                                        )
                                        HeaderIconButton(
                                            icon = R.drawable.playlist,
                                            color = colorPalette.text,
                                            onClick = {},
                                            modifier = Modifier
                                                .offset(0.dp, 2.5.dp)
                                                .clip(uiRoundnessShape()).clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {}
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        if (HomeSyncState.isSyncingAlbums) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    BasicText(
                                        text = stringResource(R.string.syncing_item, HomeSyncState.albumSyncCurrentName),
                                        style = typography.xxs.semiBold.copy(color = colorPalette.textSecondary),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp).basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                    Row {
                                        BasicText(
                                            text = stringResource(R.string.syncing_progress, HomeSyncState.albumSyncCurrentIndex, HomeSyncState.albumSyncTotal),
                                            style = typography.xxs.semiBold.copy(color = colorPalette.textSecondary)
                                        )
                                        if (HomeSyncState.albumSyncFailed > 0) {
                                            BasicText(
                                                text = " " + stringResource(R.string.syncing_failed, HomeSyncState.albumSyncFailed),
                                                style = typography.xxs.semiBold.copy(color = colorPalette.red)
                                            )
                                        }
                                    }
                                }
                                androidx.compose.material3.LinearWavyProgressIndicator(
                                    progress = { HomeSyncState.albumSyncProgress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    color = colorPalette.accent,
                                    trackColor = colorPalette.background2
                                )
                            }
                        }
                        }
                    }
                    items(
                        items = itemsOnDisplay.distinctBy { it.id },
                        key = { it.id }
                    ) { album ->
                        ReorderableItem(
                            reorderableLazyGridState,
                            key = album.id
                        ) { isDraggingItem ->
                            Box(modifier = Modifier) {
                                if (!positionLock.isLocked() && sort.sortBy == AlbumSortBy.Custom && sort.sortOrder == SortOrder.Ascending) {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                            .zIndex(2f)
                                            .draggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    val currentItems = itemsOnDisplay.toList()
                                                    Database.asyncTransaction {
                                                        currentItems.forEachIndexed { index, album ->
                                                            albumTable.updatePosition(album.id, index)
                                                        }
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.reorder),
                                            contentDescription = null,
                                            tint = if (isDraggingItem) colorPalette().accent else colorPalette().textDisabled
                                        )
                                    }
                                }

                                val songs by remember {
                                    Database.songAlbumMapTable
                                            .allSongsOf( album.id )
                                            .distinctUntilChanged()
                                }.collectAsState( emptyList(), Dispatchers.IO )

                        var position by remember {
                            mutableIntStateOf(0)
                        }
                        val context = LocalContext.current

                        AlbumItem(
                            alternative = true,
                            showAuthors = true,
                            album = album,
                            thumbnailSizeDp = itemSize.size.dp,
                            thumbnailSizePx = itemSize.size.px,
                            modifier = Modifier
                                .clip(uiRoundnessShape()).combinedClickable(

                                    onLongClick = {
                                        menuState.display {
                                            AlbumItemMenu(
                                                navController = navController,
                                                album = album,
                                                songs = songs,
                                                binder = binder
                                            ).MenuComponent()
                                        }
                                    },
                                    onClick = {
                                        search.hideIfEmpty()
                                        onAlbumClick( album )
                                    }
                                )
                                .clip(thumbnailShape()),
                            disableScrollingText = disableScrollingText,
                            isYoutubeAlbum = album.isYoutubeAlbum
                        )
                            }
                        }
                    }
                }

                if (itemsOnDisplay.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = 47.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = stringResource(R.string.no_items),
                            style = typography.m.semiBold.copy(
                                color = colorPalette().textSecondary
                            )
                        )
                    }
                }
            }
            }

            FloatingActionsContainerWithScrollToTop( lazyGridState )

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if ( UiType.ViMusic.isCurrent() && showFloatingIcon )
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )
        }
    }
}







