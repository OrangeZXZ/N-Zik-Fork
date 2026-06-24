package app.n_zik.android.utils

import android.os.Build
import androidx.annotation.RequiresApi
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.models.Album
import app.it.fast4x.rimusic.models.Playlist
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.models.SongArtistMap
import app.it.fast4x.rimusic.models.SongPlaylistMap
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.durationTextToMillis
import app.n_zik.android.core.database.Database
import app.kreate.android.me.knighthat.utils.PropUtils
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Global version of match: replaces a local/unmatched song across
 * ALL playlists, events, and relation maps in the database.
 * Use this from HomeSongsScreen to match without needing a specific playlist.
 */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun getAlbumVersionFromVideoGlobal(song: Song) {
    val random4Digit = Random.nextInt(1000, 10000)

    fun filteredText(text: String): String = text
        .lowercase()
        .replace("(", " ").replace(")", " ").replace("-", " ")
        .replace("lyrics", "").replace("vevo", "").replace(" hd", "")
        .replace("official video", "")
        .filter { it.isLetterOrDigit() || it.isWhitespace() || it == '\'' || it == ',' }
        .replace(Regex("\\s+"), " ")

    fun shuffle(word: String): String {
        val chars = word.toCharArray()
        for (i in chars.indices) {
            val randomIndex = Random.nextInt(chars.size)
            chars[i] = chars[randomIndex]
        }
        return String(chars)
    }

    // Random delay to avoid bot detection (500ms - 2500ms)
    delay(Random.nextLong(1000, 5000))

    val searchQuery = Innertube.searchPage<Innertube.SongItem>(
        body = SearchBody(
            query = filteredText("${song.cleanTitle()} ${song.artistsText}"),
            params = Innertube.SearchFilter.Song.value
        ),
        fromMusicShelfRendererContent = { content -> Innertube.SongItem.from(content) }
    )

    var searchResults = searchQuery?.getOrNull()?.items
    val sourceSongWords = filteredText(song.cleanTitle()).split(" ").filter { it.isNotEmpty() }

    // Fallback: if 0 results, try simpler query (just title)
    if (searchResults.isNullOrEmpty()) {
        val simpleQuery = filteredText(song.cleanTitle())
        delay(Random.nextLong(1500, 4000))
        val fallbackQuery = Innertube.searchPage<Innertube.SongItem>(
            body = SearchBody(
                query = simpleQuery,
                params = Innertube.SearchFilter.Song.value
            ),
            fromMusicShelfRendererContent = { content -> Innertube.SongItem.from(content) }
        )
        searchResults = fallbackQuery?.getOrNull()?.items
    }

    fun scoreCandidate(candidate: Innertube.SongItem): Int {
        val candidateTitle = filteredText(candidate.info?.name ?: "")
        val candidateArtist = filteredText(candidate.authors?.firstOrNull()?.name ?: "")
        val sourceArtist = filteredText(song.artistsText ?: "")
        var score = 0
        if (candidateTitle.contains(filteredText(song.cleanTitle()))) score += 10
        if (candidateArtist.contains(sourceArtist) || sourceArtist.contains(candidateArtist)) score += 5
        val durationMs = song.durationText?.let { durationTextToMillis(it) } ?: 0L
        val candidateDuration = candidate.durationText?.let { durationTextToMillis(it) } ?: 0L
        if (durationMs > 0 && kotlin.math.abs(durationMs - candidateDuration) < 5000) score += 5
        return score
    }

    val bestMatch = searchResults
        ?.filter { scoreCandidate(it) >= 10 }
        ?.maxByOrNull { scoreCandidate(it) }

    // Calculate the song's effective position in custom sort order BEFORE any DB changes.
    // This is needed because when position == -1, the sort uses ROWID, and after
    // delete + re-insert the ROWID changes, causing the song to jump to the end.
    val effectivePosition = Database.songTable.sortAllByPosition().first()
        .indexOfFirst { it.id == song.id }
        .takeIf { it >= 0 } ?: song.position

    Database.asyncTransaction {
        if (bestMatch != null) {
            val newSong = bestMatch.asSong

            // Check if a song with this YouTube ID already exists (duplicate match)
            val existingSong = runBlocking(Dispatchers.IO) { songTable.findById(newSong.id).first() }
            if (existingSong != null && existingSong.id != song.id) {
                // DO NOT delete — just skip this match to keep both songs
                return@asyncTransaction
            }

            // Save playlist mappings before delete (CASCADE will remove them)
            val playlistMappings = songPlaylistMapTable.getAllForSong(song.id)
            // Delete old song first — CASCADE removes SongPlaylistMap, SongAlbumMap, etc.
            songTable.delete(song)
            // Upsert merged song with new ID
            songTable.upsert(newSong.copy(
                title = PropUtils.retainIfModified(song.title, newSong.title).orEmpty(),
                artistsText = PropUtils.retainIfModified(song.artistsText, newSong.artistsText),
                thumbnailUrl = PropUtils.retainIfModified(song.thumbnailUrl, newSong.thumbnailUrl),
                likedAt = song.likedAt,
                totalPlayTimeMs = song.totalPlayTimeMs,
                position = effectivePosition
            ))
            // Re-insert playlist mappings with new song ID
            playlistMappings.forEach { mapping ->
                songPlaylistMapTable.mapAtPosition(newSong.id, mapping.playlistId, mapping.position)
            }
            // Update other references
            songAlbumMapTable.updateSongId(song.id, newSong.id)
            songArtistMapTable.updateSongId(song.id, newSong.id)
            eventTable.updateSongId(song.id, newSong.id)
            // Create album mapping from matched result
            bestMatch.album?.let { albumInfo ->
                val albumId = albumInfo.endpoint?.browseId ?: return@let
                albumTable.insertIgnore(app.it.fast4x.rimusic.models.Album(id = albumId, title = albumInfo.name))
                songAlbumMapTable.map(newSong.id, albumId)
            }
            bestMatch.authors?.forEach { author ->
                val browseId = author.endpoint?.browseId ?: return@forEach
                artistTable.insertIgnore(app.it.fast4x.rimusic.models.Artist(
                    id = browseId,
                    name = author.name,
                    thumbnailUrl = null
                ))
                songArtistMapTable.insertIgnore(SongArtistMap(newSong.id, browseId))
            }
        } else {
            // Mark as "not found" by giving it a shuffle ID so it won't be retried
            if (song.id == (song.cleanTitle() + song.artistsText).filter { it.isLetterOrDigit() }) {
                val notFound = song.copy(
                    id = shuffle(song.artistsText + random4Digit + song.cleanTitle() + "56Music").filter { it.isLetterOrDigit() },
                    position = effectivePosition
                )
                val oldId = song.id
                songTable.insertIgnore(notFound)
                songPlaylistMapTable.updateSongId(oldId, notFound.id)
                songAlbumMapTable.updateSongId(oldId, notFound.id)
                songArtistMapTable.updateSongId(oldId, notFound.id)
                eventTable.updateSongId(oldId, notFound.id)
                songTable.delete(song)
            }
            // else: Song has a Spotify URI (or other non-pseudo ID) and no match found - keeping as-is
        }
    }
}

