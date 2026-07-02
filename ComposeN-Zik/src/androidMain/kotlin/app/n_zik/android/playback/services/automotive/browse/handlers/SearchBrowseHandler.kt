package app.n_zik.android.playback.services.automotive.browse.handlers

import it.fast4x.innertube.utils.from

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.parseArtists
import app.n_zik.android.core.database.Database
import app.n_zik.android.download.utils.MyDownloadHelper
import app.n_zik.android.playback.services.automotive.models.SessionMediaItemMapper
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants
import app.n_zik.android.playback.services.PlayerServiceModern
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.browsableMediaItem
import app.n_zik.android.playback.services.automotive.models.AutoSearchState
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.ContinuationBody
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage

class SearchBrowseHandler : BrowseHandler {
    override fun handles(parentId: String): Boolean = parentId.startsWith("SEARCH_") ||
            parentId.startsWith("ID_SEARCH_")

    override suspend fun getChildren(
        parentId: String,
        context: Context,
        database: Database,
        downloadHelper: MyDownloadHelper,
        binder: PlayerServiceModern.Binder?
    ): List<MediaItem> {
        val parts = parentId.split("/")
        val actualParentId = parts[0]
        
        return when (actualParentId) {
            AutoSessionConstants.ID_SEARCH_SONGS -> {
                val allMapped = mutableListOf<MediaItem>()
                var cont: String? = null
                do {
                    val resultPage = if (cont == null) {
                        Innertube.searchPage<Innertube.SongItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Song.value), { content -> Innertube.SongItem.from(content) })?.getOrNull()
                    } else {
                        Innertube.searchPage<Innertube.SongItem>(ContinuationBody(continuation = cont), { content -> Innertube.SongItem.from(content) })?.getOrNull()
                    }
                    val songs = resultPage?.items?.map { s -> s.asSong } ?: emptyList()
                    AutoSearchState.searchedSongs = (AutoSearchState.searchedSongs + songs).distinctBy { s -> s.id }
                    allMapped.addAll(songs.map { s -> SessionMediaItemMapper.mapSongToMediaItem(s, actualParentId) })
                    cont = resultPage?.continuation
                } while (cont != null && allMapped.size < 150)
                allMapped
            }
            AutoSessionConstants.ID_SEARCH_ARTISTS -> {
                val allMapped = mutableListOf<MediaItem>()
                var cont: String? = null
                do {
                    val resultPage = if (cont == null) {
                        Innertube.searchPage<Innertube.ArtistItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Artist.value), { content -> Innertube.ArtistItem.from(content) })?.getOrNull()
                    } else {
                        Innertube.searchPage<Innertube.ArtistItem>(ContinuationBody(continuation = cont), { content -> Innertube.ArtistItem.from(content) })?.getOrNull()
                    }
                    val items = resultPage?.items ?: emptyList()
                    AutoSearchState.searchedArtists = (AutoSearchState.searchedArtists + items).distinctBy { it.key }
                    allMapped.addAll(items.map { ai -> SessionMediaItemMapper.mapArtistToMediaItem(PlayerServiceModern.ARTIST, ai.key ?: "", ai.info?.name ?: "", ai.thumbnail?.url, ai.subscribersCountText, actualParentId) })
                    cont = resultPage?.continuation
                } while (cont != null && allMapped.size < 150)
                allMapped
            }
            AutoSessionConstants.ID_SEARCH_ALBUMS -> {
                val allMapped = mutableListOf<MediaItem>()
                var cont: String? = null
                do {
                    val resultPage = if (cont == null) {
                        Innertube.searchPage<Innertube.AlbumItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Album.value), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                    } else {
                        Innertube.searchPage<Innertube.AlbumItem>(ContinuationBody(continuation = cont), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                    }
                    val items = resultPage?.items ?: emptyList()
                    AutoSearchState.searchedAlbums = (AutoSearchState.searchedAlbums + items).distinctBy { it.key }
                    allMapped.addAll(items.map { ali -> SessionMediaItemMapper.mapAlbumToMediaItem(PlayerServiceModern.ALBUM, ali.key ?: "", ali.info?.name ?: "", ali.authors.parseArtists().joinToString(", "), ali.thumbnail?.url, actualParentId) })
                    cont = resultPage?.continuation
                } while (cont != null && allMapped.size < 150)
                allMapped
            }
            AutoSessionConstants.ID_SEARCH_VIDEOS -> {
                val allMapped = mutableListOf<MediaItem>()
                var cont: String? = null
                do {
                    val resultPage = if (cont == null) {
                        Innertube.searchPage<Innertube.VideoItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Video.value), { content -> Innertube.VideoItem.from(content) })?.getOrNull()
                    } else {
                        Innertube.searchPage<Innertube.VideoItem>(ContinuationBody(continuation = cont), { content -> Innertube.VideoItem.from(content) })?.getOrNull()
                    }
                    val items = resultPage?.items ?: emptyList()
                    val songs = items.map { it.asSong }
                    AutoSearchState.searchedVideos = (AutoSearchState.searchedVideos + items).distinctBy { it.key }
                    allMapped.addAll(songs.map { s -> SessionMediaItemMapper.mapSongToMediaItem(s, actualParentId) })
                    cont = resultPage?.continuation
                } while (cont != null && allMapped.size < 150)
                allMapped
            }
            AutoSessionConstants.ID_SEARCH_PLAYLISTS -> {
                val allMapped = mutableListOf<MediaItem>()
                var cont: String? = null
                do {
                    val resultPage = if (cont == null) {
                        Innertube.searchPage<Innertube.PlaylistItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.CommunityPlaylist.value), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                    } else {
                        Innertube.searchPage<Innertube.PlaylistItem>(ContinuationBody(continuation = cont), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                    }
                    val items = resultPage?.items ?: emptyList()
                    allMapped.addAll(items.map { pi -> browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${pi.key}", pi.info?.name ?: "", null, pi.thumbnail?.url?.toUri(), MediaMetadata.MEDIA_TYPE_PLAYLIST, actualParentId) })
                    cont = resultPage?.continuation
                } while (cont != null && allMapped.size < 150)
                allMapped
            }
            AutoSessionConstants.ID_SEARCH_FEATURED -> {
                val allMapped = mutableListOf<MediaItem>()
                var cont: String? = null
                do {
                    val resultPage = if (cont == null) {
                        Innertube.searchPage<Innertube.PlaylistItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.FeaturedPlaylist.value), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                    } else {
                        Innertube.searchPage<Innertube.PlaylistItem>(ContinuationBody(continuation = cont), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                    }
                    val items = resultPage?.items ?: emptyList()
                    allMapped.addAll(items.map { pi -> browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${pi.key}", pi.info?.name ?: "", null, pi.thumbnail?.url?.toUri(), MediaMetadata.MEDIA_TYPE_PLAYLIST, actualParentId) })
                    cont = resultPage?.continuation
                } while (cont != null && allMapped.size < 150)
                allMapped
            }
            AutoSessionConstants.ID_SEARCH_PODCASTS -> {
                val allMapped = mutableListOf<MediaItem>()
                var cont: String? = null
                do {
                    val resultPage = if (cont == null) {
                        Innertube.searchPage<Innertube.AlbumItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Podcast.value), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                    } else {
                        Innertube.searchPage<Innertube.AlbumItem>(ContinuationBody(continuation = cont), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                    }
                    val items = resultPage?.items ?: emptyList()
                    AutoSearchState.searchedAlbums = (AutoSearchState.searchedAlbums + items).distinctBy { it.key }
                    allMapped.addAll(items.map { ali -> SessionMediaItemMapper.mapAlbumToMediaItem(PlayerServiceModern.ALBUM, ali.key ?: "", ali.info?.name ?: "", ali.authors.parseArtists().joinToString(", "), ali.thumbnail?.url, actualParentId) })
                    cont = resultPage?.continuation
                } while (cont != null && allMapped.size < 150)
                allMapped
            }
            else -> emptyList()
        }
    }
}
