package app.n_zik.android.playback.utils

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.n_zik.android.R
import app.n_zik.android.appContext
import app.n_zik.android.core.database.Database
import app.n_zik.android.playback.services.PlayerServiceModern
import app.kreate.android.me.knighthat.utils.Toaster
import app.it.fast4x.rimusic.enums.MaxSongs
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.forcePlayFromBeginning
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.maxSongsInQueueKey
import app.it.fast4x.rimusic.utils.mediaItems
import app.it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
object Shuffler {

    fun play(binder: PlayerServiceModern.Binder, mediaItems: List<MediaItem>) {
        if (mediaItems.isEmpty()) {
            Toaster.i(R.string.no_song_to_shuffle)
            return
        }
        val max = appContext().preferences
            .getEnum(maxSongsInQueueKey, MaxSongs.`500`)
            .toInt()
        val toPlay = mediaItems.shuffled().take(max)
        CoroutineScope(Dispatchers.Main).launch {
            binder.stopRadio()
            binder.player.forcePlayFromBeginning(toPlay)
            Toaster.s(R.string.songs_shuffled, formatArgs = *arrayOf(toPlay.size))
        }
    }

    @JvmName("playSongs")
    fun play(binder: PlayerServiceModern.Binder, songs: List<Song>) {
        play(binder, songs.map(Song::asMediaItem))
    }

    fun queue(player: Player) {
        val current = player.currentMediaItemIndex
        val total = player.mediaItemCount
        if (total <= 1) return

        val items = player.mediaItems.toMutableList().apply {
            removeAt(current)
        }
        val count = items.size
        if (count > 0) {
            if (current > 0) player.removeMediaItems(0, current)
            if (current < player.mediaItemCount - 1) player.removeMediaItems(1, player.mediaItemCount)
            player.addMediaItems(items.shuffled())
            Toaster.s(R.string.queue_shuffled, formatArgs = *arrayOf(count))
        }
    }

    fun <T> shuffle(list: List<T>): List<T> = list.shuffled()

    fun positions(playlistId: Long) {
        CoroutineScope(Dispatchers.Default).launch {
            val items = Database.songPlaylistMapTable.allSongsOf(playlistId).first()
            val count = items.size
            if (count == 0) return@launch
            val shuffled = items.shuffled()
            Database.asyncTransaction {
                shuffled.forEachIndexed { i, song ->
                    Database.songPlaylistMapTable.updatePosition(playlistId, song.id, i)
                }
            }
            withContext(Dispatchers.Main) {
                Toaster.s(R.string.playlist_positions_shuffled, formatArgs = *arrayOf(count))
            }
        }
    }
}
