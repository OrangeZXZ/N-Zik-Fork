package it.fast4x.innertube

import io.ktor.client.call.body
import it.fast4x.innertube.Innertube.getBestQuality
import it.fast4x.innertube.models.BrowseEndpoint
import it.fast4x.innertube.models.BrowseResponse
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.CreatePlaylistResponse
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.innertube.models.getContinuation
import it.fast4x.innertube.models.oddElements
import it.fast4x.innertube.requests.AlbumPage
import it.fast4x.innertube.requests.ArtistItemsContinuationPage
import it.fast4x.innertube.requests.ArtistItemsPage
import it.fast4x.innertube.requests.ArtistPage
import it.fast4x.innertube.requests.HistoryPage
import it.fast4x.innertube.requests.HomePage
import it.fast4x.innertube.requests.NewReleaseAlbumPage
import it.fast4x.innertube.requests.PlaylistContinuationPage
import it.fast4x.innertube.requests.PlaylistPage
import timber.log.Timber

object YtMusic {

    const val PLAYLIST_SIZE_LIMIT = 5000

    suspend fun createPlaylist(title: String) = runCatching {
        Innertube.createPlaylist(Context.DefaultWeb.client, title).body<CreatePlaylistResponse>().playlistId
    }.onFailure {
        Timber.e(it, "YtMusic: createPlaylist error")
    }

