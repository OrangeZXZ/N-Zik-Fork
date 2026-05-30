package app.n_zik.android.core.service

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import app.kreate.android.R
import app.kreate.android.Threads
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.core.network.NetworkClientFactory
import app.n_zik.android.core.network.Store
import app.n_zik.android.core.utils.cipher.CipherDeobfuscator
import app.n_zik.android.core.utils.cipher.PlayerJsFetcher
import app.n_zik.android.core.utils.cipher.FunctionNameExtractor
import app.n_zik.android.core.utils.potoken.PoTokenGenerator
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.clients.YouTubeClient
import it.fast4x.innertube.clients.YouTubeLocale
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.enums.AudioQualityFormat
import app.it.fast4x.rimusic.isConnectionMeteredEnabled
import app.it.fast4x.rimusic.models.Format
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.LoginRequiredException
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.UnknownException
import app.it.fast4x.rimusic.service.UnplayableException
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.utils.isConnectionMetered
import app.it.fast4x.rimusic.utils.okHttpDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.net.UnknownHostException
import io.ktor.client.call.body
import timber.log.Timber

private const val TAG = "DataspecServices"
private const val CHUNK_LENGTH = 512 * 1024L

// Clients to try in order - mirrors Metrolist's YTPlayerUtils fallback chain
private val FALLBACK_CLIENTS = listOf(
    YouTubeClient.WEB_REMIX,
    YouTubeClient.TVHTML5,          // TVHTML5_SIMPLY_EMBEDDED_PLAYER
    YouTubeClient.ANDROID_VR_1_43_32,
    YouTubeClient.ANDROID_VR_1_61_48,
    YouTubeClient.ANDROID_VR_NO_AUTH,
    YouTubeClient.ANDROID,
    YouTubeClient.IOS,
    YouTubeClient.IPADOS,
)

// Clients that return direct URL (no signatureCipher, no n-transform needed)
private val DIRECT_URL_CLIENTS = setOf("ANDROID_VR", "ANDROID", "IOS", "ANDROID_MUSIC", "ANDROID_CREATOR")

/**
 * Store id of song just added to the database to reduce load to Room.
 */
@set:Synchronized
private var justInserted: String = ""

private val jsonParser = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    useArrayPolymorphism = true
    explicitNulls = false
}

@Blocking
private fun upsertSongInfo(videoId: String) = runBlocking {
    if (videoId == justInserted) return@runBlocking
    Innertube.nextPage(NextBody(videoId = videoId))?.fold(
        onSuccess = { nextPage ->
            val songItem = nextPage.itemsPage?.items?.firstOrNull() ?: return@fold
            Database.upsert(songItem)
        },
        onFailure = {
            when (it) {
                is UnknownHostException -> justInserted = videoId
                else -> Toaster.e(R.string.failed_to_fetch_original_property)
            }
        }
    )
}

@NonBlocking
private fun upsertSongFormat(videoId: String, format: PlayerResponse.StreamingData.Format) {
    if (videoId == justInserted) return
    runCatching {
        Database.asyncTransaction {
            songTable.insertIgnore(Song.makePlaceholder(videoId))
            formatTable.insertIgnore(Format(
                videoId,
                format.itag,
                format.mimeType,
                format.bitrate.toLong(),
                format.contentLength,
                format.lastModified,
                format.loudnessDb?.toFloat()
            ))
        }
        justInserted = videoId
    }
}

/**
 * Checks playability status and throws appropriate exception.
 */
@UnstableApi
private fun checkPlayability(playabilityStatus: PlayerResponse.PlayabilityStatus?) {
    if (playabilityStatus?.status != "OK")
        when (playabilityStatus?.status) {
            "LOGIN_REQUIRED" -> throw LoginRequiredException()
            "UNPLAYABLE"     -> throw UnplayableException()
            else             -> throw UnknownException()
        }
}

/**
 * Picks the best audio format from a player response based on user quality preference.
 */
private fun pickFormat(
    streamingData: PlayerResponse.StreamingData?,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): PlayerResponse.StreamingData.Format? =
    when (audioQualityFormat) {
        AudioQualityFormat.High   -> streamingData?.highestQualityFormat
        AudioQualityFormat.Medium -> streamingData?.mediumQualityFormat
        AudioQualityFormat.Low    -> streamingData?.lowestQualityFormat
        AudioQualityFormat.Auto   ->
            if (connectionMetered && isConnectionMeteredEnabled())
                streamingData?.mediumQualityFormat
            else
                streamingData?.autoMaxQualityFormat
    }

