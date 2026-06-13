package app.it.fast4x.rimusic.ui.screens.home

import androidx.compose.ui.draw.clip

import app.n_zik.android.uiRoundnessShape

import app.n_zik.android.core.database.*


import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
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
import app.it.fast4x.rimusic.enums.ArtistSortBy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.n_zik.android.R
import app.it.fast4x.compose.persist.persistList
import it.fast4x.innertube.YtMusic
import app.n_zik.android.core.database.Database
import app.n_zik.android.colorPalette
import app.it.fast4x.rimusic.enums.ArtistsType
import app.it.fast4x.rimusic.enums.FilterBy
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.models.Artist
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.ItemSize
import app.it.fast4x.rimusic.ui.components.tab.TabHeader
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Randomizer
import app.it.fast4x.rimusic.ui.components.themed.FilterMenu
import app.it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.it.fast4x.rimusic.ui.components.themed.HeaderIconButton
import app.it.fast4x.rimusic.ui.components.themed.HeaderInfo
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.items.ArtistItem
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.utils.Preference.HOME_ARTISTS_SORT_BY
import app.it.fast4x.rimusic.utils.Preference.HOME_ARTISTS_SORT_ORDER
import app.it.fast4x.rimusic.utils.Preference.HOME_ARTIST_ITEM_SIZE
import app.it.fast4x.rimusic.utils.artistTypeKey
import app.it.fast4x.rimusic.utils.autoSyncToolbutton
import app.it.fast4x.rimusic.utils.autosyncKey
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.filterByKey
import app.it.fast4x.rimusic.utils.importYTMSubscribedChannels
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.n_zik.android.components.Sort
import app.n_zik.android.components.tab.Search
import app.n_zik.android.components.tab.SongShuffler
import app.n_zik.android.components.menu.artist.LocalArtistItemMenu

