package app.n_zik.android.playback.services.automotive.browse.handlers

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.utils.Preference
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.preferences
import app.n_zik.android.R
import app.n_zik.android.core.database.Database
import app.n_zik.android.download.utils.MyDownloadHelper
import app.n_zik.android.playback.services.automotive.models.SessionMediaItemMapper
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants
import app.n_zik.android.playback.services.PlayerServiceModern
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.browsableMediaItem
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.drawableUri
import kotlinx.coroutines.flow.first

class ArtistsBrowseHandler : BrowseHandler {
    override fun handles(parentId: String): Boolean = parentId == PlayerServiceModern.ARTIST ||
            parentId.startsWith("ARTISTS_") || parentId.startsWith("ID_ARTISTS_")

    override suspend fun getChildren(
        parentId: String,
        context: Context,
        database: Database,
        downloadHelper: MyDownloadHelper,
        binder: PlayerServiceModern.Binder?
    ): List<MediaItem> {
        return when (parentId) {
            PlayerServiceModern.ARTIST -> {
                val libraryCount = database.artistTable.allInLibrary().first().size
                val favoritesCount = database.artistTable.allFollowing().first().size
                listOf(
                    browsableMediaItem(AutoSessionConstants.ID_ARTISTS_FAVORITES, context.getString(R.string.favorites), favoritesCount.toString(), drawableUri(context, R.drawable.heart), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                    browsableMediaItem(AutoSessionConstants.ID_ARTISTS_LIBRARY, context.getString(R.string.library), libraryCount.toString(), drawableUri(context, R.drawable.artist), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                )
            }
            AutoSessionConstants.ID_ARTISTS_LIBRARY -> {
                val shuffleItem = AutoSessionConstants.shuffleItem(context, AutoSessionConstants.ID_ARTISTS_LIBRARY_SHUFFLE)
                val sortBy = context.preferences.getEnum(Preference.HOME_ARTISTS_LIBRARY_SORT_BY.key, app.it.fast4x.rimusic.enums.ArtistSortBy.Name)
                val sortOrder = context.preferences.getEnum(Preference.HOME_ARTISTS_LIBRARY_SORT_ORDER.key, SortOrder.Ascending)
                val artists = database.artistTable.sortInLibrary(sortBy, sortOrder).first().map { artist -> SessionMediaItemMapper.mapArtistToMediaItem(PlayerServiceModern.ARTIST, artist.id, artist.name ?: "", artist.thumbnailUrl) }
                listOf(shuffleItem) + artists
            }
            AutoSessionConstants.ID_ARTISTS_FAVORITES -> {
                val shuffleItem = AutoSessionConstants.shuffleItem(context, AutoSessionConstants.ID_ARTISTS_FAVORITES_SHUFFLE)
                val sortBy = context.preferences.getEnum(Preference.HOME_ARTISTS_FAVORITES_SORT_BY.key, app.it.fast4x.rimusic.enums.ArtistSortBy.Name)
                val sortOrder = context.preferences.getEnum(Preference.HOME_ARTISTS_FAVORITES_SORT_ORDER.key, SortOrder.Ascending)
                val artists = database.artistTable.sortFollowing(sortBy, sortOrder).first().map { artist -> SessionMediaItemMapper.mapArtistToMediaItem(PlayerServiceModern.ARTIST, artist.id, artist.name ?: "", artist.thumbnailUrl) }
                listOf(shuffleItem) + artists
            }
            else -> emptyList()
        }
    }
}
