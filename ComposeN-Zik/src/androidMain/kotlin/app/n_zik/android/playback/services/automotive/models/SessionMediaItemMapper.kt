package app.n_zik.android.playback.services.automotive.models

import app.n_zik.android.playback.services.automotive.session.AutoSessionConstants
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.drawableUri
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.browsableMediaItem
import app.n_zik.android.playback.services.*
import app.n_zik.android.playback.exceptions.*
import app.n_zik.android.playback.utils.*

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.core.net.toUri
import app.it.fast4x.rimusic.EXPLICIT_PREFIX
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.utils.EXPLICIT_BUNDLE_TAG
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.persistentQueueKey
import androidx.media3.session.MediaConstants.EXTRAS_KEY_IS_EXPLICIT
import app.n_zik.android.core.coil.thumbnail
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.browsableMediaItem
import app.n_zik.android.playback.services.automotive.models.AutoMediaItemMapper.drawableUri

@UnstableApi
object SessionMediaItemMapper {

    fun mapArtistToMediaItem(
        parentId: String,
        id: String,
        name: String,
        thumbnailUrl: String?,
        subtext: String? = null,
        searchPath: String = ""
    ): MediaItem = browsableMediaItem(
        id = "$parentId/$id",
        title = name,
        subtitle = subtext,
        iconUri = thumbnailUrl?.thumbnail(250)?.toUri(), // ENHANCED QUALITY
        mediaType = MediaMetadata.MEDIA_TYPE_ARTIST,
        path = searchPath.ifEmpty { parentId }
    )

    fun mapAlbumToMediaItem(
        parentId: String,
        id: String,
        title: String,
        authorsText: String?,
        thumbnailUrl: String?,
        searchPath: String = ""
    ): MediaItem = browsableMediaItem(
        id = "$parentId/$id",
        title = title,
        subtitle = authorsText,
        iconUri = thumbnailUrl?.thumbnail(250)?.toUri(), // ENHANCED QUALITY
        mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
        path = searchPath.ifEmpty { parentId }
    )


    fun mapSongToMediaItem(song: Song, path: String): MediaItem {
        val baseItem = song.asMediaItem
        var metadataBuilder = baseItem.mediaMetadata.buildUpon()

        if (song.isLocal) {
            metadataBuilder.setArtworkUri(drawableUri(app.n_zik.android.appContext(), app.n_zik.android.R.drawable.ic_launcher_box))
        }



        val extras = android.os.Bundle(metadataBuilder.build().extras ?: android.os.Bundle()).apply {
            if (!song.isLocal) {
                putLong("android.media.metadata.DURATION", app.it.fast4x.rimusic.utils.durationTextToMillis(song.durationText.orEmpty()))
            }
        }
        metadataBuilder.setExtras(extras)

        return if (path.contains(AutoSessionConstants.ID_SEARCH_VIDEOS)) {
            baseItem.buildUpon()
                .setMediaId("$path/${song.id}")
                .setMediaMetadata(metadataBuilder.setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO).build())
                .build()
        } else {
            baseItem.buildUpon()
                .setMediaId("$path/${song.id}")
                .setMediaMetadata(metadataBuilder.build())
                .build()
        }
    }

    fun mapSongToMediaItem(song: Song, isFromPersistentQueue: Boolean = false): MediaItem {
        val mediaItem = song.asMediaItem
        val existingExtras = mediaItem.mediaMetadata.extras ?: android.os.Bundle()
        val bundle = android.os.Bundle(existingExtras).apply {
            putBoolean(persistentQueueKey, isFromPersistentQueue)
        }

        var metadataBuilder = mediaItem.mediaMetadata
            .buildUpon()
            .setExtras(bundle)

        return mediaItem.buildUpon().setMediaMetadata(metadataBuilder.build()).build()
    }


}
