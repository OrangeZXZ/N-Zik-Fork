package app.n_zik.android.components.player.lyrics

import android.R.attr.duration
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.milliseconds
import timber.log.Timber

import app.n_zik.android.enums.lyrics.LyricsType

private const val TAG = "LyricsFetcher"

private var globalLastKaraokeAttemptMediaId: String? = null
private var globalLastSyncedAttemptMediaId: String? = null
private var globalLastUnSyncedAttemptMediaId: String? = null

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
    val context = LocalContext.current
    var previousLyricsType by remember { mutableStateOf(lyricsType) }
    var modeSwitchPending by remember { mutableStateOf(false) }
    LaunchedEffect(mediaId, lyricsType, checkLyrics) {
        // Mode switch → reset everything
        if (lyricsType != previousLyricsType) {
            globalLastSyncedAttemptMediaId = null
            globalLastKaraokeAttemptMediaId = null
            globalLastUnSyncedAttemptMediaId = null
            previousLyricsType = lyricsType
            modeSwitchPending = true
            onLyricsUpdated(null)
        }
        Database.lyricsTable
            .findBySongId(mediaId)
            .collect { currentLyrics ->
                val wantSynced = lyricsType != LyricsType.Unsynced
                val wantKaraoke = lyricsType == LyricsType.Karaoke
                val hasWordTimings = currentLyrics?.synced?.lines()?.any { it.trim().startsWith("<") && it.contains(":") && it.contains(">") } == true
                val hasBetterLyricsTags = currentLyrics?.synced?.contains("{agent:") == true
                val forceFetch = modeSwitchPending.also { modeSwitchPending = false }

                val needKaraokeFetch = wantKaraoke && (forceFetch || (hasBetterLyricsTags && !hasWordTimings) || (!hasWordTimings && globalLastKaraokeAttemptMediaId != mediaId))
                val needSyncedFetch = lyricsType == LyricsType.Synced && (forceFetch || globalLastSyncedAttemptMediaId != mediaId)
                val needUnsyncedFetch = lyricsType == LyricsType.Unsynced && (forceFetch || globalLastUnSyncedAttemptMediaId != mediaId)



                if ((wantSynced && currentLyrics?.synced.isNullOrEmpty()) || needKaraokeFetch || needSyncedFetch || needUnsyncedFetch) {

                    if (needKaraokeFetch) {
                        globalLastKaraokeAttemptMediaId = mediaId
                    }
                    if (needSyncedFetch) {
                        globalLastSyncedAttemptMediaId = mediaId
                    }
                    if (needUnsyncedFetch) {
                        globalLastUnSyncedAttemptMediaId = mediaId
                    }

                    // Always clear lyrics when fetching to force UI refresh
                    onLyricsUpdated(null)

                    // Unsync mode → skip synced/karaoke fetch, go directly to unsynced
                    if (needUnsyncedFetch) {
                        var foundUnsynced = false
                        kotlin.runCatching {
                            LrcLib.lyricsUnsynced(
                                artist = artistName ?: "",
                                title = title ?: "",
                                duration = duration.milliseconds
                            )?.onSuccess {
                                if (it?.text?.isNotEmpty() == true && playerEnableLyricsPopupMessage)
                                    coroutineScope.launch { Toaster.s(R.string.info_lyrics_found_on_s, context.getString(R.string.source_lrclib_unsynced)) }
                                if (it?.text?.isNotEmpty() != true)
                                    if (playerEnableLyricsPopupMessage) coroutineScope.launch { Toaster.e(R.string.info_lyrics_not_found_on_s, context.getString(R.string.source_lrclib_unsynced)) }
                                onErrorUpdated(it?.text?.isNotEmpty() != true)
                                onCheckedLrcUpdated(true)
                                Database.asyncTransaction { lyricsTable.upsert(Lyrics(songId = mediaId, fixed = it?.text, synced = currentLyrics?.synced)) }
                                onLyricsUpdated(currentLyrics)
                                foundUnsynced = true
                            }?.onFailure {
                                Timber.tag(TAG).e("→ LrcLib UNSYNCED ERROR: ${it.stackTraceToString()}")
                            }
                        }
                        if (!foundUnsynced) {
                            tryYouTubeUnsynced(mediaId, mediaMetadata, coroutineScope, playerEnableLyricsPopupMessage, onErrorUpdated, onCheckedInnertubeUpdated, onLyricsUpdated, currentLyrics, context)
                        }
                    } else {

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
                        // BetterLyrics synced fallback → always store
                            if (!fallbackSynced.isNullOrEmpty()) {
                                if (playerEnableLyricsPopupMessage) Toaster.s(R.string.info_lyrics_found_on_s, context.getString(R.string.source_betterlyrics_synced))
                            onErrorUpdated(false)
                            onCheckedLrcUpdated(true)
                            Database.asyncTransaction { lyricsTable.upsert(Lyrics(songId = mediaId, fixed = currentLyrics?.fixed, synced = fallbackSynced)) }
                        } else if (currentLyrics?.synced.isNullOrEmpty() || needSyncedFetch) {
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
                                                context.getString(R.string.source_lrclib_synced)
                                            )
                                        }
                                    else
                                        if (playerEnableLyricsPopupMessage)
                                            coroutineScope.launch {
                                                Toaster.e(
                                                    R.string.info_lyrics_not_found_on_s,
                                                    context.getString(R.string.source_lrclib_synced),
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
                                                context.getString(R.string.source_lrclib_synced), context.getString(R.string.source_kugou_synced),
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
                                            val hasContent = it?.value?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true
                                            if (hasContent) {
                                                if (playerEnableLyricsPopupMessage)
                                                    coroutineScope.launch {
                                                        Toaster.s(
                                                            R.string.info_lyrics_found_on_s,
                                                            context.getString(R.string.source_kugou_synced)
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
                                            } else {
                                                if (playerEnableLyricsPopupMessage)
                                                    coroutineScope.launch {
                                                        Toaster.e(
                                                            R.string.info_lyrics_not_found_on_s_try_on_s,
                                                            context.getString(R.string.source_kugou_synced),
                                                            context.getString(R.string.source_lrclib_unsynced)
                                                        )
                                                    }
                                                onCheckedKugouUpdated(true)
                                                kotlin.runCatching {
                                                    LrcLib.lyricsUnsynced(
                                                        artist = artistName ?: "",
                                                        title = title ?: "",
                                                        duration = duration.milliseconds,
                                                        album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                                                    )?.onSuccess {
                                                        val hasContent = it?.plainText?.isNotEmpty() == true
                                                        if (hasContent) {
                                                            if (playerEnableLyricsPopupMessage)
                                                                coroutineScope.launch {
                                                                    Toaster.s(
                                                                        R.string.info_lyrics_found_on_s,
                                                                        context.getString(R.string.source_lrclib_unsynced)
                                                                    )
                                                                }
                                                            onErrorUpdated(false)
                                                            onCheckedLrcUpdated(true)
                                                            Database.asyncTransaction {
                                                                lyricsTable.upsert(
                                                                    Lyrics(
                                                                        songId = mediaId,
                                                                        fixed = it?.plainText.orEmpty(),
                                                                        synced = currentLyrics?.synced
                                                                    )
                                                                )
                                                            }
                                                        } else {
                                                            onCheckedLrcUpdated(true)
                                                            tryYouTubeUnsynced(mediaId, mediaMetadata, coroutineScope, playerEnableLyricsPopupMessage, onErrorUpdated, onCheckedInnertubeUpdated, onLyricsUpdated, currentLyrics, context)
                                                        }
                                                    }?.onFailure {
                                                        onCheckedLrcUpdated(true)
                                                        tryYouTubeUnsynced(mediaId, mediaMetadata, coroutineScope, playerEnableLyricsPopupMessage, onErrorUpdated, onCheckedInnertubeUpdated, onLyricsUpdated, currentLyrics, context)
                                                    }
                                                }.onFailure {
                                                    Timber.tag(TAG).e("→ LrcLib(U) ERROR: ${it.stackTraceToString()}")
                                                    onCheckedLrcUpdated(true)
                                                    tryYouTubeUnsynced(mediaId, mediaMetadata, coroutineScope, playerEnableLyricsPopupMessage, onErrorUpdated, onCheckedInnertubeUpdated, onLyricsUpdated, currentLyrics, context)
                                                }
                                            }
                                        }?.onFailure {
                                            if (playerEnableLyricsPopupMessage)
                                                coroutineScope.launch {
                                                    Toaster.e(
                                                        R.string.info_lyrics_not_found_on_s_try_on_s,
                                                        context.getString(R.string.source_kugou_synced),
                                                        context.getString(R.string.source_lrclib_unsynced)
                                                    )
                                                }

                                            kotlin.runCatching {
                                                LrcLib.lyricsUnsynced(
                                                    artist = artistName ?: "",
                                                    title = title ?: "",
                                                    duration = duration.milliseconds,
                                                    album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                                                )?.onSuccess {
                                                    val hasContent = it?.plainText?.isNotEmpty() == true
                                                    if (hasContent) {
                                                        if (playerEnableLyricsPopupMessage)
                                                            coroutineScope.launch {
                                                                Toaster.s(
                                                                    R.string.info_lyrics_found_on_s,
                                                                    context.getString(R.string.source_lrclib_unsynced)
                                                                )
                                                            }
                                                        onErrorUpdated(false)
                                                        onCheckedLrcUpdated(true)
                                                        Database.asyncTransaction {
                                                            lyricsTable.upsert(
                                                                Lyrics(
                                                                    songId = mediaId,
                                                                    fixed = it?.plainText.orEmpty(),
                                                                    synced = currentLyrics?.synced
                                                                )
                                                            )
                                                        }
                                                    } else {
                                                        onCheckedLrcUpdated(true)
                                                        tryYouTubeUnsynced(mediaId, mediaMetadata, coroutineScope, playerEnableLyricsPopupMessage, onErrorUpdated, onCheckedInnertubeUpdated, onLyricsUpdated, currentLyrics, context)
                                                    }
                                                }?.onFailure {
                                                    if (playerEnableLyricsPopupMessage)
                                                        coroutineScope.launch {
                                                            Toaster.e(
                                                                R.string.info_lyrics_not_found_on_s_try_on_s,
                                                                context.getString(R.string.source_lrclib_unsynced),
                                                                context.getString(R.string.source_youtube_unsynced)
                                                            )
                                                        }
                                                    onCheckedLrcUpdated(true)
                                                    tryYouTubeUnsynced(mediaId, mediaMetadata, coroutineScope, playerEnableLyricsPopupMessage, onErrorUpdated, onCheckedInnertubeUpdated, onLyricsUpdated, currentLyrics, context)
                                                }
                                            }.onFailure {
                                                Timber.tag(TAG).e("→ LrcLib(U) ERROR: ${it.stackTraceToString()}")
                                                if (playerEnableLyricsPopupMessage)
                                                    coroutineScope.launch {
                                                        Toaster.e(
                                                            R.string.info_lyrics_not_found_on_s_try_on_s,
                                                            context.getString(R.string.source_lrclib_unsynced),
                                                            context.getString(R.string.source_youtube_unsynced)
                                                        )
                                                    }
                                                onCheckedLrcUpdated(true)
                                                tryYouTubeUnsynced(mediaId, mediaMetadata, coroutineScope, playerEnableLyricsPopupMessage, onErrorUpdated, onCheckedInnertubeUpdated, onLyricsUpdated, currentLyrics, context)
                                            }
                                        }
                                    }.onFailure {
                                        Timber.tag(TAG).e("→ KuGou ERROR: ${it.stackTraceToString()}")
                                        if (!currentLyrics?.synced.isNullOrEmpty()) {
                                            onLyricsUpdated(currentLyrics)
                                        }
                                    }
                                }
                            }.onFailure {
                                Timber.tag(TAG).e("→ LrcLib ERROR: ${it.stackTraceToString()}")
                                if (!currentLyrics?.synced.isNullOrEmpty()) {
                                    onLyricsUpdated(currentLyrics)
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
                                val hasKaraokeTimings = ttmlStr.lines().any { it.trim().startsWith("<") && it.contains(":") && it.contains(">") }
                                if (ttmlStr.isNotEmpty()) {
                                    if (hasKaraokeTimings) {
                                        if (playerEnableLyricsPopupMessage) {
                                            Toaster.s(
                                                R.string.info_lyrics_found_on_s,
                                                context.getString(R.string.source_betterlyrics_karaoke)
                                            )
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
                                        // BetterLyrics found synced lyrics (no word timings)
                                        if (!forceFetch && (hasBetterLyricsTags || !currentLyrics?.synced.isNullOrEmpty())) {
                                            // Same mode re-fetch, already have synced → warning, keep existing
                                            if (playerEnableLyricsPopupMessage) {
                                                Toaster.w(R.string.info_karaoke_not_found_showing_sync, context.getString(R.string.source_betterlyrics_karaoke), context.getString(R.string.source_betterlyrics_synced))
                                            }
                                        } else {
                                            // Mode switch or no existing lyrics → store synced
                                            if (playerEnableLyricsPopupMessage) {
                                                Toaster.s(R.string.info_lyrics_found_on_s, context.getString(R.string.source_betterlyrics_synced))
                                            }
                                            fetchLrcLibAndKugou(ttmlStr)
                                        }
                                    }
                                } else {
                                    if (playerEnableLyricsPopupMessage) {
                                        Toaster.e(
                                            R.string.info_lyrics_not_found_on_s_try_on_s,
                                            context.getString(R.string.source_betterlyrics_karaoke),
                                            context.getString(R.string.source_betterlyrics_synced)
                                        )
                                    }
                                    fetchLrcLibAndKugou(null)
                                }
                            }.onFailure {
                                if (playerEnableLyricsPopupMessage) {
                                    Toaster.e(
                                        R.string.info_lyrics_not_found_on_s_try_on_s,
                                        context.getString(R.string.source_betterlyrics_karaoke),
                                        context.getString(R.string.source_betterlyrics_synced)
                                    )
                                }
                                fetchLrcLibAndKugou(null)
                            }
                        }.onFailure {
                            Timber.tag(TAG).e("→ BetterLyrics KARAOKE ERROR: ${it.stackTraceToString()}")
                            if (playerEnableLyricsPopupMessage) {
                                Toaster.e(
                                    R.string.info_lyrics_not_found_on_s_try_on_s,
                                    context.getString(R.string.source_betterlyrics_karaoke),
                                    context.getString(R.string.source_betterlyrics_synced)
                                )
                            }
                            fetchLrcLibAndKugou(null)
                        }
                    } else {
                        fetchLrcLibAndKugou(null)
                    }
                    } // end else (not needUnsyncedFetch)

                } else if (!currentLyrics?.synced.isNullOrEmpty() || !currentLyrics?.fixed.isNullOrEmpty()) {
                    // No fetch needed — just update UI with current lyrics
                    onLyricsUpdated(currentLyrics)
                }

                if (!wantSynced && currentLyrics?.fixed == null && globalLastUnSyncedAttemptMediaId != mediaId) {
                    globalLastUnSyncedAttemptMediaId = mediaId
                    onErrorUpdated(false)
                    onLyricsUpdated(null)

                    var foundUnsynced = false

                    kotlin.runCatching {
                        LrcLib.lyricsUnsynced(
                            artist = artistName ?: "",
                            title = title ?: "",
                            duration = duration.milliseconds,
                            album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                        )?.onSuccess {
                            val hasContent = it?.plainText?.isNotEmpty() == true
                            if (hasContent) {
                                if (playerEnableLyricsPopupMessage)
                                    coroutineScope.launch {
                                        Toaster.s(
                                            R.string.info_lyrics_found_on_s,
                                            context.getString(R.string.source_lrclib_unsynced)
                                        )
                                    }
                                foundUnsynced = true
                                Database.asyncTransaction {
                                    lyricsTable.upsert(
                                        Lyrics(
                                            songId = mediaId,
                                            fixed = it?.plainText.orEmpty(),
                                            synced = currentLyrics?.synced
                                        )
                                    )
                                }
                            }
                        }?.onFailure {
                            if (playerEnableLyricsPopupMessage)
                                coroutineScope.launch {
                                    Toaster.e(
                                        R.string.info_lyrics_not_found_on_s_try_on_s,
                                        context.getString(R.string.source_lrclib_unsynced),
                                        context.getString(R.string.source_youtube_unsynced)
                                    )
                                }
                        }
                    }.onFailure {
                        Timber.tag(TAG).e("→ LrcLib(U) ERROR: ${it.stackTraceToString()}")
                        if (playerEnableLyricsPopupMessage)
                            coroutineScope.launch {
                                Toaster.e(
                                    R.string.info_lyrics_not_found_on_s_try_on_s,
                                    context.getString(R.string.source_lrclib_unsynced),
                                    context.getString(R.string.source_youtube_unsynced)
                                )
                            }
                    }

                    if (!foundUnsynced) {
                        kotlin.runCatching {
                            Innertube.lyrics(NextBody(videoId = mediaId))
                                ?.onSuccess { fixedLyrics ->
                                    if (fixedLyrics?.isNotEmpty() == true && playerEnableLyricsPopupMessage) {
                                        coroutineScope.launch {
                                            Toaster.s(
                                                R.string.info_lyrics_found_on_s,
                                                context.getString(R.string.source_youtube_unsynced)
                                            )
                                        }
                                    } else if (playerEnableLyricsPopupMessage) {
                                        coroutineScope.launch {
                                            Toaster.e(
                                                R.string.info_lyrics_not_found_on_s,
                                                context.getString(R.string.source_youtube_unsynced),
                                                duration = Toast.LENGTH_LONG
                                            )
                                        }
                                    }
                                    if (!fixedLyrics.isNullOrEmpty()) {
                                        Database.asyncTransaction {
                                            lyricsTable.upsert(
                                                Lyrics(
                                                    songId = mediaId,
                                                    fixed = fixedLyrics,
                                                    synced = currentLyrics?.synced
                                                )
                                            )
                                        }
                                    } else {
                                        onErrorUpdated(true)
                                    }
                                }?.onFailure {
                                    onErrorUpdated(true)
                                }
                        }.onFailure {
                            Timber.tag(TAG).e("→ YouTube(U) ERROR: ${it.stackTraceToString()}")
                        }
                    }
                    onCheckedInnertubeUpdated(true)
                } else {
                    onLyricsUpdated(currentLyrics)
                }
            }
    }
}

