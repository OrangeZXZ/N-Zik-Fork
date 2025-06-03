package it.fast4x.rimusic.extensions.discord

import android.content.Context
import androidx.media3.common.MediaItem
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Timestamps
import kotlinx.coroutines.*
import java.util.*

class DiscordPresenceManager(
    private val context: Context,
    private val getToken: () -> String?,
    private val pauseWaitTime: Long = 15_000L
) {
    private var pauseTimer: Timer? = null
    private var playingJob: Job? = null
    private var rpc: KizzyRPC? = null
    private var lastToken: String? = null
    private var lastMediaItem: MediaItem? = null
    private var lastPosition: Long = 0L
    private var isStopped = false

    /**
     * Call this method when the playing state changes.
     * - isPlaying = true : send the "playing" presence and refresh it every 10s
     * - isPlaying = false : launch a timer, then send the "paused" presence (frozen time)
     */
    fun onPlayingStateChanged(mediaItem: MediaItem?, isPlaying: Boolean, position: Long = 0L, duration: Long = 0L, now: Long = System.currentTimeMillis()) {
        val token = getToken() ?: return
        if (token != lastToken || rpc == null) {
            rpc = KizzyRPC(token)
            lastToken = token
        }
        lastMediaItem = mediaItem
        lastPosition = position
        if (mediaItem == null) {
            stopPlayingJob()
            sendPausedPresence(duration, now)
            return
        }
        if (isPlaying) {
            pauseTimer?.cancel()
            pauseTimer = null
            startPlayingJob(mediaItem, token, position, duration, now)
        } else {
            stopPlayingJob()
            pauseTimer?.cancel()
            pauseTimer = Timer()
            pauseTimer?.schedule(object : TimerTask() {
                override fun run() {
                    sendPausedPresence(duration, now)
                }
            }, pauseWaitTime)
        }
    }

    /**
     * Refresh the "playing" presence every 10s.
     */
    private fun startPlayingJob(mediaItem: MediaItem, token: String, position: Long, duration: Long, now: Long) {
        stopPlayingJob()
        playingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val start = now - position
                val end = start + duration
                sendActivity(
                    mediaItem = mediaItem,
                    details = mediaItem.mediaMetadata.title?.toString() ?: "N-Zik",
                    state = mediaItem.mediaMetadata.artist?.toString() ?: "N-Zik",
                    start = start,
                    end = end,
                    status = "online",
                    paused = false
                )
                delay(10_000L)
            }
        }
    }

    private fun stopPlayingJob() {
        playingJob?.cancel()
        playingJob = null
    }

    /**
     * Send the "Paused" presence with the frozen time.
     */
    private fun sendPausedPresence(duration: Long, now: Long) {
        val mediaItem = lastMediaItem ?: return
        val frozenTimestamp = now - lastPosition
        sendActivity(
            mediaItem = mediaItem,
            details = "⏸️ Paused: ${mediaItem.mediaMetadata.title}",
            state = mediaItem.mediaMetadata.artist?.toString() ?: "N-Zik",
            start = frozenTimestamp,
            end = frozenTimestamp,
            status = "idle",
            paused = true
        )
    }

    /**
     * Send a custom discord activity (playing, paused, etc.)
     */
    private fun sendActivity(
        mediaItem: MediaItem,
        details: String,
        state: String,
        start: Long,
        end: Long,
        status: String,
        paused: Boolean
    ) {
        if (isStopped) return
        rpc?.setActivity(
            activity = Activity(
                applicationId = "1379051016007454760",
                name = "N-Zik",
                details = details,
                state = state,
                type = TypeDiscordActivity.LISTENING.value,
                timestamps = Timestamps(
                    start = start,
                    end = end
                ),
                assets = Assets(
                    largeImage = "mp:attachments/1231921505923760150/1379170235298615377/album.png?ex=683f43df&is=683df25f&hm=cd08ce130264f2da98b8d2b0065ba58e39a0b0c125556fd7862f5155f110ce4b&",
                    smallImage = "mp:attachments/1231921505923760150/1379166057809575997/N-Zik_Discord.png?ex=683f3ffb&is=683dee7b&hm=73a1edc08f7f657ef36c4f49ff8a6a22fbf3d0121eaf08d4fe3d28032edaea79&",
                    largeText = mediaItem.mediaMetadata.title?.toString() + " - " + mediaItem.mediaMetadata.artist?.toString(),
                    smallText = "v${getVersionName(context)}",
                ),
                buttons = listOf("Get N-Zik", "Listen to YTMusic"),
                metadata = com.my.kizzyrpc.model.Metadata(
                    listOf(
                        "https://github.com/NEVARLeVrai/N-Zik/",
                        "https://music.youtube.com/watch?v=${mediaItem.mediaId}",
                    )
                )
            ),
            status = status,
            since = System.currentTimeMillis()
        )
    }

    /**
     * Close the discord presence (STOP)
     */
    fun onStop() {
        isStopped = true
        rpc?.closeRPC()
        stopPlayingJob()
        pauseTimer?.cancel()
        pauseTimer = null
    }


    /**
     * Temp the discord presence
     */
    fun cancel() {
        stopPlayingJob()
        pauseTimer?.cancel()
        pauseTimer = null
    }


    /**
     * Get the version name of the app
     */
    fun getVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
     /**
     * Get the type of the discord activity
     */
    enum class TypeDiscordActivity (val value: Int) {
        PLAYING(0),
        STREAMING(1),
        LISTENING(2),
        WATCHING(3),
        COMPETING(5)
    }
}