package app.kreate.android.service

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
import app.n_zik.android.core.network.Store
import app.kreate.android.utils.CharUtils
import com.google.gson.Gson
import com.grack.nanojson.JsonObject
import io.ktor.client.statement.bodyAsText
import java.net.URL
import java.net.HttpURLConnection
import app.n_zik.android.core.network.NetworkClientFactory
import io.ktor.client.request.head
import io.ktor.http.isSuccess
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.Innertube.createPoTokenChallenge
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
import app.kreate.android.me.knighthat.utils.Toaster
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper
import java.net.UnknownHostException
import timber.log.Timber

private const val CHUNK_LENGTH = 512 * 1024L     // 512Kb

/**
 * Store id of song just added to the database.
 * This is created to reduce load to Room
 */
@set:Synchronized
private var justInserted: String = ""

/**
 * Reach out to `next` endpoint for song's information.
 *
 * Info includes:
 * - Titles
 * - Artist(s)
 * - Album
 * - Thumbnails
 * - Duration
 *
 * ### If song IS already inside database
 *
 * It'll replace unmodified columns with fetched data
 *
 * ### If song IS NOT already inside database
 *
 * New record will be created and insert into database
 *
 */
@Blocking
private fun upsertSongInfo( videoId: String ) = runBlocking {       // Use this to prevent suspension of thread while waiting for response from YT

    // Skip adding if it's just added in previous call
    if( videoId == justInserted ) return@runBlocking

    Innertube.nextPage( NextBody(videoId = videoId) )?.fold(
        onSuccess = { nextPage ->
            val songItem = nextPage.itemsPage?.items?.firstOrNull() ?: return@fold
            Database.upsert( songItem )
        },
        onFailure = {
            when( it ) {
                // [UnknownHostException] means no internet connection in most cases
                // Set [justInserted] to this video will skip subsequence calls
                is UnknownHostException -> justInserted = videoId
                else                    -> Toaster.e( R.string.failed_to_fetch_original_property )
            }
        }
    )
}

/**
 * Upsert provided format to the database
 */
@NonBlocking
private fun upsertSongFormat( videoId: String, format: PlayerResponse.StreamingData.Format ) {
    // Skip adding if it's just added in previous call
    if( videoId == justInserted ) return

    runCatching {
        Database.asyncTransaction {
            // Ensure Song exists first to satisfy Foreign Key constraint
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

        // Format must be added successfully before setting variable
        justInserted = videoId
    }
}

//<editor-fold defaultstate="collapsed" desc="Extractors">
private val jsonParser =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        useArrayPolymorphism = true
        explicitNulls = false
    }

@UnstableApi
private fun checkPlayability( playabilityStatus: PlayerResponse.PlayabilityStatus? ) {
    if( playabilityStatus?.status != "OK" )
        when( playabilityStatus?.status ) {
            "LOGIN_REQUIRED"    -> throw LoginRequiredException()
            "UNPLAYABLE"        -> throw UnplayableException()
            else                -> throw UnknownException()
        }
}

private fun extractFormat(
    streamingData: PlayerResponse.StreamingData?,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): PlayerResponse.StreamingData.Format? =
    when (audioQualityFormat) {
        AudioQualityFormat.High -> streamingData?.highestQualityFormat
        AudioQualityFormat.Medium -> streamingData?.mediumQualityFormat
        AudioQualityFormat.Low -> streamingData?.lowestQualityFormat
        AudioQualityFormat.Auto ->
            if (connectionMetered && isConnectionMeteredEnabled())
                streamingData?.mediumQualityFormat
            else
                streamingData?.autoMaxQualityFormat
    }

@UnstableApi
private fun getFormatUrl(
    videoId: String,
    responseJson: JsonObject,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean,
): Uri {
    val jsonString = Gson().toJson( responseJson )
    val playerResponse = jsonParser.decodeFromString<PlayerResponse>( jsonString )

    checkPlayability( playerResponse.playabilityStatus )

    val format = extractFormat( playerResponse.streamingData, audioQualityFormat, connectionMetered )
    val url = format?.url

    if (url.isNullOrEmpty()) {
        throw UnplayableException()
    }

    format?.let {
        CoroutineScope( Threads.DATASPEC_DISPATCHER ).launch { upsertSongFormat( videoId, it ) }
    }

    return YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated( videoId, url )
                                         .toUri()
                                         .buildUpon()
                                         .appendQueryParameter( "range", "0-${format?.contentLength ?: 1_000_000}" )
                                         .build()
}