private fun tryYouTubeUnsynced(
    mediaId: String,
    mediaMetadata: MediaMetadata,
    coroutineScope: CoroutineScope,
    playerEnableLyricsPopupMessage: Boolean,
    onErrorUpdated: (Boolean) -> Unit,
    onCheckedInnertubeUpdated: (Boolean) -> Unit,
    onLyricsUpdated: (Lyrics?) -> Unit,
    currentLyrics: Lyrics?,
    context: android.content.Context
) {
    coroutineScope.launch {
        kotlin.runCatching {
            Innertube.lyrics(NextBody(videoId = mediaId))
                ?.onSuccess { fixedLyrics ->
                    if (fixedLyrics?.isNotEmpty() == true && playerEnableLyricsPopupMessage) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toaster.s(
                                app.n_zik.android.R.string.info_lyrics_found_on_s,
                                context.getString(R.string.source_youtube_unsynced)
                            )
                        }
                    } else if (playerEnableLyricsPopupMessage) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toaster.e(
                                app.n_zik.android.R.string.info_lyrics_not_found_on_s,
                                context.getString(R.string.source_youtube_unsynced),
                                duration = android.widget.Toast.LENGTH_LONG
                            )
                        }
                    }
                    onCheckedInnertubeUpdated(true)
                    if (!fixedLyrics.isNullOrEmpty()) {
                        app.n_zik.android.core.database.Database.asyncTransaction {
                            lyricsTable.upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = fixedLyrics,
                                    synced = currentLyrics?.synced
                                )
                            )
                        }
                    } else {
                        onErrorUpdated(true)
                    }
                }?.onFailure {
                    onCheckedInnertubeUpdated(true)
                    onErrorUpdated(true)
                    if (!currentLyrics?.synced.isNullOrEmpty()) {
                        onLyricsUpdated(currentLyrics)
                    }
                }
        }.onFailure {
            Timber.tag(TAG).e("→ YouTube(U) ERROR: ${it.stackTraceToString()}")
            onCheckedInnertubeUpdated(true)
            onErrorUpdated(true)
            if (!currentLyrics?.synced.isNullOrEmpty()) {
                onLyricsUpdated(currentLyrics)
            }
        }
    }
}