@ExperimentalMaterial3Api
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun HomeArtists(
    onArtistClick: (Artist) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Essentials
    val lazyGridState = rememberLazyGridState()
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    // Settings
    var artistType by rememberPreference(artistTypeKey, ArtistsType.Favorites )
    var filterBy by rememberPreference(filterByKey, FilterBy.All)


    var items by persistList<Artist>( "home/artists/items")
    var itemsToFilter by persistList<Artist>( "home/artists/itemsToFilter" )

    var itemsOnDisplay by persistList<Artist>( "home/artists/on_display" )

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val search = Search(lazyGridState)

    val sort = Sort( HOME_ARTISTS_SORT_BY, HOME_ARTISTS_SORT_ORDER )
    val positionLock = remember( sort.sortOrder ) { PositionLock(sort.sortOrder) }

    val itemSize = ItemSize.init( HOME_ARTIST_ITEM_SIZE )

    val randomizer = object: Randomizer<Artist> {
        override fun getItems(): List<Artist> = itemsOnDisplay
        override fun onClick(index: Int) = onArtistClick(itemsOnDisplay[index])

    }
    val shuffle = SongShuffler(
        databaseCall = Database.artistTable::allSongsInFollowing,
        key = arrayOf( artistType )
    )

    val buttonsList = ArtistsType.entries.map { it to it.text }

    if (!isYouTubeSyncEnabled()) {
        filterBy = FilterBy.All
    }

    LaunchedEffect( Unit, sort.sortBy, sort.sortOrder, artistType ) {
        when( artistType ) {
            ArtistsType.Favorites -> Database.artistTable.sortFollowing( sort.sortBy, sort.sortOrder )
            ArtistsType.Library -> Database.artistTable.sortInLibrary( sort.sortBy, sort.sortOrder )
        }.collect { itemsToFilter = it }
    }
    LaunchedEffect( Unit, itemsToFilter, filterBy ) {
        items = when(filterBy) {
            FilterBy.All -> itemsToFilter
            FilterBy.YoutubeLibrary -> itemsToFilter.filter { it.isYoutubeArtist }
            FilterBy.Local -> itemsToFilter.filterNot { it.isYoutubeArtist }
        }

    }
    LaunchedEffect( items, search.inputValue ) {
        itemsOnDisplay = items.filter {
            it.name?.contains( search.inputValue, true ) ?: false
        }
    }
    if (items.any{it.thumbnailUrl == null}) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                items.filter { it.thumbnailUrl == null }.forEach { artist ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val artistThumbnail = YtMusic.getArtistPage(artist.id).getOrNull()?.artist?.thumbnail?.url
                        Database.asyncTransaction {
                            artistTable.update( artist.copy(thumbnailUrl = artistThumbnail) )
                        }
                    }
                }
            }
        }
    }

    val sync = autoSyncToolbutton(R.string.autosync_channels)

    val doAutoSync by rememberPreference(autosyncKey, false)
    var justSynced by rememberSaveable { mutableStateOf(!doAutoSync) }

    var refreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    fun refresh() {
        if (refreshing) return
        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            justSynced = false
            delay(500)
            refreshing = false
        }
    }

    // START: Import YTM subscribed channels
    LaunchedEffect(justSynced, doAutoSync) {
        if (!justSynced && importYTMSubscribedChannels())
                justSynced = true
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = ::refresh
    ) {
        Box (
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {

            Column( Modifier.fillMaxSize() ) {
                // Sticky tab's title
                TabHeader( R.string.artists ) {
                    HeaderInfo(items.size.toString(), R.drawable.people)
                }

                // Sticky tab's tool bar
                val buttonsList = remember(sort.sortBy) {
                    mutableListOf<app.it.fast4x.rimusic.ui.components.tab.toolbar.Button>().apply {
                        add(sort)
                        if (sort.sortBy == app.it.fast4x.rimusic.enums.ArtistSortBy.Custom)
                            add(positionLock)
                        add(sync)
                        add(search)
                        add(randomizer)
                        add(shuffle)
                        add(itemSize)
                    }
                }

                TabToolBar.Buttons( buttonsList )

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
                                    currentValue = artistType,
                                    onValueUpdate = { artistType = it },
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
                                                .clip(uiRoundnessShape()).combinedClickable(
                                                    onClick = {
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
                                        )
                                        HeaderIconButton(
                                            icon = R.drawable.playlist,
                                            color = colorPalette.text,
                                            onClick = {},
                                            modifier = Modifier
                                                .offset(0.dp, 2.5.dp)
                                                .clip(uiRoundnessShape()).combinedClickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {}
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(items = itemsOnDisplay, key = { it.id }) { artist ->
                        ReorderableItem(
                            reorderableLazyGridState,
                            key = artist.id
                        ) { isDraggingItem ->
                            Box(modifier = Modifier) {
                                if (!positionLock.isLocked() && sort.sortBy == app.it.fast4x.rimusic.enums.ArtistSortBy.Custom && sort.sortOrder == app.it.fast4x.rimusic.enums.SortOrder.Ascending) {
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
                                                        currentItems.forEachIndexed { index, artist ->
                                                            artistTable.updatePosition(artist.id, index)
                                                        }
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.Icon(
                                            painter = androidx.compose.ui.res.painterResource(R.drawable.reorder),
                                            contentDescription = null,
                                            tint = if (isDraggingItem) colorPalette().accent else colorPalette().textDisabled
                                        )
                                    }
                                }

                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizeDp = itemSize.size.dp,
                                    thumbnailSizePx = itemSize.size.px,
                                    alternative = true,
                                    modifier = Modifier.clip(uiRoundnessShape()).combinedClickable(
                                        onClick = {
                                            search.hideIfEmpty()
                                            onArtistClick( artist )
                                        },
                                        onLongClick = {
                                            menuState.display { LocalArtistItemMenu(artist = artist).MenuComponent() }
                                        }
                                    ),
                                    disableScrollingText = disableScrollingText,
                                    isYoutubeArtist = artist.isYoutubeArtist
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

            FloatingActionsContainerWithScrollToTop(lazyGridState = lazyGridState)

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if( UiType.ViMusic.isCurrent() && showFloatingIcon )
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )
        }
    }
}









