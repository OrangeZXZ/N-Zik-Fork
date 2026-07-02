package app.n_zik.android.playback.services.automotive.browse.handlers

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.it.fast4x.rimusic.MONTHLY_PREFIX
import app.it.fast4x.rimusic.PINNED_PREFIX
import app.it.fast4x.rimusic.PIPED_PREFIX
import app.it.fast4x.rimusic.enums.PlaylistSortBy
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.utils.*
import app.n_zik.android.R
import app.n_zik.android.core.database.Database
import app.n_zik.android.download.utils.MyDownloadHelper
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants.ID_PLAYLISTS_LOCAL
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants.ID_PLAYLISTS_MONTHLY
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants.ID_PLAYLISTS_PINNED
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants.ID_PLAYLISTS_PIPED
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants.ID_PLAYLISTS_YT
import app.n_zik.android.playback.services.PlayerServiceModern
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.browsableMediaItem
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.drawableUri
import kotlinx.coroutines.flow.first

class PlaylistsBrowseHandler : BrowseHandler {
    override fun handles(parentId: String): Boolean = parentId == PlayerServiceModern.PLAYLIST ||
            parentId.startsWith("PLAYLISTS_") || parentId.startsWith("ID_PLAYLISTS_")

    override suspend fun getChildren(
        parentId: String,
        context: Context,
        database: Database,
        downloadHelper: MyDownloadHelper,
        binder: PlayerServiceModern.Binder?
    ): List<MediaItem> {
        return when (parentId) {
            PlayerServiceModern.PLAYLIST -> {
                val showMonthlyPlaylists = try { context.preferences.getBoolean(showMonthlyPlaylistsKey, true) } catch (e: Exception) { true }
                val showPipedPlaylists = try { context.preferences.getBoolean(showPipedPlaylistsKey, true) } catch (e: Exception) { true }
                val showPinnedPlaylists = try { context.preferences.getBoolean(showPinnedPlaylistsKey, true) } catch (e: Exception) { true }
                val pinnedCount = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PINNED_PREFIX, true) }.size
                val localCount = database.playlistTable.allAsPreview().first().filter { !it.playlist.isYoutubePlaylist && !it.playlist.name.startsWith(PIPED_PREFIX, true) && !it.playlist.name.startsWith(PINNED_PREFIX, true) && !it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.size
                val ytCount = database.playlistTable.allAsPreview().first().filter { it.playlist.isYoutubePlaylist }.size
                val pipedCount = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PIPED_PREFIX, true) }.size
                val monthlyCount = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.size
                val playlists = mutableListOf<MediaItem>()
                playlists.add(browsableMediaItem(ID_PLAYLISTS_LOCAL, context.getString(R.string.library), localCount.toString(), drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                if (showPinnedPlaylists) {
                    playlists.add(browsableMediaItem(ID_PLAYLISTS_PINNED, context.getString(R.string.pinned_playlists), pinnedCount.toString(), drawableUri(context, R.drawable.pin_filled), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                }
                if (showMonthlyPlaylists) {
                    playlists.add(browsableMediaItem(ID_PLAYLISTS_MONTHLY, context.getString(R.string.monthly_playlists), monthlyCount.toString(), drawableUri(context, R.drawable.calendar), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                }
                if (showPipedPlaylists) {
                    playlists.add(browsableMediaItem(ID_PLAYLISTS_PIPED, context.getString(R.string.piped_playlists), pipedCount.toString(), drawableUri(context, R.drawable.piped_logo), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                }
                playlists.add(browsableMediaItem(ID_PLAYLISTS_YT, context.getString(R.string.ytm_playlists), ytCount.toString(), drawableUri(context, R.drawable.ytmusic), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                playlists
            }
            ID_PLAYLISTS_LOCAL -> {
                val shuffleItem = AutoSessionConstants.shuffleItem(context, AutoSessionConstants.ID_PLAYLISTS_LOCAL_SHUFFLE)
                val sortBy = context.preferences.getEnum(Preference.HOME_LIBRARY_PLAYLIST_SORT_BY.key, PlaylistSortBy.SongCount)
                val sortOrder = context.preferences.getEnum(Preference.HOME_LIBRARY_PLAYLIST_SORT_ORDER.key, SortOrder.Ascending)
                val playlists = database.playlistTable.allAsPreview().first()
                    .filter { !it.playlist.isYoutubePlaylist && !it.playlist.name.startsWith(PIPED_PREFIX, true) && !it.playlist.name.startsWith(PINNED_PREFIX, true) && !it.playlist.name.startsWith(MONTHLY_PREFIX, true) }
                    .let { list ->
                        when (sortBy) {
                            PlaylistSortBy.Name -> list.sortedBy { it.playlist.cleanName() }
                            PlaylistSortBy.SongCount -> list.sortedBy { it.songCount }
                            PlaylistSortBy.DateAdded -> list.sortedByDescending { it.playlist.id }
                            PlaylistSortBy.Custom -> list.sortedBy { it.playlist.position }
                            else -> list.sortedBy { it.songCount }
                        }
                    }
                    .let { list -> if (sortOrder == SortOrder.Descending) list.reversed() else list }
                    .map { preview -> browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                listOf(shuffleItem) + playlists
            }
            ID_PLAYLISTS_YT -> {
                val shuffleItem = AutoSessionConstants.shuffleItem(context, AutoSessionConstants.ID_PLAYLISTS_YT_SHUFFLE)
                val sortBy = context.preferences.getEnum(Preference.HOME_LIBRARY_YT_PLAYLIST_SORT_BY.key, PlaylistSortBy.SongCount)
                val sortOrder = context.preferences.getEnum(Preference.HOME_LIBRARY_YT_PLAYLIST_SORT_ORDER.key, SortOrder.Ascending)
                val playlists = database.playlistTable.allAsPreview().first()
                    .filter { it.playlist.isYoutubePlaylist }
                    .let { list ->
                        when (sortBy) {
                            PlaylistSortBy.Name -> list.sortedBy { it.playlist.cleanName() }
                            PlaylistSortBy.SongCount -> list.sortedBy { it.songCount }
                            PlaylistSortBy.DateAdded -> list.sortedByDescending { it.playlist.id }
                            PlaylistSortBy.Custom -> list.sortedBy { it.playlist.position }
                            else -> list.sortedBy { it.songCount }
                        }
                    }
                    .let { list -> if (sortOrder == SortOrder.Descending) list.reversed() else list }
                    .map { preview -> browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                listOf(shuffleItem) + playlists
            }
            ID_PLAYLISTS_PIPED -> {
                val shuffleItem = AutoSessionConstants.shuffleItem(context, AutoSessionConstants.ID_PLAYLISTS_PIPED_SHUFFLE)
                val sortBy = context.preferences.getEnum(Preference.HOME_LIBRARY_PIPED_PLAYLIST_SORT_BY.key, PlaylistSortBy.SongCount)
                val sortOrder = context.preferences.getEnum(Preference.HOME_LIBRARY_PIPED_PLAYLIST_SORT_ORDER.key, SortOrder.Ascending)
                val playlists = database.playlistTable.allAsPreview().first()
                    .filter { it.playlist.name.startsWith(PIPED_PREFIX, true) }
                    .let { list ->
                        when (sortBy) {
                            PlaylistSortBy.Name -> list.sortedBy { it.playlist.cleanName() }
                            PlaylistSortBy.SongCount -> list.sortedBy { it.songCount }
                            PlaylistSortBy.DateAdded -> list.sortedByDescending { it.playlist.id }
                            PlaylistSortBy.Custom -> list.sortedBy { it.playlist.position }
                            else -> list.sortedBy { it.songCount }
                        }
                    }
                    .let { list -> if (sortOrder == SortOrder.Descending) list.reversed() else list }
                    .map { preview -> browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                listOf(shuffleItem) + playlists
            }
            ID_PLAYLISTS_PINNED -> {
                val shuffleItem = AutoSessionConstants.shuffleItem(context, AutoSessionConstants.ID_PLAYLISTS_PINNED_SHUFFLE)
                val sortBy = context.preferences.getEnum(Preference.HOME_LIBRARY_PINNED_PLAYLIST_SORT_BY.key, PlaylistSortBy.SongCount)
                val sortOrder = context.preferences.getEnum(Preference.HOME_LIBRARY_PINNED_PLAYLIST_SORT_ORDER.key, SortOrder.Ascending)
                val playlists = database.playlistTable.allAsPreview().first()
                    .filter { it.playlist.name.startsWith(PINNED_PREFIX, true) }
                    .let { list ->
                        when (sortBy) {
                            PlaylistSortBy.Name -> list.sortedBy { it.playlist.cleanName() }
                            PlaylistSortBy.SongCount -> list.sortedBy { it.songCount }
                            PlaylistSortBy.DateAdded -> list.sortedByDescending { it.playlist.id }
                            PlaylistSortBy.Custom -> list.sortedBy { it.playlist.position }
                            else -> list.sortedBy { it.songCount }
                        }
                    }
                    .let { list -> if (sortOrder == SortOrder.Descending) list.reversed() else list }
                    .map { preview -> browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                listOf(shuffleItem) + playlists
            }
            ID_PLAYLISTS_MONTHLY -> {
                val shuffleItem = AutoSessionConstants.shuffleItem(context, AutoSessionConstants.ID_PLAYLISTS_MONTHLY_SHUFFLE)
                val sortBy = context.preferences.getEnum(Preference.HOME_LIBRARY_MONTHLY_PLAYLIST_SORT_BY.key, PlaylistSortBy.SongCount)
                val sortOrder = context.preferences.getEnum(Preference.HOME_LIBRARY_MONTHLY_PLAYLIST_SORT_ORDER.key, SortOrder.Ascending)
                val playlists = database.playlistTable.allAsPreview().first()
                    .filter { it.playlist.name.startsWith(MONTHLY_PREFIX, true) }
                    .let { list ->
                        when (sortBy) {
                            PlaylistSortBy.Name -> list.sortedBy { it.playlist.cleanName() }
                            PlaylistSortBy.SongCount -> list.sortedBy { it.songCount }
                            PlaylistSortBy.DateAdded -> list.sortedByDescending { it.playlist.id }
                            PlaylistSortBy.Custom -> list.sortedBy { it.playlist.position }
                            else -> list.sortedBy { it.songCount }
                        }
                    }
                    .let { list -> if (sortOrder == SortOrder.Descending) list.reversed() else list }
                    .map { preview -> browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                listOf(shuffleItem) + playlists
            }
            else -> emptyList()
        }
    }
}