suspend fun getAlbumVersionFromVideo(song: Song, playlistId: Long, position: Int, playlist: Playlist?) {
    val isExtPlaylist = (song.thumbnailUrl.isNullOrEmpty()) && (song.durationText != "0:00")
    var songNotFound: Song
    val random4Digit = Random.nextInt(1000, 10000)

    fun filteredText(text: String): String {
        val filteredText = text
            .lowercase()
            .replace("(", " ")
            .replace(")", " ")
            .replace("-", " ")
            .replace("lyrics", "")
            .replace("vevo", "")
            .replace(" hd", "")
            .replace("official video", "")
            .filter { it.isLetterOrDigit() || it.isWhitespace() || it == '\'' || it == ',' }
            .replace(Regex("\\s+"), " ")
        return filteredText
    }

    // Random delay to avoid bot detection (500ms - 2500ms)
    delay(Random.nextLong(1000, 5000))

    val searchQuery = Innertube.searchPage<Innertube.SongItem>(
        body = SearchBody(
            query = filteredText("${song.cleanTitle()} ${song.artistsText}"),
            params = Innertube.SearchFilter.Song.value
        ),
        fromMusicShelfRendererContent = { content -> Innertube.SongItem.from(content) }
    )

    var searchResults = searchQuery?.getOrNull()?.items

    // Fallback: if 0 results, try simpler query (just title)
    if (searchResults.isNullOrEmpty()) {
        val simpleQuery = filteredText(song.cleanTitle())
        delay(Random.nextLong(1500, 4000))
        val fallbackQuery = Innertube.searchPage<Innertube.SongItem>(
            body = SearchBody(
                query = simpleQuery,
                params = Innertube.SearchFilter.Song.value
            ),
            fromMusicShelfRendererContent = { content -> Innertube.SongItem.from(content) }
        )
        searchResults = fallbackQuery?.getOrNull()?.items
    }

    val sourceSongWords = filteredText(song.cleanTitle())
        .split(" ").filter { it.isNotEmpty() }
    val lofi = sourceSongWords.contains("lofi")
    val rock = sourceSongWords.contains("rock")
    val reprise = sourceSongWords.contains("reprise")
    val unplugged = sourceSongWords.contains("unplugged")
    val instrumental = sourceSongWords.contains("instrumental")
    val remix = sourceSongWords.contains("remix")
    val acapella = sourceSongWords.contains("acapella")
    val acoustic = sourceSongWords.contains("acoustic")
    val live = sourceSongWords.contains("live")
    val concert = sourceSongWords.contains("concert")
    val tour = sourceSongWords.contains("tour")
    val redux = sourceSongWords.contains("redux")

    fun shuffle(word: String): String {
        val chars = word.toCharArray()
        for (i in chars.indices) {
            val randomIndex = Random.nextInt(chars.size)
            chars[i] = chars[randomIndex]
        }
        return String(chars)
    }

    fun findSongIndex(): Int {
        for (i in 0..4) {
            val requiredSong = searchResults?.getOrNull(i) as? Innertube.SongItem ?: continue
            val requiredSongWords = filteredText(song.cleanTitle()) // Actually in RiPlay it was filteredText(cleanPrefix(requiredSong?.title ?: ""))
            // Wait, I should implement exactly like RiPlay
            val reqWords = filteredText(requiredSong.title ?: "").split(" ").filter { it.isNotEmpty() }

            val songMatched = (reqWords.any { it in sourceSongWords })
                    && (if (lofi) (reqWords.any { it == "lofi" }) else reqWords.all { it != "lofi" })
                    && (if (rock) (reqWords.any { it == "rock" }) else reqWords.all { it != "rock" })
                    && (if (reprise) (reqWords.any { it == "reprise" }) else reqWords.all { it != "reprise" })
                    && (if (unplugged) (reqWords.any { it == "unplugged" }) else reqWords.all { it != "unplugged" })
                    && (if (instrumental) (reqWords.any { it == "instrumental" }) else reqWords.all { it != "instrumental" })
                    && (if (remix) (reqWords.any { it == "remix" }) else reqWords.all { it != "remix" })
                    && (if (acapella) (reqWords.any { it == "acapella" }) else reqWords.all { it != "acapella" })
                    && (if (acoustic) (reqWords.any { it == "acoustic" }) else reqWords.all { it != "acoustic" })
                    && (if (live) (reqWords.any { it == "live" }) else reqWords.all { it != "live" })
                    && (if (concert) (reqWords.any { it == "concert" }) else reqWords.all { it != "concert" })
                    && (if (tour) (reqWords.any { it == "tour" }) else reqWords.all { it != "tour" })
                    && (if (redux) (reqWords.any { it == "redux" }) else reqWords.all { it != "redux" })
                    && (if (isExtPlaylist) {
                (durationTextToMillis(requiredSong.durationText ?: "") - durationTextToMillis(song.durationText ?: "")).absoluteValue <= 7000
            } else {
                true
            })

            if (songMatched) return i
        }
        return -1
    }

    val matchedSongIndex = findSongIndex()
    val matchedSong = if (matchedSongIndex != -1) searchResults?.getOrNull(matchedSongIndex) as? Innertube.SongItem else null

    // Calculate the song's effective position in custom sort order BEFORE any DB changes.
    val effectivePosition = Database.songTable.sortAllByPosition().first()
        .indexOfFirst { it.id == song.id }
        .takeIf { it >= 0 } ?: song.position

    Database.asyncTransaction {
        val oldPosition = songPlaylistMapTable.findPositionOf(song.id, playlistId)

        if (matchedSongIndex != -1 && matchedSong != null) {
            val newSong = matchedSong.asSong

            // Check if a song with this YouTube ID already exists (duplicate match)
            val existingSong = runBlocking(Dispatchers.IO) { songTable.findById(newSong.id).first() }
            if (existingSong != null && existingSong.id != song.id) {
                // DO NOT delete — just skip this match to keep both songs
                return@asyncTransaction
            }

            // Save playlist mappings before delete (CASCADE will remove them)
            val playlistMappings = songPlaylistMapTable.getAllForSong(song.id)

            // Delete old song first — CASCADE removes SongPlaylistMap, etc.
            songTable.delete(song)

            // Upsert merged song with new ID
            songTable.upsert(newSong.copy(
                title = PropUtils.retainIfModified(song.title, newSong.title).orEmpty(),
                artistsText = PropUtils.retainIfModified(song.artistsText, newSong.artistsText),
                thumbnailUrl = PropUtils.retainIfModified(song.thumbnailUrl, newSong.thumbnailUrl),
                likedAt = song.likedAt,
                totalPlayTimeMs = song.totalPlayTimeMs,
                position = effectivePosition
            ))

            // Re-insert playlist mappings with new song ID
            playlistMappings.forEach { mapping ->
                songPlaylistMapTable.mapAtPosition(newSong.id, mapping.playlistId, mapping.position)
            }

            // Update other references
            songAlbumMapTable.updateSongId(song.id, newSong.id)
            songArtistMapTable.updateSongId(song.id, newSong.id)
            eventTable.updateSongId(song.id, newSong.id)

            // Create album mapping from matched result
            matchedSong.album?.let { albumInfo ->
                val albumId = albumInfo.endpoint?.browseId ?: return@let
                albumTable.insertIgnore(app.it.fast4x.rimusic.models.Album(id = albumId, title = albumInfo.name))
                songAlbumMapTable.map(newSong.id, albumId)
            }

            // Create artist mappings from matched result
            matchedSong.authors?.forEach { author ->
                val browseId = author.endpoint?.browseId ?: return@forEach
                artistTable.insertIgnore(app.it.fast4x.rimusic.models.Artist(
                    id = browseId,
                    name = author.name,
                    thumbnailUrl = null
                ))
                songArtistMapTable.insertIgnore(SongArtistMap(newSong.id, browseId))
            }

            // Restore position in THIS playlist
            if (oldPosition != -1) {
                songPlaylistMapTable.updatePosition(playlistId, newSong.id, oldPosition)
            }
        } else if (song.id == (song.cleanTitle() + song.artistsText).filter { it.isLetterOrDigit() }) {
            songNotFound = song.copy(id = shuffle(song.artistsText + random4Digit + song.cleanTitle() + "56Music").filter { it.isLetterOrDigit() })
            songTable.insertIgnore(songNotFound)
            songPlaylistMapTable.map(songNotFound.id, playlistId)
            if (oldPosition != -1) {
                songPlaylistMapTable.updatePosition(playlistId, songNotFound.id, oldPosition)
            }
        }
        // else: Song has a Spotify URI (or other non-pseudo ID) and no match found - keeping as-is
    }
}
