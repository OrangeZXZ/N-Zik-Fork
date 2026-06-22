package app.n_zik.android.components.player.lyrics

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import app.it.fast4x.rimusic.cleanPrefix
import app.n_zik.android.models.Lyrics
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
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.milliseconds

import app.n_zik.android.enums.lyrics.LyricsType

@Composable
fun LyricsFetcher(
    mediaId: String,
    lyricsType: LyricsType,
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
    val lastKaraokeAttemptMediaId = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val lastSyncedAttemptMediaId = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    LaunchedEffect(mediaId, lyricsType, checkLyrics) {
        Database.lyricsTable
            .findBySongId(mediaId)
            .collect { currentLyrics ->
                val wantSynced = lyricsType != LyricsType.Unsynced
                val wantKaraoke = lyricsType == LyricsType.Karaoke
                val hasWordTimings = currentLyrics?.synced?.lines()?.any { it.trim().startsWith("<") && it.contains(":") && it.contains(">") } == true

                val needKaraokeFetch = wantKaraoke && !hasWordTimings && lastKaraokeAttemptMediaId.value != mediaId
                val needSyncedFetch = lyricsType == LyricsType.Synced && hasWordTimings && lastSyncedAttemptMediaId.value != mediaId

                if ((wantSynced && currentLyrics?.synced == null) || needKaraokeFetch || needSyncedFetch) {
                    if (needKaraokeFetch) {
                        lastKaraokeAttemptMediaId.value = mediaId
                    }
                    if (needSyncedFetch) {
                        lastSyncedAttemptMediaId.value = mediaId
                    }

                    if (currentLyrics?.synced == null || needSyncedFetch) {
                        onLyricsUpdated(null)
                    } else {
                        onLyricsUpdated(currentLyrics)
                    }

                    var duration = withContext(Dispatchers.Main) {
                        durationProvider()
                    }

                    while (duration == C.TIME_UNSET) {
                        delay(100)
                        duration = withContext(Dispatchers.Main) {
                            durationProvider()
                        }
                    }

                    val fetchLrcLibAndKugou: suspend (betterLyricsFallback: String?) -> Unit = { fallbackSynced ->
                        if (currentLyrics?.synced == null || needSyncedFetch) {
                            if (fallbackSynced != null) {
                                if (playerEnableLyricsPopupMessage) coroutineScope.launch { Toaster.s(R.string.info_lyrics_found_on_s, "BetterLyrics (Synced)") }
                                onErrorUpdated(false)
                                onCheckedLrcUpdated(true)
                                Database.asyncTransaction { lyricsTable.upsert(Lyrics(songId = mediaId, fixed = currentLyrics?.fixed, synced = fallbackSynced)) }
                            } else {
                                kotlin.runCatching {
                                LrcLib.lyrics(
                                    artist = artistName ?: "",
                                    title = title ?: "",
                                    duration = duration.milliseconds,
                                    album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
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
                                            if (currentLyrics?.synced != null) {
                                                onLyricsUpdated(currentLyrics)
                                            }
                                        }
                                    }.onFailure {
                                        Timber.e("Lyrics Kugou get error ${it.stackTraceToString()}")
                                        if (currentLyrics?.synced != null) {
                                            onLyricsUpdated(currentLyrics)
                                        }
                                    }
                                }
                            }.onFailure {
                                Timber.e("Lyrics get error ${it.stackTraceToString()}")
                                if (currentLyrics?.synced != null) {
                                    onLyricsUpdated(currentLyrics)
                                }
                            }
                        }
                    }
                }

                    if (wantKaraoke) {
                        kotlin.runCatching {
                            com.metrolist.music.betterlyrics.BetterLyrics.getLyrics(
                                title = cleanPrefix(title ?: ""),
                                artist = artistName ?: "",
                                duration = duration.milliseconds.inWholeSeconds.toInt(),
                                album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                            ).onSuccess { ttmlStr ->
                                val hasWordTimings = ttmlStr.lines().any { it.trim().startsWith("<") && it.contains(":") && it.contains(">") }
                                if (ttmlStr.isNotEmpty()) {
                                    if (hasWordTimings) {
                                        if (playerEnableLyricsPopupMessage) {
                                            coroutineScope.launch {
                                                Toaster.s(
                                                    R.string.info_lyrics_found_on_s,
                                                    "BetterLyrics (Karaoke)"
                                                )
                                            }
                                        }

                                        onErrorUpdated(false)
                                        onCheckedLrcUpdated(true)

                                        Database.asyncTransaction {
                                            lyricsTable.upsert(
                                                Lyrics(
                                                    songId = mediaId,
                                                    fixed = currentLyrics?.fixed,
                                                    synced = ttmlStr
                                                )
                                            )
                                        }
                                    } else {
                                        // Pass BetterLyrics Synced as fallback, check LrcLib/KuGou for Karaoke
                                        fetchLrcLibAndKugou(ttmlStr)
                                    }
                                } else {
                                    fetchLrcLibAndKugou(null)
                                }
                            }.onFailure {
                                fetchLrcLibAndKugou(null)
                            }
                        }.onFailure {
                            Timber.e("Lyrics BetterLyrics get error ${it.stackTraceToString()}")
                            fetchLrcLibAndKugou(null)
                        }
                    } else {
                        fetchLrcLibAndKugou(null)
                    }

                } else if (!wantSynced && currentLyrics?.fixed == null) {
                    onErrorUpdated(false)
                    onLyricsUpdated(null)
                    kotlin.runCatching {
                        Innertube.lyrics(NextBody(videoId = mediaId))
                            ?.onSuccess { fixedLyrics ->
                                if (fixedLyrics?.isNotEmpty() == true && playerEnableLyricsPopupMessage) {
                                    coroutineScope.launch {
                                        Toaster.s(
                                            R.string.info_lyrics_found_on_s,
                                            "YouTube"
                                        )
                                    }
                                } else if (playerEnableLyricsPopupMessage) {
                                    coroutineScope.launch {
                                        Toaster.e(
                                            R.string.info_lyrics_not_found_on_s,
                                            "YouTube",
                                            duration = Toast.LENGTH_LONG
                                        )
                                    }
                                }
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
                                if (playerEnableLyricsPopupMessage) {
                                    coroutineScope.launch {
                                        Toaster.e(
                                            R.string.info_lyrics_not_found_on_s,
                                            "YouTube",
                                            duration = Toast.LENGTH_LONG
                                        )
                                    }
                                }
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