    suspend fun deletePlaylist(playlistId: String) = runCatching {
        Innertube.deletePlaylist(Context.DefaultWeb.client, playlistId)
    }.onFailure {
        Timber.e(it, "YtMusic: deletePlaylist error")
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = runCatching {
        Innertube.renamePlaylist(Context.DefaultWeb.client, playlistId, name)
    }.onFailure {
        Timber.e(it, "YtMusic: renamePlaylist error")
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String) = runCatching {
        Innertube.addToPlaylist(Context.DefaultWeb.client, playlistId, videoId)
    }.onFailure {
        Timber.e(it, "YtMusic: addToPlaylist(single) error")
    }

    suspend fun addToPlaylist(playlistId: String, videoIds: List<String>) = runCatching {
        val requestedVideoIds = videoIds.take(PLAYLIST_SIZE_LIMIT)
        val difference = videoIds.size - requestedVideoIds.size
        if (difference > 0) {
            Timber.w("YtMusic: addToPlaylist warning: only adding (at most) %d ids, (surpassed limit by %d)", PLAYLIST_SIZE_LIMIT, difference)
        }
        Innertube.addToPlaylist(Context.DefaultWeb.client, playlistId, requestedVideoIds)
    }.onFailure {
        Timber.e(it, "YtMusic: addToPlaylist (list of size %d) error", videoIds.size)
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String? = null) = runCatching {
        Timber.d("YtMusic: removeFromPlaylist params: playlistId: %s, videoId: %s, setVideoId: %s", playlistId, videoId, setVideoId)
            Innertube.removeFromPlaylist(Context.DefaultWeb.client, playlistId, videoId, setVideoId)
        }.onFailure {
            Timber.e(it, "YtMusic: removeFromPlaylist error")
        }

    suspend fun addPlaylistToPlaylist(playlistId: String, videoId: String) = runCatching {
        Innertube.addPlaylistToPlaylist(Context.DefaultWeb.client, playlistId, videoId)
    }.onFailure {
        Timber.e(it, "YtMusic: addPlaylistToPlaylist error")
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoIds: List<String?>) = runCatching {
        Innertube.removeFromPlaylist(Context.DefaultWeb.client, playlistId, videoId, setVideoIds)
    }.onFailure {
        Timber.e(it, "YtMusic: removeFromPlaylist (list of size %d) error", setVideoIds.size)
    }

    suspend fun subscribeChannel(channelId: String) = runCatching {
        Timber.d("YtMusic: subscribeChannel channelId: %s", channelId)
        Innertube.subscribeChannel(channelId)
    }.onFailure {
        Timber.e(it, "YtMusic: subscribeChannel error")
    }

    suspend fun unsubscribeChannel(channelId: String) = runCatching {
        Timber.d("YtMusic: unsubscribeChannel channelId: %s", channelId)
        Innertube.unsubscribeChannel(channelId)
    }.onFailure {
        Timber.e(it, "YtMusic: unsubscribeChannel error")
    }

    suspend fun likePlaylistOrAlbum(playlistId: String) = runCatching {
        Timber.d("YtMusic: likePlaylistOrAlbum playlistId: %s", playlistId)
        Innertube.likePlaylistOrAlbum(playlistId)
    }.onFailure {
        Timber.e(it, "YtMusic: likePlaylistOrAlbum error")
    }

    suspend fun removelikePlaylistOrAlbum(playlistId: String) = runCatching {
        Timber.d("YtMusic: removelikePlaylistOrAlbum playlistId: %s", playlistId)
        Innertube.removelikePlaylistOrAlbum(playlistId)
    }.onFailure {
        Timber.e(it, "YtMusic: removelikePlaylistOrAlbum error")
    }

    suspend fun likeVideoOrSong(VideoId: String) = runCatching {
        Timber.d("YtMusic: likeVideoOrSong VideoId: %s", VideoId)
        Innertube.likeVideoOrSong(VideoId)
    }.onFailure {
        Timber.e(it, "YtMusic: likeVideoOrSong error")
    }

    suspend fun removelikeVideoOrSong(VideoId: String) = runCatching {
        Timber.d("YtMusic: removelikeVideoOrSong playlistIdId: %s", VideoId)
        Innertube.removelikeVideoOrSong(VideoId)
    }.onFailure {
        Timber.e(it, "YtMusic: removelikeVideoOrSong error")
    }

    suspend fun getHomePage(setLogin: Boolean = false): Result<HomePage> = runCatching {

        val hl = "en" // Force English to keep section matching simple in HomeQuickPicks
        var response = Innertube.browse(browseId = "FEmusic_home", setLogin = setLogin, hl = hl).body<BrowseResponse>()

        Timber.d("homePage() response sections: %s", response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents)

        val sectionListRender = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer

        var continuation = sectionListRender?.continuations?.getContinuation()

        val chips = sectionListRender?.header?.chipCloudRenderer?.chips?.mapNotNull {
            Innertube.Chip.fromChipCloudChipRenderer(it)
        }

        val sections = sectionListRender?.contents!!
            .mapNotNull { it.musicCarouselShelfRenderer }
            .mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.toMutableList()
        while (continuation != null) {
            Timber.d("gethomePage() continuation before: %s", continuation)
            response = Innertube.browse(continuation = continuation, setLogin = setLogin, hl = "en").body<BrowseResponse>()
            continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
            Timber.d("gethomePage() continuation after: %s", continuation)

            sections += response.continuationContents?.sectionListContinuation?.contents
                ?.mapNotNull { it.musicCarouselShelfRenderer }
                ?.mapNotNull {
                    HomePage.Section.fromMusicCarouselShelfRenderer(it)
                }.orEmpty()

        }
        HomePage( sections = sections, chips = chips )
    }

    suspend fun getQuickPicks(setLogin: Boolean = false): Result<List<Innertube.SongItem>> = runCatching {
        val response = Innertube.browse(browseId = "FEmusic_home", setLogin = setLogin, hl = "en").body<BrowseResponse>()

        val sectionListRender = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer

        sectionListRender?.contents
            ?.mapNotNull { it.musicCarouselShelfRenderer }
            ?.mapNotNull { HomePage.Section.fromMusicCarouselShelfRenderer(it) }
            ?.firstOrNull { it.title.contains("Quick picks", ignoreCase = true) }
            ?.items
            ?.filterIsInstance<Innertube.SongItem>()
            .orEmpty()
    }

    suspend fun getHistory(setLogin: Boolean = false): Result<HistoryPage> = runCatching {

        val response = Innertube.browse(browseId = "FEmusic_history", setLogin = setLogin)
            .body<BrowseResponse>()

        Timber.d("getHistory() response sections: %s", response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents)

        HistoryPage(
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull {
                    it.musicShelfRenderer?.let { musicShelfRenderer ->
                        HistoryPage.fromMusicShelfRenderer(musicShelfRenderer)
                    }
                }
        )

    }

    suspend fun getArtistPage(browseId: String, setLogin: Boolean = false): Result<ArtistPage> = runCatching {
        val response = Innertube.browse(browseId = browseId, setLogin = setLogin).body<BrowseResponse>()
        val sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!

        ArtistPage(
            artist = Innertube.ArtistItem(
                info = Innertube.Info(
                    name = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = browseId,
                        params = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.params
                    )
                ),
                thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
                channelId = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.channelId,
                subscribersCountText = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscriberCountText?.runs?.firstOrNull()?.text,
            ),
            sections = sections,
            description = response.header?.musicImmersiveHeaderRenderer?.description?.runs?.firstOrNull()?.text,
            subscribers = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.longSubscriberCountText?.text,
            listeners = response.header?.musicImmersiveHeaderRenderer?.monthlyListenerCount?.runs?.firstOrNull()?.text
                ?: response.header?.musicVisualHeaderRenderer?.monthlyListenerCount?.runs?.firstOrNull()?.text,
            shuffleEndpoint = response.header?.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
            radioEndpoint = response.header?.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
        )
    }

