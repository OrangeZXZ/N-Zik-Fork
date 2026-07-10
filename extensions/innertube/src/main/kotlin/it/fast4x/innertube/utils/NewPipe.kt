package it.fast4x.innertube.utils

import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.PlayerResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.Proxy


class NewPipeDownloaderImpl(private val clientProvider: () -> OkHttpClient) : Downloader() {

    private fun normalizeResponseBody(url: String, body: String?): String? {
        if (!url.contains("returnyoutubedislikeapi.com", ignoreCase = true)) {
            return body
        }
        val trimmed = body?.trimStart().orEmpty()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return body
        }
        return "{\"likes\":0,\"dislikes\":0,\"viewCount\":0}"
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", Context.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = clientProvider().newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = normalizeResponseBody(url, response.body?.string())

        val latestUrl = response.request.url.toString()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBodyToReturn,
            responseBodyToReturn?.toByteArray(),
            latestUrl
        )
    }

    override fun executeAsync(request: Request, callback: org.schabi.newpipe.extractor.downloader.Downloader.AsyncCallback?): org.schabi.newpipe.extractor.downloader.CancellableCall {
        TODO("Placeholder")
    }

}

object NewPipeUtils {

    fun init(clientProvider: () -> OkHttpClient) {
        NewPipe.init(NewPipeDownloaderImpl(clientProvider))
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            val url = format.url ?: format.signatureCipher?.let { signatureCipher ->
                val params = parseQueryString(signatureCipher)
                val obfuscatedSignature = params["s"]
                    ?: throw ParsingException("Could not parse cipher signature")
                val signatureParam = params["sp"]
                    ?: throw ParsingException("Could not parse cipher signature parameter")
                val url = params["url"]?.let { URLBuilder(it) }
                    ?: throw ParsingException("Could not parse cipher url")
                url.parameters[signatureParam] =
                    YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                        videoId,
                        obfuscatedSignature
                    )
                url.toString()
            } ?: throw ParsingException("Could not find format url")

            return@runCatching YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url
            )
        }

    fun decodeSignatureCipher(
        videoId: String,
        signatureCipher: String,
    ): String? =
        try {
            val params = parseQueryString(signatureCipher)
            val obfuscatedSignature = params["s"] ?: throw ParsingException("Could not parse cipher signature")
            val signatureParam = params["sp"] ?: throw ParsingException("Could not parse cipher signature parameter")
            val url = params["url"]?.let { URLBuilder(it) } ?: throw ParsingException("Could not parse cipher url")
            url.parameters[signatureParam] = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSignature)
            println("NewPipe: decodeSignatureCipher URL $url")
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url.toString())
        } catch (e: Exception) {
            println("NewPipe: decodeSignatureCipher error: ${e.stackTraceToString()}")
            null
        }

    /**
     * Fetch full stream info via NewPipe extractor and return itag-to-URL pairs.
     * Used as a last-resort fallback when all clients fail to provide a working stream URL.
     */
    fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        return try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId",
            )
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull {
                (it.itagItem?.id ?: return@mapNotNull null) to it.content
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Enrich a PlayerResponse with NewPipe stream URLs by matching itags.
     * Returns null if NewPipe has no streams or the response is not OK.
     */
    fun enrichWithNewPipe(videoId: String, response: PlayerResponse): PlayerResponse? {
        if (response.playabilityStatus?.status != "OK") return null

        val streamsList = newPipePlayer(videoId)
        if (streamsList.isEmpty()) return null

        return response.copy(
            streamingData = response.streamingData?.copy(
                formats = response.streamingData?.formats?.map { format ->
                    format.copy(
                        url = streamsList.find { it.first == format.itag }?.second ?: format.url,
                    )
                },
                adaptiveFormats = response.streamingData?.adaptiveFormats?.map { adaptiveFormat ->
                    adaptiveFormat.copy(
                        url = streamsList.find { it.first == adaptiveFormat.itag }?.second ?: adaptiveFormat.url,
                    )
                },
            ),
        )
    }

}