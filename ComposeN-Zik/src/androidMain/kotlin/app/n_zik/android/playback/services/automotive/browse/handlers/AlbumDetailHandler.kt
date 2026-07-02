package app.n_zik.android.playback.services.automotive.browse.handlers

import it.fast4x.innertube.requests.albumPage
import app.it.fast4x.rimusic.utils.asSong

import android.content.Context
import androidx.media3.common.MediaItem
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.parseArtists
import app.n_zik.android.core.database.Database
import app.n_zik.android.download.utils.MyDownloadHelper
import app.n_zik.android.playback.services.automotive.models.SessionMediaItemMapper
import app.n_zik.android.playback.services.PlayerServiceModern
import app.n_zik.android.playback.services.automotive.models.AutoSearchState
import it.fast4x.innertube.models.bodies.BrowseBody
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlbumDetailHandler : BrowseHandler {
    override fun handles(parentId: String): Boolean {
        if (!parentId.startsWith("${PlayerServiceModern.ALBUM}/")) return false
        val parts = parentId.split("/")
        return parts.size == 2
    }

    override suspend fun getChildren(
        parentId: String,
        context: Context,
        database: Database,
        downloadHelper: MyDownloadHelper,
        binder: PlayerServiceModern.Binder?
    ): List<MediaItem> {
        val parts = parentId.split("/")
        val actualParentId = parts[0]
        val albumId = parts[1]
        var onlineSongs: List<Song>? = null
        
        val online = it.fast4x.innertube.YtMusic.getAlbum(albumId, true).getOrNull()
        if (online != null) {
            val onlineAlbum = online.album
            val authorsText: String? = onlineAlbum.authors.parseArtists().joinToString(", ")
            onlineSongs = online.songs.map { it.asSong }
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val album = database.albumTable.findById(albumId).first()
                database.asyncTransaction {
                    albumTable.upsert(
                        app.it.fast4x.rimusic.models.Album(
                            id = albumId,
                            title = app.kreate.android.me.knighthat.utils.PropUtils.retainIfModified(album?.title, onlineAlbum.title),
                            thumbnailUrl = app.kreate.android.me.knighthat.utils.PropUtils.retainIfModified(album?.thumbnailUrl, onlineAlbum.thumbnail?.url),
                            year = onlineAlbum.year,
                            authorsText = app.kreate.android.me.knighthat.utils.PropUtils.retainIfModified(album?.authorsText, authorsText),
                            shareUrl = online.url,
                            timestamp = album?.timestamp ?: System.currentTimeMillis(),
                            bookmarkedAt = album?.bookmarkedAt,
                            isYoutubeAlbum = album?.isYoutubeAlbum ?: true,
                            position = album?.position ?: -1
                        )
                    )

                    online.songs.map { it.asMediaItem }.onEach { insertIgnore(it) }
                        .mapIndexed { position, mediaItem ->
                            app.it.fast4x.rimusic.models.SongAlbumMap(
                                songId = mediaItem.mediaId,
                                albumId = albumId,
                                position = position
                            )
                        }
                        .also(songAlbumMapTable::upsert)
                }
            }
        } else {
            val albumPage = it.fast4x.innertube.Innertube.albumPage(BrowseBody(browseId = albumId))?.getOrNull()
            onlineSongs = albumPage?.songsPage?.items?.toList()?.map { item -> item.asSong }
        }
        
        return if (!onlineSongs.isNullOrEmpty()) {
            AutoSearchState.searchedSongs = (AutoSearchState.searchedSongs + onlineSongs).distinctBy { s -> s.id }
            onlineSongs.mapIndexed { index, song ->
                SessionMediaItemMapper.mapSongToMediaItem(song, actualParentId).let { item ->
                    item.buildUpon()
                        .setMediaMetadata(
                            item.mediaMetadata.buildUpon()
                                .setTrackNumber(index + 1)
                                .build()
                        )
                        .build()
                }
            }
        } else {
            val localSongs = database.songAlbumMapTable.allSongsOf(albumId).first()
            localSongs.mapIndexed { index, song ->
                SessionMediaItemMapper.mapSongToMediaItem(song, actualParentId).let { item ->
                    item.buildUpon()
                        .setMediaMetadata(
                            item.mediaMetadata.buildUpon()
                                .setTrackNumber(index + 1)
                                .build()
                        )
                        .build()
                }
            }
        }
    }
}