    suspend fun getArtistItemsPage(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        var response = Innertube.browse(browseId = endpoint.browseId, params = endpoint.params).body<BrowseResponse>()

        var contents = (response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: response.contents?.sectionListRenderer?.contents
            ?: emptyList())

        if (contents.isEmpty()) {
            val tabEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.endpoint?.browseEndpoint
            if (tabEndpoint != null) {
                response = Innertube.browse(browseId = tabEndpoint.browseId ?: endpoint.browseId, params = tabEndpoint.params).body<BrowseResponse>()
                contents = (response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                    ?.tabRenderer?.content?.sectionListRenderer?.contents
                    ?: response.contents?.sectionListRenderer?.contents
                    ?: emptyList())
            }
        }

        val matchingCarousel = contents.firstNotNullOfOrNull { content ->
            content.musicCarouselShelfRenderer?.takeIf { carousel ->
                val moreParams = carousel.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.params
                val requestedParams = endpoint.params
                moreParams != null && requestedParams != null && (
                    moreParams == requestedParams ||
                    (moreParams.length > 50 && requestedParams.length > 50 && moreParams.takeLast(50) == requestedParams.takeLast(50))
                )
            }
        }

        if (matchingCarousel != null) {
            return@runCatching ArtistItemsPage(
                title = matchingCarousel.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                items = matchingCarousel.contents.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { renderer ->
                        ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                    } ?: it.musicResponsiveListItemRenderer?.let { renderer ->
                        ArtistItemsPage.fromMusicResponsiveListItemRenderer(renderer)
                    }
                },
                continuation = null
            )
        }

