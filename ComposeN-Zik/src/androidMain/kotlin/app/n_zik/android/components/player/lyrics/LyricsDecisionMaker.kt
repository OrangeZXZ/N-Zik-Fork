package app.n_zik.android.components.player.lyrics

import app.n_zik.android.enums.lyrics.LyricsType
import app.n_zik.android.models.Lyrics

/**
 * Encapsulates the logic for determining which lyrics to display and whether a network fetch is needed.
 */
data class FetchNeeds(
    val currentLyrics: Lyrics?,
    val needKaraokeFetch: Boolean,
    val needSyncedFetch: Boolean,
    val needUnsyncedFetch: Boolean
)

object LyricsDecisionMaker {

    fun evaluateFetchNeeds(
        mediaId: String,
        lyricsType: LyricsType,
        allLyrics: List<Lyrics>,
        globalLastKaraokeAttemptMediaId: String?,
        globalLastSyncedAttemptMediaId: String?,
        globalLastUnSyncedAttemptMediaId: String?
    ): FetchNeeds {
        val wantSynced = lyricsType != LyricsType.Unsynced
        val wantKaraoke = lyricsType == LyricsType.Karaoke

        val currentLyrics = if (wantKaraoke) {
            allLyrics.find { it.type == LyricsType.Karaoke.name } ?: allLyrics.find { it.type == LyricsType.Synced.name }
        } else if (wantSynced) {
            allLyrics.find { it.type == LyricsType.Synced.name }
        } else {
            allLyrics.find { it.type == LyricsType.Unsynced.name }
        }

        val hasWordTimings = currentLyrics?.data?.lines()?.any { it.trim().startsWith("<") && it.contains(":") && it.contains(">") } == true

        val needKaraokeFetch = wantKaraoke && (!hasWordTimings && globalLastKaraokeAttemptMediaId != mediaId)
        val needSyncedFetch = lyricsType == LyricsType.Synced && currentLyrics?.data.isNullOrEmpty() && globalLastSyncedAttemptMediaId != mediaId
        val needUnsyncedFetch = lyricsType == LyricsType.Unsynced && currentLyrics?.data.isNullOrEmpty() && globalLastUnSyncedAttemptMediaId != mediaId

        return FetchNeeds(
            currentLyrics = currentLyrics,
            needKaraokeFetch = needKaraokeFetch,
            needSyncedFetch = needSyncedFetch,
            needUnsyncedFetch = needUnsyncedFetch
        )
    }
}
