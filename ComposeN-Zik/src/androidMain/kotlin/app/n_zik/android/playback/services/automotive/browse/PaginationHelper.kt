package app.n_zik.android.playback.services.automotive.browse

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.n_zik.android.R
import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants
import app.n_zik.android.playback.services.PlayerServiceModern
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.drawableUri

object PaginationHelper {
    fun paginate(
        context: Context,
        filteredList: List<MediaItem>,
        actualParentId: String,
        pageIndex: Int
    ): List<MediaItem> {
        return if (pageIndex >= 0) {
            val itemsPerPage = 200
            val shuffleItem = filteredList.firstOrNull { it.mediaId.endsWith("_SHUFFLE") }
            val realItems = filteredList.filter { !it.mediaId.endsWith("_SHUFFLE") && !it.mediaId.endsWith("_PLAY_ALL") }
            val pageItems = realItems.chunked(itemsPerPage).getOrNull(pageIndex) ?: emptyList()
            if (shuffleItem != null) listOf(shuffleItem) + pageItems else pageItems
        } else if (filteredList.size > 200 && actualParentId != PlayerServiceModern.ROOT && actualParentId != PlayerServiceModern.SONG && actualParentId != PlayerServiceModern.ARTIST && actualParentId != PlayerServiceModern.ALBUM && actualParentId != PlayerServiceModern.PLAYLIST) {
            val itemsPerPage = 200
            val shuffleItem = filteredList.firstOrNull { it.mediaId.endsWith("_SHUFFLE") }
            val realItems = filteredList.filter { !it.mediaId.endsWith("_SHUFFLE") && !it.mediaId.endsWith("_PLAY_ALL") }
            val playAllItem = if (shuffleItem != null && shuffleItem.mediaId != AutoSessionConstants.ID_LUCKY_SHUFFLE) AutoSessionConstants.playAllItem(context, shuffleItem.mediaId.replace("_SHUFFLE", "_PLAY_ALL")) else null
            val folders = realItems.chunked(itemsPerPage).mapIndexed { index, chunk ->
                val start = index * itemsPerPage + 1
                val end = start + chunk.size - 1
                MediaItem.Builder()
                    .setMediaId("${actualParentId}_PAGE_$index")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Page ${index + 1} ($start - $end)")
                            .setArtworkUri(drawableUri(context, R.drawable.library))
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                            .build()
                    )
                    .build()
            }
            listOfNotNull(playAllItem, shuffleItem) + folders
        } else {
            val shuffleItem = filteredList.firstOrNull { it.mediaId.endsWith("_SHUFFLE") }
            if (shuffleItem != null) {
                val playAllItem = if (shuffleItem.mediaId != AutoSessionConstants.ID_LUCKY_SHUFFLE) AutoSessionConstants.playAllItem(context, shuffleItem.mediaId.replace("_SHUFFLE", "_PLAY_ALL")) else null
                val realItems = filteredList.filter { !it.mediaId.endsWith("_SHUFFLE") && !it.mediaId.endsWith("_PLAY_ALL") }
                listOfNotNull(playAllItem, shuffleItem) + realItems
            } else {
                filteredList
            }
        }
    }
}
