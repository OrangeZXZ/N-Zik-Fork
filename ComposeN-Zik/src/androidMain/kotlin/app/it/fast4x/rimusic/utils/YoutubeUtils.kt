package app.it.fast4x.rimusic.utils

import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.utils.NewPipeUtils
import timber.log.Timber

fun getSignatureTimestampOrNull(
    videoId: String
): Int? {
    return NewPipeUtils.getSignatureTimestamp(videoId)
        .onFailure {
            Timber.tag("YoutubeUtils").e("getSignatureTimestampOrNull Error while getting signature timestamp ${it.stackTraceToString()}")
            Timber.tag("YoutubeUtils").e("getSignatureTimestampOrNull Error while getting signature timestamp ${it.stackTraceToString()}")
        }
        .getOrNull()
}

fun getStreamUrl(
    format: PlayerResponse.StreamingData.Format,
    videoId: String
): String? {
    val streamUrl =  NewPipeUtils.getStreamUrl(format, videoId)
        .onFailure {
            Timber.tag("YoutubeUtils").e("getStreamUrlOrNull Error while getting stream url ${it.stackTraceToString()}")
            Timber.tag("YoutubeUtils").e("getStreamUrlOrNull Error while getting stream url ${it.stackTraceToString()}")
        }
        .getOrNull()

    Timber.tag("YoutubeUtils").d("getStreamUrlOrNull streamUrl $streamUrl")

    return streamUrl
}



