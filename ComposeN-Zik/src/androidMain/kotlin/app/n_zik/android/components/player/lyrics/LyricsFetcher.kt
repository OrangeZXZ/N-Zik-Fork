package app.n_zik.android.components.player.lyrics

import android.R.attr.duration
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import timber.log.Timber
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.milliseconds

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
    LaunchedEffect(mediaId, lyricsType, checkLyrics) {
        Database.lyricsTable
            .findBySongId(mediaId)
            .collect { currentLyrics ->
                val wantSynced = lyricsType != LyricsType.Unsynced
                val wantKaraoke = lyricsType == LyricsType.Karaoke
                val hasWordTimings = currentLyrics?.synced?.lines()?.any { it.trim().startsWith("<") && it.contains(":") && it.contains(">") } == true

                Timber.tag(TAG).d("=== START | mediaId=$mediaId | type=$lyricsType | wantSynced=$wantSynced | wantKaraoke=$wantKaraoke | hasWordTimings=$hasWordTimings | synced=${currentLyrics?.synced?.take(50)} | fixed=${currentLyrics?.fixed?.take(50)}")

                val needKaraokeFetch = wantKaraoke && !hasWordTimings && globalLastKaraokeAttemptMediaId != mediaId
                val needSyncedFetch = lyricsType == LyricsType.Synced && hasWordTimings && globalLastSyncedAttemptMediaId != mediaId

                Timber.tag(TAG).d("needKaraoke=$needKaraokeFetch | needSynced=$needSyncedFetch | lastKaraoke=$globalLastKaraokeAttemptMediaId | lastSynced=$globalLastSyncedAttemptMediaId")

                if ((wantSynced && currentLyrics?.synced.isNullOrEmpty()) || needKaraokeFetch || needSyncedFetch) {
                    Timber.tag(TAG).d("→ Entering fetch block")
                    if (needKaraokeFetch) {
                        globalLastKaraokeAttemptMediaId = mediaId
                    }
                    if (needSyncedFetch) {
                        globalLastSyncedAttemptMediaId = mediaId
                    }

                    if (currentLyrics?.synced.isNullOrEmpty() || needSyncedFetch) {
                        Timber.tag(TAG).d("→ Clearing lyrics (null)")
                        onLyricsUpdated(null)
                    } else {
                        Timber.tag(TAG).d("→ Keeping existing lyrics")
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
                    Timber.tag(TAG).d("→ Duration: $duration ms")

                    val fetchLrcLibAndKugou: suspend (betterLyricsFallback: String?) -> Unit = { fallbackSynced ->
                        Timber.tag(TAG).d("→ fetchLrcLibAndKugou | fallback=${fallbackSynced?.take(50)} | currentSynced=${currentLyrics?.synced?.take(30)} | needSynced=$needSyncedFetch")
                        if (currentLyrics?.synced.isNullOrEmpty() || needSyncedFetch) {
                            if (!fallbackSynced.isNullOrEmpty()) {
                                Timber.tag(TAG).d("→ Using BetterLyrics synced fallback directly")
                                if (playerEnableLyricsPopupMessage) coroutineScope.launch { Toaster.s(R.string.info_lyrics_found_on_s, context.getString(R.string.source_betterlyrics_synced)) }
                                onErrorUpdated(false)
                                onCheckedLrcUpdated(true)
                                Database.asyncTransaction { lyricsTable.upsert(Lyrics(songId = mediaId, fixed = currentLyrics?.fixed, synced = fallbackSynced)) }
                            } else {
                                Timber.tag(TAG).d("→ Trying LrcLib...")
                                kotlin.runCatching {
                                LrcLib.lyrics(
                                    artist = artistName ?: "",
                                    title = title ?: "",
                                    duration = duration.milliseconds,
                                    album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                                )?.onSuccess {
                                    Timber.tag(TAG).d("→ LrcLib SUCCESS | text=${it?.text?.take(50)} | sentences=${it?.sentences?.size}")
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
                                    Timber.tag(TAG).d("→ LrcLib FAILURE: ${it.message}")
                                    if (playerEnableLyricsPopupMessage)
                                        coroutineScope.launch {
                                            Toaster.e(
                                                R.string.info_lyrics_not_found_on_s_try_on_s,
                                                context.getString(R.string.source_lrclib_synced), context.getString(R.string.source_kugou_synced),
                                                duration = Toast.LENGTH_LONG
                                            )
                                        }

                                    onCheckedLrcUpdated(true)

                                    Timber.tag(TAG).d("→ Trying KuGou...")
                                    kotlin.runCatching {
                                        KuGou.lyrics(
                                            artist = mediaMetadata.artist?.toString() ?: "",
                                            title = cleanPrefix(mediaMetadata.title?.toString() ?: ""),
                                            duration = duration / 1000
                                        )?.onSuccess {
                                            val hasContent = it?.value?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true
                                            Timber.tag(TAG).d("→ KuGou SUCCESS | hasContent=$hasContent | value=${it?.value?.take(50)} | sentences=${it?.sentences?.size}")
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
                                                Timber.tag(TAG).d("→ KuGou empty content → trying LrcLib(U)")
                                                if (playerEnableLyricsPopupMessage)
                                                    coroutineScope.launch {
                                                        Toaster.e(
                                                            R.string.info_lyrics_not_found_on_s_try_on_s,
                                                            context.getString(R.string.source_kugou_synced),
                                                            context.getString(R.string.source_lrclib_unsynced)
                                                        )
                                                    }
                                                onCheckedKugouUpdated(true)
                                                Timber.tag(TAG).d("→ Trying LrcLib (Unsynced)...")
                                                kotlin.runCatching {
                                                    LrcLib.lyricsUnsynced(
                                                        artist = artistName ?: "",
                                                        title = title ?: "",
                                                        duration = duration.milliseconds,
                                                        album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                                                    )?.onSuccess {
                                                        val hasContent = it?.plainText?.isNotEmpty() == true
                                                        Timber.tag(TAG).d("→ LrcLib(U) SUCCESS | hasContent=$hasContent | text=${it?.plainText?.take(50)}")
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
                                                        Timber.tag(TAG).d("→ LrcLib(U) FAILURE: ${it.message}")
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
                                            Timber.tag(TAG).d("→ KuGou FAILURE: ${it.message}")
                                            if (playerEnableLyricsPopupMessage)
                                                coroutineScope.launch {
                                                    Toaster.e(
                                                        R.string.info_lyrics_not_found_on_s_try_on_s,
                                                        context.getString(R.string.source_kugou_synced),
                                                        context.getString(R.string.source_lrclib_unsynced)
                                                    )
                                                }

                                            Timber.tag(TAG).d("→ Trying LrcLib (Unsynced)...")
                                            kotlin.runCatching {
                                                LrcLib.lyricsUnsynced(
                                                    artist = artistName ?: "",
                                                    title = title ?: "",
                                                    duration = duration.milliseconds,
                                                    album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                                                )?.onSuccess {
                                                    val hasContent = it?.plainText?.isNotEmpty() == true
                                                    Timber.tag(TAG).d("→ LrcLib(U) SUCCESS | hasContent=$hasContent | text=${it?.plainText?.take(50)}")
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
                                                    Timber.tag(TAG).d("→ LrcLib(U) FAILURE: ${it.message}")
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
                        } else {
                            Timber.tag(TAG).d("→ BLOCKED: currentSynced is NOT null and needSynced=false")
                        }
                    }

                    if (wantKaraoke) {
                        Timber.tag(TAG).d("→ KARAOKE PATH: calling BetterLyrics...")
                        kotlin.runCatching {
                            com.metrolist.music.betterlyrics.BetterLyrics.getLyrics(
                                title = cleanPrefix(title ?: ""),
                                artist = artistName ?: "",
                                duration = duration.milliseconds.inWholeSeconds.toInt(),
                                album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                            ).onSuccess { ttmlStr ->
                                val hasKaraokeTimings = ttmlStr.lines().any { it.trim().startsWith("<") && it.contains(":") && it.contains(">") }
                                Timber.tag(TAG).d("→ BetterLyrics KARAOKE result | length=${ttmlStr.length} | hasKaraokeTimings=$hasKaraokeTimings | first100=${ttmlStr.take(100)}")
                                if (ttmlStr.isNotEmpty()) {
                                    if (hasKaraokeTimings) {
                                        Timber.tag(TAG).d("→ BetterLyrics has KARAOKE timings → saving directly")
                                        if (playerEnableLyricsPopupMessage) {
                                            coroutineScope.launch {
                                                Toaster.s(
                                                    R.string.info_lyrics_found_on_s,
                                                    context.getString(R.string.source_betterlyrics_karaoke)
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
                                        Timber.tag(TAG).d("→ BetterLyrics has synced ONLY (no karaoke) → fallback")
                                        fetchLrcLibAndKugou(ttmlStr)
                                    }
                                } else {
                                    Timber.tag(TAG).d("→ BetterLyrics EMPTY → fallback")
                                    if (playerEnableLyricsPopupMessage) {
                                        coroutineScope.launch {
                                            Toaster.e(
                                                R.string.info_lyrics_not_found_on_s_try_on_s,
                                                context.getString(R.string.source_betterlyrics_karaoke),
                                                context.getString(R.string.source_betterlyrics_synced)
                                            )
                                        }
                                    }
                                    fetchLrcLibAndKugou(null)
                                }
                            }.onFailure {
                                Timber.tag(TAG).d("→ BetterLyrics KARAOKE FAILURE: ${it.message}")
                                if (playerEnableLyricsPopupMessage) {
                                    coroutineScope.launch {
                                        Toaster.e(
                                            R.string.info_lyrics_not_found_on_s_try_on_s,
                                            context.getString(R.string.source_betterlyrics_karaoke),
                                            context.getString(R.string.source_betterlyrics_synced)
                                        )
                                    }
                                }
                                fetchLrcLibAndKugou(null)
                            }
                        }.onFailure {
                            Timber.tag(TAG).e("→ BetterLyrics KARAOKE ERROR: ${it.stackTraceToString()}")
                            if (playerEnableLyricsPopupMessage) {
                                coroutineScope.launch {
                                    Toaster.e(
                                        R.string.info_lyrics_not_found_on_s_try_on_s,
                                        context.getString(R.string.source_betterlyrics_karaoke),
                                        context.getString(R.string.source_betterlyrics_synced)
                                    )
                                }
                            }
                            fetchLrcLibAndKugou(null)
                        }
                    } else {
                        Timber.tag(TAG).d("→ SYNCED PATH (not karaoke): calling fetchLrcLibAndKugou directly")
                        fetchLrcLibAndKugou(null)
                    }

                } else if (!wantSynced && currentLyrics?.fixed == null && globalLastUnSyncedAttemptMediaId != mediaId) {
                    Timber.tag(TAG).d("→ UNSYNCED PATH: LrcLib(U) → YouTube(U)")
                    globalLastUnSyncedAttemptMediaId = mediaId
                    onErrorUpdated(false)
                    onLyricsUpdated(null)

                    var foundUnsynced = false

                    Timber.tag(TAG).d("→ Trying LrcLib (Unsynced)...")
                    kotlin.runCatching {
                        LrcLib.lyricsUnsynced(
                            artist = artistName ?: "",
                            title = title ?: "",
                            duration = duration.milliseconds,
                            album = mediaMetadata.albumTitle?.toString() ?: Database.albumTable.findBySongId(mediaId).firstOrNull()?.title
                        )?.onSuccess {
                            val hasContent = it?.plainText?.isNotEmpty() == true
                            Timber.tag(TAG).d("→ LrcLib(U) SUCCESS | hasContent=$hasContent | text=${it?.plainText?.take(50)}")
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
                            Timber.tag(TAG).d("→ LrcLib(U) FAILURE: ${it.message}")
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
                        Timber.tag(TAG).d("→ Trying YouTube (Unsynced)...")
                        kotlin.runCatching {
                            Innertube.lyrics(NextBody(videoId = mediaId))
                                ?.onSuccess { fixedLyrics ->
                                    Timber.tag(TAG).d("→ YouTube(U) SUCCESS | lyrics=${fixedLyrics?.take(50)}")
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
                                    Timber.tag(TAG).d("→ YouTube(U) FAILURE: ${it.message}")
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
    Timber.tag(TAG).d("→ Trying YouTube (Unsynced)...")
    coroutineScope.launch {
        kotlin.runCatching {
            Innertube.lyrics(NextBody(videoId = mediaId))
                ?.onSuccess { fixedLyrics ->
                    Timber.tag(TAG).d("→ YouTube(U) SUCCESS | lyrics=${fixedLyrics?.take(50)}")
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
                    Timber.tag(TAG).d("→ YouTube(U) FAILURE: ${it.message}")
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
