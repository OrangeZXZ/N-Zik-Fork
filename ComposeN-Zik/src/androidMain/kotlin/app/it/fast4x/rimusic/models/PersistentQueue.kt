package app.it.fast4x.rimusic.models

import android.content.ContentUris
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import app.n_zik.android.playback.services.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.hasExplicitPrefix
import java.io.Serializable
import app.it.fast4x.rimusic.MODIFIED_PREFIX
import app.it.fast4x.rimusic.cleanPrefix

data class PersistentQueue(
    val title: String?,
    val songMediaItems: List<PersistentSong>,
    val mediaItemIndex: Int,
    val position: Long,
) : Serializable

data class PersistentSong(
    val id: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String?,
    val thumbnailUrl: String?,
    val likedAt: Long? = null,
    val totalPlayTimeMs: Long = 0
) : Serializable

val PersistentSong.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(
                    if (title.hasExplicitPrefix()) {
                        "\uD83C\uDD74 " + cleanPrefix(title)
                    } else {
                        cleanPrefix(title)
                    }
                )
                .setArtist(artistsText?.let { cleanPrefix(it) })
                .setArtworkUri(thumbnailUrl?.let { cleanPrefix(it) }?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "isCoverModified" to (thumbnailUrl?.startsWith(MODIFIED_PREFIX, true) == true),
                        "isTitleModified" to (title.startsWith(MODIFIED_PREFIX, true)),
                        "isArtistModified" to (artistsText?.startsWith(MODIFIED_PREFIX, true) == true)
                    )
                )
                .build()
        )
        .setMediaId(id)
        .setUri(
            if (id.startsWith(LOCAL_KEY_PREFIX)) ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id.substringAfter(LOCAL_KEY_PREFIX).toLong()
            ) else id.toUri()
        )
        .setCustomCacheKey(id)
        .build()


