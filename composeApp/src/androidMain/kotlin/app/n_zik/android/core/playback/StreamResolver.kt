package app.n_zik.android.core.playback

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import app.n_zik.android.R

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
import app.it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.net.UnknownHostException
import io.ktor.client.call.body
import timber.log.Timber

private const val TAG = "StreamResolver"
private const val CHUNK_LENGTH = 512 * 1024L
private const val MAX_RESOLVE_RETRIES = 7
private const val INITIAL_RETRY_DELAY_MS = 1500L

// Clients to try in order - mirrors Metrolist's YTPlayerUtils fallback chain
private val FALLBACK_CLIENTS = listOf(
    YouTubeClient.WEB_REMIX, // This corresponds to MAIN_CLIENT in Metrolist
    YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
    YouTubeClient.TVHTML5,
    YouTubeClient.ANDROID_VR_1_43_32,
    YouTubeClient.ANDROID_VR_1_61_48,
    YouTubeClient.ANDROID_CREATOR,
    YouTubeClient.IPADOS,
    YouTubeClient.ANDROID_VR_NO_AUTH,
    YouTubeClient.MOBILE,
    YouTubeClient.IOS,
    YouTubeClient.WEB,
    YouTubeClient.WEB_CREATOR
)

private val WEB_CLIENTS = setOf("WEB", "WEB_REMIX", "WEB_CREATOR", "TVHTML5", "TVHTML5_SIMPLY_EMBEDDED_PLAYER")

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
            formatTable.upsert(Format(
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
 * Checks playability status and throws appropriate exception with the real reason.
 */
@UnstableApi
private fun checkPlayability(playabilityStatus: PlayerResponse.PlayabilityStatus?) {
    if (playabilityStatus?.status != "OK") {
        val reason = playabilityStatus?.reason ?: "Unknown reason (status=${playabilityStatus?.status})"
        when (playabilityStatus?.status) {
            "LOGIN_REQUIRED" -> throw LoginRequiredException("Login required: $reason")
            "UNPLAYABLE"     -> throw UnplayableException("Unplayable: $reason")
            else             -> throw UnknownException("${playabilityStatus?.status ?: "NULL"}: $reason")
        }
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
    if (clientName !in WEB_CLIENTS) {
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
                    val params = io.ktor.http.parseQueryString(sigCipher)
                    val s = params["s"] ?: throw Exception("No signature")
                    val sp = params["sp"] ?: throw Exception("No signature parameter")
                    val urlParam = params["url"] ?: throw Exception("No url")
                    val urlBuilder = io.ktor.http.URLBuilder(urlParam)
                    urlBuilder.parameters[sp] = org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, s)
                    val decUrl = urlBuilder.buildString()
                    org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, decUrl)
                }.onFailure {
                    Timber.tag(TAG).e(it, "NewPipe fallback failed")
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
 * Resolves the stream URI with retry logic.
 *
 * Wraps [resolveStreamUriInternal] with up to [MAX_RESOLVE_RETRIES] attempts,
 * invalidating the format cache between retries. Shows a toast when all
 * attempts are exhausted.
 */
@UnstableApi
private suspend fun resolveStreamUri(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    var lastException: Exception? = null

    for (attempt in 1..MAX_RESOLVE_RETRIES) {
        try {
            return resolveStreamUriInternal(videoId, audioQualityFormat, connectionMetered)
        } catch (e: Exception) {
            lastException = e
            Timber.tag(TAG).w("Resolve attempt $attempt/$MAX_RESOLVE_RETRIES failed for $videoId: ${e.message}")
            if (attempt < MAX_RESOLVE_RETRIES) {
                // Invalidate cached URL so next attempt fetches a fresh one
                formatCache.remove(videoId)
                delay(INITIAL_RETRY_DELAY_MS * attempt)
            }
        }
    }

    // All retries exhausted â€” show toast with the real reason
    Timber.tag(TAG).e("All $MAX_RESOLVE_RETRIES resolve attempts failed for $videoId")
    val errorDetail = lastException?.message ?: "Unknown error"
    Toaster.e(R.string.error_all_stream_attempts_failed, formatArgs = arrayOf(errorDetail.take(100)))
    throw lastException ?: UnplayableException("All retries exhausted for $videoId")
}

/**
 * Core stream resolution logic (single attempt).
 *
 * Mirrors Metrolist's YTPlayerUtils.playerResponseForPlayback() flow:
 * 1. Fetch signatureTimestamp from player.js
 * 2. Generate PoToken (WebView)
 * 3. Try each client in order â†’ get player response â†’ resolve URL â†’ validate
 * 4. Return the first working stream URL
 */
@UnstableApi
private suspend fun resolveStreamUriInternal(
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
            FunctionNameExtractor.extractSignatureTimestamp(playerJsResult.first)
        } else null
    }.getOrNull()
    Timber.tag(TAG).d("signatureTimestamp: $signatureTimestamp")

    // 2. Generate PoToken for WEB_REMIX (web clients only)
    val poToken: String? = runCatching<String?> {
        val sessionId = Store.getIosVisitorData() ?: ""
        app.n_zik.android.core.utils.potoken.PoTokenGenerator().getWebClientPoToken(videoId, sessionId)?.playerRequestPoToken
    }.getOrNull()
    Timber.tag(TAG).d("poToken generated: ${poToken != null}")

    val isLoggedIn = !Innertube.cookie.isNullOrBlank() && Innertube.cookie?.contains("SAPISID") == true

    // Track the last meaningful failure reason across all clients
    var lastFailureReason: String? = null
    var fallbackUri: Uri? = null
    var fallbackFormat: PlayerResponse.StreamingData.Format? = null

    val prefs = appContext().preferences
    // Use separate keys for logged-in vs logged-out so we don't prioritize a no-auth
    // client (e.g. ANDROID_VR) when logged in, which could miss premium-only tracks.
    val prefKey = if (isLoggedIn) "last_successful_yt_client_auth" else "last_successful_yt_client_noauth"
    val lastSuccessfulClientName = prefs.getString(prefKey, null)
    
    // Sort clients: put the last successful one first, keep the rest in their original order
    val clientsToTry = if (lastSuccessfulClientName != null) {
        val lastSuccessfulClient = FALLBACK_CLIENTS.find { it.clientName == lastSuccessfulClientName }
        if (lastSuccessfulClient != null) {
            Timber.tag(TAG).d("Prioritizing remembered client: ${lastSuccessfulClient.clientName} (${if (isLoggedIn) "auth" else "noauth"})")
            listOf(lastSuccessfulClient) + FALLBACK_CLIENTS.filter { it.clientName != lastSuccessfulClientName }
        } else FALLBACK_CLIENTS
    } else FALLBACK_CLIENTS

    // 3. Try each client in order
    for ((index, ytClient) in clientsToTry.withIndex()) {
        try {
            Timber.tag(TAG).d("Trying client (${index + 1}/${clientsToTry.size}): ${ytClient.clientName} for $videoId")

            if (ytClient.loginRequired && !isLoggedIn) {
                Timber.tag(TAG).d("Skipping client ${ytClient.clientName} - requires login but user is not logged in")
                continue
            }

            val context = ytClient.toContext(
                locale = locale,
                visitorData = visitorData,
                dataSyncId = if (isLoggedIn) Innertube.dataSyncId else null
            )

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
                lastFailureReason = "${ytClient.clientName}: failed to parse PlayerResponse"
                Timber.tag(TAG).w(lastFailureReason)
                continue
            }

            // Check playability â€” capture the reason if not OK
            if (playerResponse.playabilityStatus?.status != "OK") {
                val status = playerResponse.playabilityStatus?.status ?: "NULL"
                val reason = playerResponse.playabilityStatus?.reason ?: "no reason provided"
                lastFailureReason = "$status: $reason"
                Timber.tag(TAG).w("${ytClient.clientName}: status=$status reason=$reason")
                continue
            }

            // Pick best audio format
            val format = pickFormat(playerResponse.streamingData, audioQualityFormat, connectionMetered)
            if (format == null) {
                lastFailureReason = "${ytClient.clientName}: no suitable audio format found"
                Timber.tag(TAG).w(lastFailureReason)
                continue
            }

            // Resolve URL
            val uri = resolveFormatUrl(videoId, format, ytClient.clientName) ?: run {
                lastFailureReason = "${ytClient.clientName}: could not resolve URL for format itag=${format.itag}"
                Timber.tag(TAG).w(lastFailureReason)
                continue
            }

            // Validate (HEAD request)
            val streamUrl = uri.toString()
            val isValid = NetworkClientFactory.validateStreamUrl(streamUrl, ytClient.userAgent)
            if (!isValid) {
                lastFailureReason = "${ytClient.clientName}: stream URL validation failed (403/expired?) for $videoId"
                Timber.tag(TAG).w(lastFailureReason)
                continue
            }

            // Check if metadata is missing. If so, save as fallback and try next client.
            if ((format.bitrate == null || format.bitrate == 0) && (format.contentLength?.toLong() ?: 0L) == 0L) {
                lastFailureReason = "${ytClient.clientName}: stream resolved but missing metadata"
                Timber.tag(TAG).w(lastFailureReason)
                if (fallbackUri == null) {
                    fallbackUri = uri
                    fallbackFormat = format
                }
                continue
            }

            // Success!
            Timber.tag(TAG).d("${ytClient.clientName}: stream resolved successfully for $videoId")
            CoroutineScope(PlaybackDispatchers.STREAM_RESOLVER).launch { upsertSongFormat(videoId, format) }
            prefs.edit().putString(prefKey, ytClient.clientName).apply()

            return uri
                .buildUpon()
                .appendQueryParameter("range", "0-${format.contentLength ?: 1_000_000}")
                .build()

        } catch (e: LoginRequiredException) {
            lastFailureReason = "${ytClient.clientName}: ${e.message ?: "Login required"}"
            Timber.tag(TAG).w(lastFailureReason)
            continue
        } catch (e: io.ktor.client.plugins.ResponseException) {
            lastFailureReason = "${ytClient.clientName}: HTTP ${e.response.status}"
            Timber.tag(TAG).w(lastFailureReason)
            continue
        } catch (e: Exception) {
            lastFailureReason = "${ytClient.clientName}: ${e::class.simpleName}: ${e.message}"
            Timber.tag(TAG).w(lastFailureReason)
            continue
        }
    }

    if (fallbackUri != null && fallbackFormat != null) {
        Timber.tag(TAG).w("All clients failed to provide metadata. Using fallback stream for $videoId")
        CoroutineScope(PlaybackDispatchers.STREAM_RESOLVER).launch { upsertSongFormat(videoId, fallbackFormat!!) }
        return fallbackUri!!
            .buildUpon()
            .appendQueryParameter("range", "0-${fallbackFormat?.contentLength ?: 1_000_000}")
            .build()
    }

    // All clients exhausted â€” throw with the last meaningful reason
    val finalReason = lastFailureReason ?: "All ${FALLBACK_CLIENTS.size} clients failed for $videoId"
    Timber.tag(TAG).e("resolveStreamUri FAILED: $finalReason")
    throw UnplayableException(finalReason)
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Cache + DataSpec integration
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Cache of resolved stream URLs by videoId.
 * Exposed internally so [PlayerServiceModern.onPlayerError] can invalidate stale entries.
 */
internal val formatCache = mutableMapOf<String, Uri>()

@UnstableApi
fun DataSpec.process(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): DataSpec = runBlocking(Dispatchers.IO) {
    val isLoggedIn = Store.getCookie().isNotBlank()
    Timber.tag(TAG).d("Resolving stream for videoId=$videoId, isLoggedIn=$isLoggedIn")

    var formatUri = formatCache[videoId]

    if (formatUri != null) {
        val expireTime = formatUri.getQueryParameter("expire")?.toLongOrNull()?.times(1000)
        val isExpired = expireTime != null && System.currentTimeMillis() >= expireTime - 30_000
        val isUnknownMetadata = formatUri.getQueryParameter("range") == "0-1000000"

        if (isExpired || isUnknownMetadata) {
            Timber.tag(TAG).d("formatCache entry invalid (expired=$isExpired, unknownMetadata=$isUnknownMetadata), removing for $videoId")
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

    // DO NOT ADD Cookie or X-Goog-Visitor-Id here!
    // ExoPlayer sends these headers to the googlevideo.com CDN, which will reject them with 403 Forbidden.
    // The stream URL itself contains all necessary authentication tokens (sig, expire, id).

    buildUpon()
        .setUri(formatUri)
        .setHttpRequestHeaders(newHeaders)
        .setUriPositionOffset(uriPositionOffset)
        .build()
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// DataSource factories (ExoPlayer integration)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@UnstableApi
fun PlayerServiceModern.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")
        val isLocal = dataSpec.uri.scheme == ContentResolver.SCHEME_CONTENT ||
                      dataSpec.uri.scheme == ContentResolver.SCHEME_FILE

        if (isLocal) return@Factory dataSpec

        CoroutineScope(PlaybackDispatchers.STREAM_RESOLVER).launch { upsertSongInfo(videoId) }

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

        CoroutineScope(PlaybackDispatchers.STREAM_RESOLVER).launch { upsertSongInfo(videoId) }

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