        val gridRenderer = contents.firstNotNullOfOrNull { it.gridRenderer }
        val musicShelfRenderer = contents.firstNotNullOfOrNull { it.musicShelfRenderer }
        val musicPlaylistShelfRenderer = contents.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }

        when {
            gridRenderer != null -> {
                ArtistItemsPage(
                    title = gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = gridRenderer.items!!.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                            ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                        }
                    },
                    continuation = gridRenderer.continuations?.getContinuation()
                )
            }
            musicShelfRenderer != null -> {
                val headerTitle = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                ArtistItemsPage.fromMusicShelfRenderer(musicShelfRenderer, headerTitle)!!
            }
            musicPlaylistShelfRenderer != null -> {
                ArtistItemsPage(
                    title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = musicPlaylistShelfRenderer.contents?.mapNotNull {
                        it.musicResponsiveListItemRenderer?.let { it1 ->
                            ArtistItemsPage.fromMusicResponsiveListItemRenderer(it1)
                        }
                    }!!,
                    continuation = musicPlaylistShelfRenderer.contents?.lastOrNull()
                        ?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                )
            }
            else -> {
                // Fallback or empty
                ArtistItemsPage(
                    title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = emptyList(),
                    continuation = null
                )
            }
        }
    }.onFailure {
        Timber.e(it, "YtMusic: getArtistItemsPage() error")
    }

    suspend fun getPlaylist(playlistId: String): Result<PlaylistPage> = runCatching {
        val playlistIdChecked = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        Timber.d("YtMusic: getPlaylist playlistId: %s Checked: %s", playlistId, playlistIdChecked)
        val response = Innertube.browse(
            browseId = playlistIdChecked,
            setLogin = true
        ).body<BrowseResponse>()


        if (response.header != null)
            getPlaylistPreviousMode(playlistIdChecked, response)
        else
            getPlaylistNewMode(playlistIdChecked, response)
    }.onFailure {
        Timber.e(it, "YtMusic: getPlaylist error")
    }

    private fun getPlaylistPreviousMode(playlistId: String, response: BrowseResponse): PlaylistPage {
        val header = response.header?.musicDetailHeaderRenderer ?:
            response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer


        val editable = response.header?.musicEditablePlaylistDetailHeaderRenderer != null

        return PlaylistPage(
            playlist = Innertube.PlaylistItem(
                info = Innertube.Info(
                    name = header?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = playlistId,
                    )
                ),
                songCount = 0, //header.secondSubtitle.runs?.firstOrNull()?.text,
                thumbnail = header.thumbnail.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                channel = null,
                isEditable = editable,
//                playEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
//                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
//                    ?.musicPlaylistShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer
//                    ?.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
//                shuffleEndpoint = header.menu.menuRenderer.topLevelButtons?.firstOrNull()?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,
//                radioEndpoint = header.menu.menuRenderer.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,

            ),
            description = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
                ?.description?.musicDescriptionShelfRenderer?.description?.text,
            songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                    it.musicResponsiveListItemRenderer?.let { it1 ->
                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                            it1
                        )
                    }
                }!!,
            songsContinuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.continuations?.getContinuation(),
            continuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.continuations?.getContinuation()
        )
    }

    private fun getPlaylistNewMode(playlistId: String, response: BrowseResponse): PlaylistPage {
        val header = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
            ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer

        val isEditable = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicEditablePlaylistDetailHeaderRenderer != null

        Timber.d("getPlaylist new mode editable: %s", isEditable)

        return PlaylistPage(
            playlist = Innertube.PlaylistItem(
                info = Innertube.Info(
                    name = header?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = playlistId,
                    )
                ),
                songCount = 0,//header.secondSubtitle?.runs?.firstOrNull()?.text,
                thumbnail = response.background?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                channel = null,
                isEditable = isEditable,
//                playEndpoint = header.buttons.getOrNull(1)?.musicPlayButtonRenderer
//                    ?.playNavigationEndpoint?.watchEndpoint,
//                shuffleEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
//                radioEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
            ),
            description = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
                ?.description?.musicDescriptionShelfRenderer?.description?.text,
            songs = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                    it.musicResponsiveListItemRenderer?.let { it1 ->
                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                            it1
                        )
                    }
                }!!,