@UnstableApi
suspend fun getAndroidReelFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val cpn = CharUtils.randomString( 16 )
    
    val country = java.util.Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "GB"
    val language = java.util.Locale.getDefault().language.takeIf { it.isNotEmpty() } ?: "en"
    val contentCountry = ContentCountry(country)
    val localization = Localization(language)
    
    val response = YoutubeStreamHelper.getAndroidReelPlayerResponse( contentCountry, localization, videoId, cpn )
    val uriWithoutCpn = getFormatUrl( videoId, response, audioQualityFormat, connectionMetered )
    
    if (!NetworkClientFactory.validateStreamUrl(uriWithoutCpn.toString())) {
        throw UnplayableException()
    }
    
    return uriWithoutCpn.buildUpon().appendQueryParameter("cpn", cpn).build()
}

private fun String.getPoToken(): String? =
    this.replace("[", "")
        .replace("]", "")
        .split(",")
        .findLast { it.contains("\"") }
        ?.replace("\"", "")

private suspend fun generateIosPoToken() =
    createPoTokenChallenge().bodyAsText()
        .let { challenge ->
            val listChallenge = jsonParser.decodeFromString<List<String?>>(challenge)
            listChallenge.filterIsInstance<String>().firstOrNull()
        }?.let { poTokenChallenge ->
            Innertube.generatePoToken(poTokenChallenge)
                .bodyAsText()
                .getPoToken()
        }

@UnstableApi
suspend fun getIosFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val cpn = CharUtils.randomString( 16 )
    val visitorData = Store.getIosVisitorData()
    val playerRequestToken = generateIosPoToken().orEmpty()
    val poTokenResult = PoTokenResult(visitorData, playerRequestToken, null )
    
    val country = java.util.Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "GB"
    val language = java.util.Locale.getDefault().language.takeIf { it.isNotEmpty() } ?: "en"
    val contentCountry = ContentCountry(country)
    val localization = Localization(language)
    
    val response = YoutubeStreamHelper.getIosPlayerResponse( contentCountry, localization, videoId, cpn, poTokenResult )
    val uriWithoutCpn = getFormatUrl( videoId, response, audioQualityFormat, connectionMetered )
    
    if (!NetworkClientFactory.validateStreamUrl(uriWithoutCpn.toString())) {
        throw UnplayableException()
    }
    
    return uriWithoutCpn.buildUpon().appendQueryParameter("cpn", cpn).build()
}
//</editor-fold>

private val formatCache = mutableMapOf<String, Uri>()

@UnstableApi
fun DataSpec.process(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): DataSpec = runBlocking( Dispatchers.IO ) {
    var formatUri = formatCache[videoId]

    if (formatUri != null) {
        val expireTime = formatUri.getQueryParameter("expire")?.toLongOrNull()?.times(1000)
        if (expireTime != null && System.currentTimeMillis() >= expireTime - 30_000) {
            formatCache.remove(videoId)
            formatUri = null
        }
    }

    if (formatUri == null) {
        var retries = 0
        var successUri: Uri? = null
        var lastException: Exception? = null

        while (retries < 7 && successUri == null) {
            try {
                successUri = try {
                    getAndroidReelFormatUrl( videoId, audioQualityFormat, connectionMetered )
                } catch ( e: Exception ) {
                    when( e ) {
                        is LoginRequiredException,
                        is UnplayableException -> getIosFormatUrl( videoId, audioQualityFormat, connectionMetered )
                        else -> throw e
                    }
                }
            } catch (e: UnplayableException) {
                lastException = e
                retries++
                Timber.w("Stream extraction failed on attempt $retries for $videoId, retrying...")
                if (retries < 7) {
                    kotlinx.coroutines.delay(300) // small delay before retrying
                }
            }
        }

        if (successUri == null) {
            throw lastException ?: UnplayableException()
        }

        formatUri = successUri
        formatCache[videoId] = formatUri
    }

    withUri( formatUri ).subrange( uriPositionOffset )
}

//<editor-fold defaultstate="collapsed" desc="Data source factories">
@UnstableApi
fun PlayerServiceModern.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    // This factory resolves the Video ID to a real URL when needed (cache miss)
    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")
        val isLocal = dataSpec.uri.scheme == ContentResolver.SCHEME_CONTENT || dataSpec.uri.scheme == ContentResolver.SCHEME_FILE

        if (isLocal) {
            return@Factory dataSpec
        }

        // Only upsert info if we are actually resolving (cache miss)
        CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongInfo(videoId) }

        // Always resolve URL for non-local files and ensure key is set to videoId
        // This ensures CacheDataSource uses the correct key even if URI changes
        dataSpec.process(videoId, audioQualityFormat, applicationContext.isConnectionMetered())
            .buildUpon()
            .setKey(videoId)
            .build()
    }

    // LRU Cache (Writable) - Upstream is the Resolver
    val lruCacheFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)

    // Download Cache (Read-Only during playback) - Upstream is LRU Cache
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

    // Download Cache (Writable for downloads? No, CacheWriter handles writing)
    // We expose it as a source that can read from cache if available, or resolve upstream.
    return CacheDataSource.Factory()
        .setCache(getDownloadCache(appContext()))
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
        .setCacheWriteDataSinkFactory(null)
}
//</editor-fold>

