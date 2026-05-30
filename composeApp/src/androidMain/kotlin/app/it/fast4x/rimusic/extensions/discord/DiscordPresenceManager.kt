package app.it.fast4x.rimusic.extensions.discord

import android.content.Context
import androidx.media3.common.MediaItem
import app.n_zik.android.core.network.isNetworkAvailable
import app.kreate.android.R
import app.kreate.android.me.knighthat.utils.Toaster
import com.metrolist.music.discordrpc.DiscordRpcConnection
import com.metrolist.music.discordrpc.entities.Timestamps
import com.metrolist.music.discordrpc.ActivityType
import com.metrolist.music.discordrpc.entities.Button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import timber.log.Timber

/**
 * Call this method when the playing state changes.
 * - isPlaying = true : send the "playing" presence and refresh it every 10s
 * - isPlaying = false : launch a timer, then send the "paused" presence (frozen time)
 */

class DiscordPresenceManager(
    private val context: Context,
    private val getToken: () -> String?,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private const val APPLICATION_ID = "1379051016007454760"
    }

    private var rpc: DiscordRpcConnection? = null
    private var lastToken: String? = null
    private var lastMediaItem: MediaItem? = null
    private var lastPosition: Long = 0L
    private var isStopped = false
    private val discordScope = externalScope
    private var refreshJob: Job? = null
    private val client = app.n_zik.android.core.network.NetworkClientFactory.getClientWithTimeout(10L, 10L)

    private fun getSmallImageUrl(): String {
        return "https://raw.githubusercontent.com/NEVARLeVrai/N-Zik/main/assets/discord/fallback_app.png"
    }

    private fun getLargeImageFallback(): String {
        return "https://raw.githubusercontent.com/NEVARLeVrai/N-Zik/main/assets/discord/fallback_album.png"
    }

    /**
     * Validate the token
     */
    internal suspend fun validateToken(token: String): Boolean? = withContext(Dispatchers.IO) {
        if (!context.isNetworkAvailable) return@withContext null
        val request = Request.Builder()
            .url("https://discord.com/api/v9/users/@me")
            .header("Authorization", token)
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { exception ->
            if (exception.message?.contains("429") == true || exception.message?.contains("Too Many Requests") == true) {
                Timber.tag("DiscordPresence").d("Rate limited by Discord API during token validation")
                null // Treat as network error to retry later
            } else {
                Timber.tag("DiscordPresence").e(exception, "Error validating token: ${exception.message}")
                if (exception is java.io.IOException) {
                    null
                } else {
                    false
                }
            }
        }
    }

    fun onPlayingStateChanged(mediaItem: MediaItem?, isPlaying: Boolean, position: Long = 0L, duration: Long = 0L, now: Long = System.currentTimeMillis(), getCurrentPosition: (() -> Long)? = null, isPlayingProvider: (() -> Boolean)? = null) {
        if (isStopped) return
        val token = getToken() ?: return
        if (token.isEmpty()) return

        if (!context.isNetworkAvailable) {
            return
        }

        refreshJob?.cancel()
        refreshJob = null

        if (token != lastToken) {
            rpc?.closeDirect()
            rpc = DiscordRpcConnection(
                token = token,
                os = "Android",
                browser = "Discord Android",
                device = android.os.Build.DEVICE,
                userAgent = com.metrolist.music.discordrpc.SuperProperties.userAgent,
                superPropertiesBase64 = com.metrolist.music.discordrpc.SuperProperties.superPropertiesBase64
            )
            lastToken = token
        }

        lastMediaItem = mediaItem
        lastPosition = position
        if (mediaItem == null) {
            sendPausedPresence(duration, now, position)
            return
        }
        if (isPlaying) {
            sendPlayingPresence(mediaItem, position, duration, now)
            val currentIsPlaying = isPlaying
            val currentPosition = position
            startRefreshJob(
                isPlayingProvider = { currentIsPlaying },
                mediaItem = mediaItem,
                getCurrentPosition = { currentPosition },
                pausedPosition = position,
                duration = duration
            )
        } else {
            sendPausedPresence(duration, now, position)
            val currentIsPlaying = isPlaying
            val currentPosition = position
            startRefreshJob(
                isPlayingProvider = { currentIsPlaying },
                mediaItem = mediaItem,
                getCurrentPosition = { currentPosition },
                pausedPosition = position,
                duration = duration
            )
        }
    }

    /**
     * Send the "Paused" presence with the frozen time.
     */
    private fun sendPausedPresence(duration: Long, now: Long, pausedPosition: Long) {
        if (isStopped) return
        val mediaItem = lastMediaItem ?: return
        val frozenTimestamp = now - pausedPosition
        val title = mediaItem.mediaMetadata.title?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_title)
        val artist = mediaItem.mediaMetadata.artist?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_artist)
        discordScope.launch {
            if (isStopped) return@launch
            sendActivity(
                mediaItem = mediaItem,
                details = "⏸️ Paused: $title",
                state = artist,
                start = frozenTimestamp,
                end = frozenTimestamp,
                status = "online",
                paused = true
            )
        }
    }

    /**
     * Send a custom discord activity
     */
    private suspend fun sendActivity(
        mediaItem: MediaItem,
        details: String,
        state: String,
        start: Long,
        end: Long,
        status: String,
        paused: Boolean
    ) {
        if (isStopped) return
        val token = getToken() ?: return
        if (token.isEmpty()) return

        when (validateToken(token)) {
            false -> {
                Timber.tag("DiscordPresence").e("Invalid token, stopping presence updates")
                withContext(Dispatchers.Main) {
                    Toaster.e(R.string.discord_token_text_invalid)
                }
                return
            }
            null -> {
                Timber.tag("DiscordPresence").w("Network error while updating presence, skipping.")
                return
            }
            true -> { /* Token is valid, continue */ }
        }

        if (token != lastToken) {
            rpc?.closeDirect()
            rpc = DiscordRpcConnection(
                token = token,
                os = "Android",
                browser = "Discord Android",
                device = android.os.Build.DEVICE,
                userAgent = com.metrolist.music.discordrpc.SuperProperties.userAgent,
                superPropertiesBase64 = com.metrolist.music.discordrpc.SuperProperties.superPropertiesBase64
            )
            lastToken = token
        }
        val largeImageUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: getLargeImageFallback()
        val smallImageUrl = getSmallImageUrl()
        val largeTextValue = if (state.isNotBlank()) "$details - $state" else details
        
        runCatching {
            rpc?.setActivity(
                applicationId = APPLICATION_ID,
                name = "N-Zik",
                details = details,
                state = state,
                type = ActivityType.LISTENING,
                timestamps = Timestamps(
                    start = start,
                    end = end
                ),
                largeImage = largeImageUrl,
                smallImage = smallImageUrl,
                largeText = largeTextValue,
                smallText = "v${getVersionName(context)}",
                buttons = listOf(
                    Button(label = "Get N-Zik", url = "https://github.com/NEVARLeVrai/N-Zik/"),
                    Button(label = "Listen to YTMusic", url = "https://music.youtube.com/watch?v=${mediaItem.mediaId}")
                ),
                status = status,
                since = System.currentTimeMillis()
            )
        }.onFailure {
            Timber.tag("DiscordPresence").w("Error setting Discord activity: ${it.message}")
        }
    }

    /**
     * Close the discord presence (STOP)
     */
    fun onStop() {
        isStopped = true
        refreshJob?.cancel()
        rpc?.closeDirect()
        discordScope.cancel()
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
     * Send a custom discord activity
     */
    private fun sendPlayingPresence(mediaItem: MediaItem, position: Long, duration: Long, now: Long) {
        val start = now - position
        val end = start + duration
        val title = mediaItem.mediaMetadata.title?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_title)
        val artist = mediaItem.mediaMetadata.artist?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_artist)
        discordScope.launch {
            sendActivity(
                mediaItem = mediaItem,
                details = title,
                state = artist,
                start = start,
                end = end,
                status = "online",
                paused = false
            )
        }
    }

    /**
     * Start the refresh job
     */
    private fun startRefreshJob(
        isPlayingProvider: () -> Boolean,
        mediaItem: MediaItem,
        getCurrentPosition: () -> Long,
        pausedPosition: Long,
        duration: Long
    ) {
        refreshJob = discordScope.launch {
            while (isActive && !isStopped) {
                delay(15_000L)
                if (!context.isNetworkAvailable) {
                    continue
                }
                val isPlaying = isPlayingProvider()
                if (isPlaying) {
                    val pos = getCurrentPosition()
                    sendPlayingPresence(mediaItem, pos, duration, System.currentTimeMillis())
                } else {
                    sendPausedPresence(duration, System.currentTimeMillis(), pausedPosition)
                }
            }
        }
    }
}