//            songsContinuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
//                .contents.firstOrNull()?.musicPlaylistShelfRenderer?.continuations?.getContinuation(),
            songsContinuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                .contents.firstOrNull()?.musicPlaylistShelfRenderer?.contents!!.lastOrNull()
                    ?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                ,
            continuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                .continuations?.getContinuation(),
            isEditable = isEditable
        )
    }

    suspend fun getPlaylistContinuation(continuation: String) = runCatching {
        val response = Innertube.browse(
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        Timber.d("YtMusic: getPlaylistContinuation response: %s", response.onResponseReceivedActions?.firstOrNull()
            ?.appendContinuationItemsAction?.continuationItems?.lastOrNull()?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token)

//        response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
//            it.musicResponsiveListItemRenderer?.let { it1 ->
//                PlaylistPage.fromMusicResponsiveListItemRenderer( it1 )
//            }
//        }?.let {
//            PlaylistContinuationPage(
//                songs = it,
//                continuation = response.continuationContents.musicPlaylistShelfContinuation.continuations?.getContinuation()
//            )
//        }

        response.onResponseReceivedActions?.map {
            it.appendContinuationItemsAction?.continuationItems?.mapNotNull { it1 ->
                it1.musicResponsiveListItemRenderer?.let { it2 ->
                    PlaylistPage.fromMusicResponsiveListItemRenderer(
                        it2
                    )
                }
            }
        }?.let {
            it.firstOrNull()?.let { it1 ->
                PlaylistContinuationPage(
                    songs = it1,
                    continuation = response.onResponseReceivedActions.firstOrNull()
                        ?.appendContinuationItemsAction?.continuationItems?.lastOrNull()?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                )
            }
        }

    }.onFailure {
        Timber.e(it, "YtMusic: getPlaylistContinuation error")
    }

    suspend fun getArtistItemsContinuation(continuation: String) = runCatching {
        val response = Innertube.browse(
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        response.onResponseReceivedActions?.map {
            it.appendContinuationItemsAction?.continuationItems?.mapNotNull { it1 ->
                it1.musicResponsiveListItemRenderer?.let { it2 ->
                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(
                        it2
                    )
                }
            }
        }?.let {
            it.firstOrNull()?.let { it1 ->
                ArtistItemsContinuationPage(
                    items = it1,
                    continuation = response.onResponseReceivedActions.firstOrNull()
                        ?.appendContinuationItemsAction?.continuationItems?.lastOrNull()
                        ?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                )
            }
        }

    }.onFailure {
        Timber.e(it, "YtMusic: getArtistItemsContinuation error")
    }

    suspend fun getAlbum(browseId: String, withSongs: Boolean = true): Result<AlbumPage> = runCatching {
        val response = Innertube.browse(browseId = browseId).body<BrowseResponse>()
        val playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!

        AlbumPage(
            album = Innertube.AlbumItem(
                playlistId = playlistId,
                info = Innertube.Info(
                    name = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = browseId,
                    )
                ),
                authors = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.straplineTextOne?.runs?.oddElements()
                    ?.map {
                        Innertube.Info(
                            name = it.text,
                            endpoint = it.navigationEndpoint?.browseEndpoint,
                        )
                    }!!,
                year = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text,
                thumbnail = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
            ),
            songs = if (withSongs) getAlbumSongs(playlistId).getOrThrow() else emptyList(),
            otherVersions = response.contents.twoColumnBrowseResultsRenderer.secondaryContents?.sectionListRenderer?.contents?.getOrNull(
                1
            )?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.map(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                .orEmpty(),
            url = response.microformat.microformatDataRenderer.urlCanonical,
            description = response.contents.twoColumnBrowseResultsRenderer.tabs
                .firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicResponsiveHeaderRenderer
                ?.description
                ?.musicDescriptionShelfRenderer
                ?.description?.text,
        )
    }

    suspend fun getAlbumSongs(playlistId: String): Result<List<Innertube.SongItem>> = runCatching {
        val response = Innertube.browse(browseId = "VL$playlistId").body<BrowseResponse>()

        val contents =
            response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.contents ?:
            response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents

        val songs = contents?.mapNotNull {
            it.musicResponsiveListItemRenderer?.let { it1 -> AlbumPage.getSong(it1) }
        }
        Timber.d("mediaItem getAlbumSongs songs: %s", songs)
        songs!!
    }

}