package it.fast4x.rimusic.extensions.discord

import android.content.Context
import androidx.media3.common.MediaItem
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Timestamps
import kotlinx.coroutines.*
import okhttp3.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.SupervisorJob

class DiscordPresenceManager(
    private val context: Context,
    private val getToken: () -> String?,
) {
    private var rpc: KizzyRPC? = null
    private var lastToken: String? = null
    private var lastMediaItem: MediaItem? = null
    private var lastPosition: Long = 0L
    private var isStopped = false
    private val discordScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var refreshJob: Job? = null

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val uploadApi = "https://tmpfiles.org/api/v1/upload"

    /**
     * THIS IS STILL IN BETA AND MAY NOT WORK AS EXPECTED AND CAUSE CRASH
     * Call this method when the playing state changes.
     * - isPlaying = true : send the "playing" presence and refresh it every 10s
     * - isPlaying = false : launch a timer, then send the "paused" presence (frozen time)
     */
    fun onPlayingStateChanged(mediaItem: MediaItem?, isPlaying: Boolean, position: Long = 0L, duration: Long = 0L, now: Long = System.currentTimeMillis(), getCurrentPosition: (() -> Long)? = null, isPlayingProvider: (() -> Boolean)? = null) {
        if (isStopped) return
        val token = getToken() ?: return

        rpc = KizzyRPC(token)
        lastToken = token
        
        lastMediaItem = mediaItem
        lastPosition = position
        refreshJob?.cancel()
        refreshJob = null
        if (mediaItem == null) {
            sendPausedPresence(duration, now, position)
            return
        }
        if (isPlaying) {
            sendPlayingPresence(mediaItem, position, duration, now)
            startRefreshJob(
                isPlayingProvider = isPlayingProvider ?: { true },
                mediaItem = mediaItem,
                getCurrentPosition = getCurrentPosition ?: { position },
                pausedPosition = position,
                duration = duration
            )
        } else {
            sendPausedPresence(duration, now, position)
            startRefreshJob(
                isPlayingProvider = isPlayingProvider ?: { false },
                mediaItem = mediaItem,
                getCurrentPosition = getCurrentPosition ?: { position },
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
        discordScope.launch {
            if (isStopped) return@launch
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
        val largeImageUrl = getLargeImageUrl(mediaItem)
            ?: "mp:attachments/1231921505923760150/1379170235298615377/album.png?ex=683f43df&is=683df25f&hm=cd08ce130264f2da98b8d2b0065ba58e39a0b0c125556fd7862f5155f110ce4b&="
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
                    largeImage = largeImageUrl,
                    smallImage = "mp:attachments/1231921505923760150/1379166057809575997/N-Zik_Discord.png?ex=683fe8bb&is=683e973b&hm=20d40b8f9fc926cbc94de732dd06128abcc6126d7b58285650ded02bd7fd07a6&",
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
        refreshJob?.cancel()
        rpc?.closeRPC()
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
     * Get the type of the discord activity
     */
    enum class TypeDiscordActivity (val value: Int) {
        PLAYING(0),
        STREAMING(1),
        LISTENING(2),
        WATCHING(3),
        COMPETING(5)
    }

    /**
     * Upload an image to the tmpfiles.org
     */
    private suspend fun uploadImage(file: File): String? = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.readBytes().toRequestBody("image/*".toMediaType()))
            .build()
        val request = Request.Builder().url(uploadApi).post(requestBody).build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@runCatching null
                json.decodeFromString<ApiResponse>(body).data.url
            }
        }.getOrNull()?.replace("https://tmpfiles.org/", "https://tmpfiles.org/dl/")
    }

    /**
     * Get the artwork file
     */
    private fun getArtworkFile(mediaItem: MediaItem): File? {
        val uri = mediaItem.mediaMetadata.artworkUri ?: return null
        return if (uri.scheme == "file" || uri.scheme == null) File(uri.path!!) else null
    }

    /**
     * Get the discord asset uri
     */
    private suspend fun getDiscordAssetUri(imageUrl: String, token: String): String? = withContext(Dispatchers.IO) {
        if (imageUrl.startsWith("mp:")) return@withContext imageUrl
        val api = "https://discord.com/api/v9/applications/1379051016007454760/external-assets"
        val request = Request.Builder().url(api)
            .header("Authorization", token)
            .post("{\"urls\":[\"$imageUrl\"]}".toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@runCatching null
                val jsonArr = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonArray
                val externalAssetPath = jsonArr.firstOrNull()?.jsonObject?.get("external_asset_path")?.toString()?.replace("\"", "")
                externalAssetPath?.let { "mp:$it" }
            }
        }.getOrNull()
    }

    /**
     * Get the large image url
     */
    private suspend fun getLargeImageUrl(mediaItem: MediaItem): String? {
        val token = getToken() ?: return null
        val artworkFile = getArtworkFile(mediaItem)
        val url = if (artworkFile != null && artworkFile.exists()) {
            uploadImage(artworkFile)
        } else {
            mediaItem.mediaMetadata.artworkUri?.toString()?.takeIf { it.startsWith("http") }
        }
        return if (url != null) getDiscordAssetUri(url, token) else null
    }

    /**
     * Send a custom discord activity
     */
    private fun sendPlayingPresence(mediaItem: MediaItem, position: Long, duration: Long, now: Long) {
        val start = now - position
        val end = start + duration
        discordScope.launch {
            sendActivity(
                mediaItem = mediaItem,
                details = mediaItem.mediaMetadata.title?.toString() ?: "N-Zik",
                state = mediaItem.mediaMetadata.artist?.toString() ?: "N-Zik",
                start = start,
                end = end,
                status = "online",
                paused = false
            )
        }
    }

    private fun startRefreshJob(
        isPlayingProvider: () -> Boolean,
        mediaItem: MediaItem,
        getCurrentPosition: () -> Long,
        pausedPosition: Long,
        duration: Long
    ) {
        refreshJob = discordScope.launch {
            while (isActive && !isStopped) {
                delay(40_000L)
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

/**
 * Api response
 */
@Serializable
data class ApiResponse(val status: String, val data: Data) {
    @Serializable
    data class Data(val url: String)
}