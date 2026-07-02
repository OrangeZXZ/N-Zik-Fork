package app.n_zik.android.components.ui.screens.home.quickpicks

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.compose.persist.persistList
import app.it.fast4x.rimusic.EXPLICIT_PREFIX
import app.it.fast4x.rimusic.MONTHLY_PREFIX
import app.it.fast4x.rimusic.enums.*
import app.it.fast4x.rimusic.models.Artist
import app.it.fast4x.rimusic.models.PlaylistPreview
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.px
import app.it.fast4x.rimusic.utils.*
import app.n_zik.android.LocalPlayerAwareWindowInsets
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.core.database.Database
import app.n_zik.android.typography
import app.n_zik.android.playback.utils.Shuffler
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.QueueBody
import it.fast4x.innertube.requests.queue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun HomeQuickPicks(
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMoodClick: (mood: Innertube.Mood.Item) -> Unit,
    onChipClick: (chip: Innertube.Chip) -> Unit,
    onSettingsClick: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    var playEventType by rememberPreference(playEventsTypeKey, PlayEventsType.MostPlayed)
    var selectedCountryCode by rememberPreference(selectedCountryCodeKey, Countries.ZZ)
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val localRecommandationsNumber by rememberPreference(
        key = "LocalRecommandationsNumber",
        defaultValue = LocalRecommandationsNumber.SixQ
    )
    val localCount = localRecommandationsNumber.value

    val state = rememberHomeQuickPicksState(
        playEventType = playEventType,
        selectedCountryCode = selectedCountryCode,
        parentalControlEnabled = parentalControlEnabled,
        localCount = localCount
    )

    var lastPlayEventType by rememberSaveable { mutableStateOf(playEventType) }
    var lastSelectedCountry by rememberSaveable { mutableStateOf(selectedCountryCode) }
    var lastYouTubeLoggedIn by rememberSaveable { mutableStateOf(isYouTubeLoggedIn()) }

    LaunchedEffect(playEventType, selectedCountryCode) {
        if (playEventType != lastPlayEventType || selectedCountryCode != lastSelectedCountry) {
            state.loadedQuickPicks.value = false
            state.loadedData.value = false
            state.relatedPageResult.value = null
            state.trending.value = null
            state.trendingList.value = emptyList()
            delay(100)
            lastPlayEventType = playEventType
            lastSelectedCountry = selectedCountryCode
        }
        state.loadData()
    }

    val currentYouTubeLoggedIn = isYouTubeLoggedIn()
    LaunchedEffect(currentYouTubeLoggedIn) {
        if (currentYouTubeLoggedIn != lastYouTubeLoggedIn) {
            lastYouTubeLoggedIn = currentYouTubeLoggedIn
            state.homePageResult.value = null
            state.homePageInit.value = null
            state.discoverPageResult.value = null
            state.discoverPageInit.value = null
            state.chartsPageResult.value = null
            state.chartsPageInit.value = null
            state.ytmQuickPicks.value = emptyList()
            state.loadedQuickPicks.value = false
            state.loadedData.value = false
            state.loadData()
            Timber.tag("HomeQuickPicks").d("YouTube login state changed. Data cleared.")
        }
    }

    LaunchedEffect(state.loadedData.value) {
        if (state.loadedData.value) {
            val itemsToFetch = state.homePageInit.value?.sections?.flatMap { it.items }
                ?.filterIsInstance<Innertube.VideoItem>()
                ?.filter { it.durationText == null }
                ?.map { it.key }
                ?.distinct()
                ?: emptyList()
            if (itemsToFetch.isNotEmpty()) {
                Innertube.queue(QueueBody(videoIds = itemsToFetch))?.onSuccess { queueItems ->
                    val durationsMap = queueItems?.associate { it.key to it.durationText }.orEmpty()
                    if (durationsMap.isNotEmpty()) {
                        state.homePageInit.value = state.homePageInit.value?.copy(
                            sections = state.homePageInit.value!!.sections.map { section ->
                                section.copy(
                                    items = section.items.map { item ->
                                        when (item) {
                                            is Innertube.VideoItem -> {
                                                val duration = durationsMap[item.key]
                                                if (duration != null) item.copy(durationText = duration)
                                                else item
                                            }
                                            is Innertube.SongItem -> {
                                                val duration = durationsMap[item.key]
                                                if (duration != null) item.copy(durationText = duration)
                                                else item
                                            }
                                            else -> item
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(state.trendingList.value, state.relatedPageResult.value, localCount, playEventType, state.ytmQuickPicks.value) {
        val relatedInit = state.relatedPageResult.value?.getOrNull()
        val mainIds = state.trendingList.value.map { it.id }.toSet()
        val seed = (state.trendingList.value.joinToString { it.id } + (relatedInit?.songs?.joinToString { it.key } ?: "")).hashCode()
        val random = Random(seed)

        val relatedSongsSource = relatedInit?.songs
            ?.map { it.asSong }
            ?.filter { !parentalControlEnabled || !it.title.startsWith(EXPLICIT_PREFIX, true) }
            ?.distinctBy { it.id }
            .orEmpty()

        val candidateList = if (playEventType == PlayEventsType.MostPlayed || playEventType == PlayEventsType.LastPlayed) {
            val first = state.trendingList.value.firstOrNull()
            val others = state.trendingList.value.drop(1)
            val pool = (others + state.ytmQuickPicks.value + relatedSongsSource).distinctBy { it.id }
            (listOfNotNull(first) + pool.shuffled(random))
        } else {
            val locals = state.trendingList.value.take(localCount)
            val pool = (locals + state.ytmQuickPicks.value + relatedSongsSource).distinctBy { it.id }
            pool.shuffled(random)
        }

        val finalLocalCount = candidateList.count { it.id in mainIds }
        val finalYtmQuickPicksCount = candidateList.count { song -> state.ytmQuickPicks.value.any { it.id == song.id } && song.id !in mainIds }
        val finalRelatedCount = candidateList.size - finalLocalCount - finalYtmQuickPicksCount

        Timber.tag("HomeQuickPicks").d("Assembling Quick Picks -> Local: $finalLocalCount, YTM Related: $finalRelatedCount, YouTube QuickPicks: $finalYtmQuickPicksCount (Total: ${candidateList.size})")

        val oldIds = state.recommendations.value.map { it.id }.toSet()
        val newIds = candidateList.map { it.id }.toSet()

        if (state.recommendations.value.isEmpty() || oldIds != newIds) {
            state.recommendations.value = candidateList
        }
    }

    val scrollState = rememberScrollState()
    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()
    val gridsContentPadding = PaddingValues(start = 12.dp, end = endPaddingValues.calculateEndPadding(LocalLayoutDirection.current))
    val sectionTextModifier = Modifier.padding(horizontal = 12.dp).padding(top = 16.dp, bottom = 8.dp).padding(endPaddingValues)
    val showSearchTab by rememberPreference(showSearchTabKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var showLoader by remember { mutableStateOf(!state.loadedData.value) }
    LaunchedEffect(state.loadedData.value) {
        if (state.loadedData.value) {
            delay(600)
            showLoader = false
        } else {
            showLoader = true
        }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing.value,
        onRefresh = { state.refresh() }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val itemInHorizontalGridWidth = maxWidth * (if (isLandscape && maxWidth * 0.475f >= 320.dp) 0.375f else 0.7f)
            val albumThumbnailSizeDp = 108.dp
            val albumThumbnailSizePx = albumThumbnailSizeDp.px
            val artistThumbnailSizeDp = 92.dp
            val artistThumbnailSizePx = artistThumbnailSizeDp.px
            val playlistThumbnailSizeDp = 108.dp
            val playlistThumbnailSizePx = playlistThumbnailSizeDp.px
            val songThumbnailSizeDp = Dimensions.thumbnails.song
            val songThumbnailSizePx = songThumbnailSizeDp.px

            Column(
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {
                WelcomeMessage()

                if (!state.loadedQuickPicks.value) {
                    Box(modifier = Modifier.fillMaxWidth().height(Dimensions.itemsVerticalPadding * 3 * 9), contentAlignment = Alignment.Center) {
                        LoadingIndicator(color = colorPalette().accent, modifier = Modifier.fillMaxHeight(0.5f).aspectRatio(1f))
                    }
                } else {
                    if (UiType.ViMusic.isCurrent())
                        HeaderWithIcon(
                            title = if (!isYouTubeLoggedIn()) stringResource(R.string.quick_picks) else stringResource(R.string.home),
                            iconId = R.drawable.search,
                            enabled = true,
                            showIcon = !showSearchTab,
                            modifier = Modifier,
                            onClick = onSearchClick
                        )

                    QuickPicksHeader(
                        playEventType = playEventType,
                        onPlayEventTypeChange = { playEventType = it },
                        onDiceClick = {
                            val relatedInit = state.relatedPageResult.value?.getOrNull()
                            val allItems = listOfNotNull(state.trending.value?.asMediaItem) + (relatedInit?.songs?.map { it.asMediaItem } ?: emptyList())
                            binder?.let { Shuffler.play(it, allItems) }
                        },
                        onPlayAllClick = {
                            binder?.stopRadio()
                            state.trending.value?.let { binder?.player?.forcePlay(it.asMediaItem) }
                            val relatedInit = state.relatedPageResult.value?.getOrNull()
                            binder?.player?.addMediaItems(relatedInit?.songs?.map { it.asMediaItem } ?: emptyList())
                        }
                    )

                    QuickPicksGrid(
                        recommendations = state.recommendations.value,
                        trendingList = state.trendingList.value,
                        playEventType = playEventType,
                        itemInHorizontalGridWidth = itemInHorizontalGridWidth,
                        navController = navController,
                        endPaddingValues = endPaddingValues,
                        onSongClick = { binder?.startRadio(it, true) }
                    )
                }

                if (showLoader) {
                    HomeBottomShimmer(albumThumbnailSizeDp, artistThumbnailSizeDp, endPaddingValues)
                } else {
                    val displayedSectionTitles = remember { mutableSetOf<String>() }
                    val ytmSections = state.homePageInit.value?.sections.orEmpty()

                    val artistsState = persistList<Artist>("home/quickpicks/local/artists")
                    val artists by remember { Database.artistTable.sortFollowingByName().distinctUntilChanged() }.collectAsState(artistsState.value, Dispatchers.IO)
                    LaunchedEffect(artists) { artistsState.value = artists }

                    val newReleaseAlbumsFiltered = remember(state.discoverPageInit.value, artists) {
                        state.discoverPageInit.value?.newReleaseAlbums?.filter { album ->
                            artists.any { it.name == album.authors?.firstOrNull()?.name }
                        }.orEmpty()
                    }

                    val monthlyPlaylistsState = persistList<PlaylistPreview>("home/quickpicks/local/monthlyPlaylists")
                    val monthlyPlaylists by remember {
                        Database.playlistTable.allAsPreview().distinctUntilChanged().map { list -> list.filter { it.playlist.name.startsWith(MONTHLY_PREFIX, true) } }
                    }.collectAsState(monthlyPlaylistsState.value, Dispatchers.IO)
                    LaunchedEffect(monthlyPlaylists) { monthlyPlaylistsState.value = monthlyPlaylists }

                    val maxTopPlaylistItems by rememberPreference(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`)
                    val myTopSongsState = persistList<Song>("home/quickpicks/local/myTopSongs")
                    val myTopSongs by remember { Database.eventTable.findSongsMostPlayedBetween(from = 0L, limit = maxTopPlaylistItems.toInt()) }.collectAsState(myTopSongsState.value, Dispatchers.IO)
                    LaunchedEffect(myTopSongs) { myTopSongsState.value = myTopSongs }

                    // 2. Fresh finds ... to New releases (YTM)
                    val firstYtmMerged = listOf(
                        Pair(stringResource(R.string.fresh_finds_old_favorites)) { s: String -> s.contains("Fresh finds", ignoreCase = true) || s.contains("Old favorites", ignoreCase = true) },
                        Pair(stringResource(R.string.mixed_for_you)) { s: String -> s.contains("Mixed for you", ignoreCase = true) },
                        Pair(stringResource(R.string.forgotten_favorites)) { s: String -> s.contains("Forgotten favorites", ignoreCase = true) },
                        Pair(stringResource(R.string.your_daily_discover)) { s: String -> s.contains("Your daily discover", ignoreCase = true) },
                        Pair(stringResource(R.string.fresh_new_music)) { s: String -> s.contains("Fresh new music", ignoreCase = true) },
                        Pair(stringResource(R.string.new_releases)) { s: String -> s.contains("New release", ignoreCase = true) && !s.contains("Fresh new music", ignoreCase = true) }
                    )
                    firstYtmMerged.forEach { (title, predicate) ->
                        YtmSectionByTitle(ytmSections, predicate, title, itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)
                    }

                    // 3. New albums (Discovery)
                    NewAlbumsOfYourArtistsSection(state.discoverPageInit.value, artists, newReleaseAlbumsFiltered, rememberPreference(showNewAlbumsArtistsKey, true).value, onAlbumClick, navController, albumThumbnailSizePx, albumThumbnailSizeDp, disableScrollingText, endPaddingValues, sectionTextModifier)
                    NewAlbumsSection(state.discoverPageInit.value, rememberPreference(showNewAlbumsKey, true).value, onAlbumClick, navController, albumThumbnailSizePx, albumThumbnailSizeDp, disableScrollingText, endPaddingValues, displayedSectionTitles)

                    // 4. Albums for you (YTM)
                    YtmSectionByTitle(ytmSections, { it.contains("Albums for you", ignoreCase = true) }, stringResource(R.string.albums_for_you), itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)

                    // 5. Related albums (Related)
                    RelatedAlbumsSection(state.relatedPageResult.value?.getOrNull(), rememberPreference(showRelatedAlbumsKey, true).value, onAlbumClick, navController, albumThumbnailSizePx, albumThumbnailSizeDp, disableScrollingText, endPaddingValues, sectionTextModifier, displayedSectionTitles)

                    // 6. Monthly Albums (Local)
                    MonthlyPlaylistsSection(rememberPreference(showMonthlyPlaylistInQuickPicksKey, true).value, monthlyPlaylists, navController, endPaddingValues, playlistThumbnailSizeDp, playlistThumbnailSizePx, disableScrollingText)

                    // 7. Show My Top (Local)
                    MyTopSection(rememberPreference(showMyTopPlaylistKey, true).value, myTopSongs, navController, endPaddingValues, sectionTextModifier, itemInHorizontalGridWidth)

                    // 8. Similar artists (Related)
                    SimilarArtistsSection(state.relatedPageResult.value?.getOrNull(), rememberPreference(showSimilarArtistsKey, true).value, onArtistClick, navController, artistThumbnailSizePx, artistThumbnailSizeDp, disableScrollingText, endPaddingValues, sectionTextModifier, displayedSectionTitles)

                    // 9. Today's biggest hits & All hits (YTM)
                    YtmSectionByTitle(ytmSections, { it.contains("Today's biggest hits", ignoreCase = true) }, stringResource(R.string.todays_biggest_hits), itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)
                    YtmSectionByTitle(ytmSections, { it.contains("All hits", ignoreCase = true) }, stringResource(R.string.all_hits), itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)

                    // 10. Playlists you might like (YTM)
                    YtmSectionByTitle(ytmSections, { it.contains("Playlist you might like", ignoreCase = true) }, stringResource(R.string.playlists_you_might_like), itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)

                    // 11. Featured playlists ... to Trending songs for you (YTM)
                    val secondYtmMerged = listOf(
                        Pair(stringResource(R.string.featured_playlists_for_you)) { s: String -> s.contains("Featured playlists", ignoreCase = true) },
                        Pair(stringResource(R.string.trending_community_playlists)) { s: String -> s.contains("Trending community playlists", ignoreCase = true) },
                        Pair(stringResource(R.string.from_the_community)) { s: String -> s.contains("From the community", ignoreCase = true) },
                        Pair(stringResource(R.string.trending_songs_for_you)) { s: String -> s.contains("Trending songs for you", ignoreCase = true) }
                    )
                    secondYtmMerged.forEach { (title, predicate) ->
                        YtmSectionByTitle(ytmSections, predicate, title, itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)
                    }

                    // 12. Top music videos (YTM)
                    YtmSectionByTitle(ytmSections, { it.contains("Top music videos", ignoreCase = true) }, stringResource(R.string.top_music_videos), itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)

                    // 13. Cover and remixes ... to Live performances (YTM)
                    val thirdYtmMerged = listOf(
                        Pair(stringResource(R.string.cover_and_remixes)) { s: String -> s.contains("Cover", ignoreCase = true) || s.contains("remix", ignoreCase = true) },
                        Pair(stringResource(R.string.trending_in_shorts)) { s: String -> s.contains("Trending in Shorts", ignoreCase = true) },
                        Pair(stringResource(R.string.music_videos_for_you)) { s: String -> s.contains("Music videos for you", ignoreCase = true) },
                        Pair(stringResource(R.string.live_performances)) { s: String -> s.contains("Live performances", ignoreCase = true) }
                    )
                    thirdYtmMerged.forEach { (title, predicate) ->
                        YtmSectionByTitle(ytmSections, predicate, title, itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick, displayedSectionTitles)
                    }

                    // 14. Moods & Chips (YTM/Discovery)
                    MoodsSection(state.homePageInit.value, onChipClick, gridsContentPadding, displayedSectionTitles)
                    MoodsAndGenresSection(rememberPreference(showMoodsAndGenresKey, true).value, state.discoverPageInit.value, onMoodClick, navController, gridsContentPadding, displayedSectionTitles)

                    // 15. Generic YTM Sections
                    GenericYtmSections(state.homePageInit.value, displayedSectionTitles, itemInHorizontalGridWidth, albumThumbnailSizePx, albumThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, playlistThumbnailSizePx, playlistThumbnailSizeDp, disableScrollingText, endPaddingValues, navController, onAlbumClick, onArtistClick, onPlaylistClick)

                    // 16. Charts (Charts)
                    ChartsSection(rememberPreference(showChartsKey, true).value, state.chartsPageInit.value, selectedCountryCode, { selectedCountryCode = it }, navController, onPlaylistClick, onArtistClick, endPaddingValues, playlistThumbnailSizePx, playlistThumbnailSizeDp, songThumbnailSizePx, songThumbnailSizeDp, disableScrollingText, parentalControlEnabled, displayedSectionTitles, itemInHorizontalGridWidth)
                }

                if (state.relatedPageResult.value?.exceptionOrNull() != null) {
                    Spacer(modifier = Modifier.height(50.dp))
                    BasicText(text = stringResource(R.string.page_not_been_loaded), style = typography().s.secondary.center, modifier = Modifier.fillMaxWidth().padding(all = 16.dp))
                } else {
                    if (!isYouTubeLoggedIn()) {
                        Spacer(modifier = Modifier.height(50.dp))
                        BasicText(text = stringResource(R.string.log_in_to_ytm), style = typography().s.secondary.center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onSettingsClick))
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
            }

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if (UiType.ViMusic.isCurrent() && showFloatingIcon)
                Box(modifier = Modifier.fillMaxSize()) {
                    MultiFloatingActionsContainer(iconId = R.drawable.search, onClick = onSearchClick, onClickSettings = onSettingsClick, onClickSearch = onSearchClick)
                }
        }
    }
}