/**
 * Resolves the stream URL for a given format.
 *
 * - For ANDROID_VR / ANDROID / IOS: uses the direct URL from the format.
 * - For WEB_REMIX / web clients: deobfuscates via CipherDeobfuscator.
 */
private suspend fun resolveFormatUrl(
    videoId: String,
    format: PlayerResponse.StreamingData.Format,
    clientName: String
): Uri? {
    // Direct URL clients (no signature cipher, no n-transform)
    if (clientName in DIRECT_URL_CLIENTS) {
        val directUrl = format.url ?: return null
        Timber.tag(TAG).d("Direct URL for $clientName: ${directUrl.take(80)}...")
        return directUrl.toUri()
    }

    // signatureCipher needs deobfuscation (web clients)
    val sigCipher = format.signatureCipher
    val formatUrl = format.url
    return if (sigCipher != null) {
        Timber.tag(TAG).d("signatureCipher detected, deobfuscating for $videoId")
        try {
            val deobfuscated = CipherDeobfuscator.deobfuscateStreamUrl(sigCipher, videoId)
            if (deobfuscated != null) {
                Timber.tag(TAG).d("CipherDeobfuscator success for $videoId")
                android.net.Uri.parse(deobfuscated)
            } else {
                Timber.tag(TAG).w("CipherDeobfuscator returned null, trying NewPipe fallback")
                val newPipeUrl = runCatching {
                    org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
                        .getUrlWithThrottlingParameterDeobfuscated(videoId, sigCipher)
                }.getOrNull()
                newPipeUrl?.let { android.net.Uri.parse(it) }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cipher deobfuscation failed for $videoId")
            null
        }
    } else if (formatUrl != null) {
        android.net.Uri.parse(formatUrl)
    } else {
        null
    }
}

/**
 * Core stream resolution logic.
 *
 * Mirrors Metrolist's YTPlayerUtils.playerResponseForPlayback() flow:
 * 1. Fetch signatureTimestamp from player.js
 * 2. Generate PoToken (WebView)
 * 3. Try each client in order → get player response → resolve URL → validate
 * 4. Return the first working stream URL
 */
@UnstableApi
private suspend fun resolveStreamUri(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val locale = YouTubeLocale(
        gl = java.util.Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "US",
        hl = java.util.Locale.getDefault().language.takeIf { it.isNotEmpty() } ?: "en"
    )
    val visitorData = Store.getIosVisitorData() ?: Innertube.DEFAULT_VISITOR_DATA

    // 1. Get signatureTimestamp from player.js (needed for WEB_REMIX)
    val signatureTimestamp: Int? = runCatching {
        val playerJsResult = PlayerJsFetcher.getPlayerJs(forceRefresh = false)
        if (playerJsResult != null) {
            FunctionNameExtractor.extractSignatureTimestamp(playerJsResult.second)
        } else null
    }.getOrNull()
    Timber.tag(TAG).d("signatureTimestamp: $signatureTimestamp")

    // 2. Generate PoToken for WEB_REMIX (web clients only)
    val poToken: String? = runCatching<String?> {
        val sessionId = Store.getIosVisitorData() ?: ""
        app.n_zik.android.core.utils.potoken.PoTokenGenerator().getWebClientPoToken(videoId, sessionId)?.playerRequestPoToken
    }.getOrNull()
    Timber.tag(TAG).d("poToken generated: ${poToken != null}")

    // 3. Try each client in order
    for (ytClient in FALLBACK_CLIENTS) {
        try {
            Timber.tag(TAG).d("Trying client: ${ytClient.clientName} for $videoId")
            val context = ytClient.toContext(locale, visitorData)

            val sigTs = if (ytClient.useSignatureTimestamp) signatureTimestamp else null
            val pot   = if (ytClient.clientName == "WEB_REMIX") poToken else null

            val httpResponse = Innertube.playerRequest(
                videoId = videoId,
                playlistId = null,
                signatureTimestamp = sigTs,
                poToken = pot,
                context = context
            )

            val playerResponse = runCatching {
                httpResponse.body<PlayerResponse>()
            }.getOrNull() ?: run {
                Timber.tag(TAG).w("${ytClient.clientName}: failed to parse PlayerResponse")
                continue
            }

            // Check playability
            if (playerResponse.playabilityStatus?.status != "OK") {
                Timber.tag(TAG).w("${ytClient.clientName}: status=${playerResponse.playabilityStatus?.status} reason=${playerResponse.playabilityStatus?.reason}")
                continue
            }

            // Pick best audio format
            val format = pickFormat(playerResponse.streamingData, audioQualityFormat, connectionMetered)
            if (format == null) {
                Timber.tag(TAG).w("${ytClient.clientName}: no suitable format found")
                continue
            }

            // Resolve URL
            val uri = resolveFormatUrl(videoId, format, ytClient.clientName) ?: run {
                Timber.tag(TAG).w("${ytClient.clientName}: could not resolve URL for format ${format.itag}")
                continue
            }

            // Validate (HEAD request)
            val streamUrl = uri.toString()
            val isValid = NetworkClientFactory.validateStreamUrl(streamUrl)
            if (!isValid) {
                Timber.tag(TAG).w("${ytClient.clientName}: stream URL validation failed (403?) for $videoId")
                continue
            }

            // Success!
            Timber.tag(TAG).d("${ytClient.clientName}: stream resolved successfully for $videoId")
            CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongFormat(videoId, format) }

            return uri
                .buildUpon()
                .appendQueryParameter("range", "0-${format.contentLength ?: 1_000_000}")
                .build()

        } catch (e: LoginRequiredException) {
            Timber.tag(TAG).w("${ytClient.clientName}: LoginRequired, skipping")
            continue
        } catch (e: Exception) {
            Timber.tag(TAG).w("${ytClient.clientName}: exception: ${e.message}")
            continue
        }
    }

    throw UnplayableException()
}

