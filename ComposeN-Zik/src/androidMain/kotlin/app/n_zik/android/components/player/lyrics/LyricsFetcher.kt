package app.n_zik.android.components.player.lyrics

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.models.Lyrics
import app.n_zik.android.R
import app.n_zik.android.core.database.Database
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.lyrics
import it.fast4x.kugou.KuGou
import it.fast4x.lrclib.LrcLib
import app.kreate.android.me.knighthat.utils.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LyricsFetcher(
    mediaId: String,
    isShowingSynchronizedLyrics: Boolean,
    checkLyrics: Boolean,
    artistName: String?,
    title: String?,
    mediaMetadata: MediaMetadata,
    durationProvider: () -> Long,
    coroutineScope: CoroutineScope,
    playerEnableLyricsPopupMessage: Boolean,
    onLyricsUpdated: (Lyrics?) -> Unit,
    onErrorUpdated: (Boolean) -> Unit,
    onCheckedLrcUpdated: (Boolean) -> Unit,
    onCheckedKugouUpdated: (Boolean) -> Unit,
    onCheckedInnertubeUpdated: (Boolean) -> Unit
) {
    LaunchedEffect(mediaId, isShowingSynchronizedLyrics, checkLyrics) {
        Database.lyricsTable
            .findBySongId(mediaId)
            .collect { currentLyrics ->
                if (isShowingSynchronizedLyrics && currentLyrics?.synced == null) {
                    onLyricsUpdated(null)
                    var duration = withContext(Dispatchers.Main) {
                        durationProvider()
                    }

                    while (duration == C.TIME_UNSET) {
                        delay(100)
                        duration = withContext(Dispatchers.Main) {
                            durationProvider()
                        }
                    }

                    kotlin.runCatching {
                        LrcLib.lyrics(
                            artist = artistName ?: "",
                            title = title ?: "",
                            duration = duration.milliseconds,
                            album = mediaMetadata.albumTitle?.toString()
                        )?.onSuccess {
                            if ((it?.text?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true)
                                && playerEnableLyricsPopupMessage
                            )
                                coroutineScope.launch {
                                    Toaster.s(
                                        R.string.info_lyrics_found_on_s,
                                        "LrcLib.net"
                                    )
                                }
                            else
                                if (playerEnableLyricsPopupMessage)
                                    coroutineScope.launch {

                                        Toaster.e(
                                            R.string.info_lyrics_not_found_on_s,
                                            "LrcLib.net",
                                            duration = Toast.LENGTH_LONG
                                        )
                                    }

                            onErrorUpdated(false)
                            onCheckedLrcUpdated(true)

                            Database.asyncTransaction {
                                lyricsTable.upsert(
                                    Lyrics(
                                        songId = mediaId,
                                        fixed = currentLyrics?.fixed,
                                        synced = it?.text.orEmpty()
                                    )
                                )
                            }
                        }?.onFailure {
                            if (playerEnableLyricsPopupMessage)
                                coroutineScope.launch {
                                    Toaster.e(
                                        R.string.info_lyrics_not_found_on_s_try_on_s,
                                        "LrcLib.net", "KuGou.com",
                                        duration = Toast.LENGTH_LONG
                                    )
                                }

                            onCheckedLrcUpdated(true)

                            kotlin.runCatching {
                                KuGou.lyrics(
                                    artist = mediaMetadata.artist?.toString() ?: "",
                                    title = cleanPrefix(mediaMetadata.title?.toString() ?: ""),
                                    duration = duration / 1000
                                )?.onSuccess {
                                    if ((it?.value?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true)
                                        && playerEnableLyricsPopupMessage
                                    )
                                        coroutineScope.launch {
                                            Toaster.s(
                                                R.string.info_lyrics_found_on_s,
                                                "KuGou.com"
                                            )
                                        }
                                    else
                                        if (playerEnableLyricsPopupMessage)
                                            coroutineScope.launch {
                                                Toaster.e(
                                                    R.string.info_lyrics_not_found_on_s,
                                                    "KuGou.com",
                                                    duration = Toast.LENGTH_LONG
                                                )
                                            }

                                    onErrorUpdated(false)
                                    onCheckedKugouUpdated(true)
                                    Database.asyncTransaction {
                                        lyricsTable.upsert(
                                            Lyrics(
                                                songId = mediaId,
                                                fixed = currentLyrics?.fixed,
                                                synced = it?.value.orEmpty()
                                            )
                                        )
                                    }
                                }?.onFailure {
                                    if (playerEnableLyricsPopupMessage)
                                        coroutineScope.launch {
                                            Toaster.e(
                                                R.string.info_lyrics_not_found_on_s,
                                                "KuGou.com",
                                                duration = Toast.LENGTH_LONG
                                            )
                                        }

                                    onErrorUpdated(true)
                                }
                            }.onFailure {
                                Timber.e("Lyrics Kugou get error ${it.stackTraceToString()}")
                            }
                        }
                    }.onFailure {
                        Timber.e("Lyrics get error ${it.stackTraceToString()}")
                    }

                } else if (!isShowingSynchronizedLyrics && currentLyrics?.fixed == null) {
                    onErrorUpdated(false)
                    onLyricsUpdated(null)
                    kotlin.runCatching {
                        Innertube.lyrics(NextBody(videoId = mediaId))
                            ?.onSuccess { fixedLyrics ->
                                Database.asyncTransaction {
                                    lyricsTable.upsert(
                                        Lyrics(
                                            songId = mediaId,
                                            fixed = fixedLyrics ?: "",
                                            synced = currentLyrics?.synced
                                        )
                                    )
                                }
                            }?.onFailure {
                                onErrorUpdated(true)
                            }
                    }.onFailure {
                        Timber.e("Lyrics Innertube get error ${it.stackTraceToString()}")
                    }
                    onCheckedInnertubeUpdated(true)
                } else {
                    onLyricsUpdated(currentLyrics)
                }
            }
    }
}