// ──────────────────────────────────────────────────────────────────────────────
// Cache + DataSpec integration
// ──────────────────────────────────────────────────────────────────────────────

private val formatCache = mutableMapOf<String, Uri>()

@UnstableApi
fun DataSpec.process(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): DataSpec = runBlocking(Dispatchers.IO) {
    Timber.tag(TAG).d("Resolving stream for videoId=$videoId, isLoggedIn=false")

    var formatUri = formatCache[videoId]

    if (formatUri != null) {
        val expireTime = formatUri.getQueryParameter("expire")?.toLongOrNull()?.times(1000)
        if (expireTime != null && System.currentTimeMillis() >= expireTime - 30_000) {
            formatCache.remove(videoId)
            formatUri = null
        }
    }

    if (formatUri == null) {
        formatUri = resolveStreamUri(videoId, audioQualityFormat, connectionMetered)
        formatCache[videoId] = formatUri
    }

    val newHeaders = mutableMapOf<String, String>()
    newHeaders.putAll(httpRequestHeaders)
    
    val ghostCookie = Store.getCookie()
    if (ghostCookie.isNotBlank()) {
        newHeaders["Cookie"] = ghostCookie
    }
    
    val iosVisitorData = Store.getIosVisitorData()
    if (!iosVisitorData.isNullOrBlank() && iosVisitorData != "null") {
        newHeaders["X-Goog-Visitor-Id"] = iosVisitorData
    }

    buildUpon()
        .setUri(formatUri)
        .setHttpRequestHeaders(newHeaders)
        .setUriPositionOffset(uriPositionOffset)
        .build()
}

// ──────────────────────────────────────────────────────────────────────────────
// DataSource factories (ExoPlayer integration)
// ──────────────────────────────────────────────────────────────────────────────

@UnstableApi
fun PlayerServiceModern.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")
        val isLocal = dataSpec.uri.scheme == ContentResolver.SCHEME_CONTENT ||
                      dataSpec.uri.scheme == ContentResolver.SCHEME_FILE

        if (isLocal) return@Factory dataSpec

        CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongInfo(videoId) }

        dataSpec.process(videoId, audioQualityFormat, applicationContext.isConnectionMetered())
            .buildUpon()
            .setKey(videoId)
            .build()
    }

    val lruCacheFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)

    return CacheDataSource.Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(lruCacheFactory)
        .setCacheWriteDataSinkFactory(null)
        .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
}

@UnstableApi
fun MyDownloadHelper.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")

        CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongInfo(videoId) }

        dataSpec.process(videoId, audioQualityFormat, appContext().isConnectionMetered())
            .buildUpon()
            .setKey(videoId)
            .build()
    }

    return CacheDataSource.Factory()
        .setCache(getDownloadCache(appContext()))
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
        .setCacheWriteDataSinkFactory(null)
}
