package app.n_zik.android.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.n_zik.android.core.database.DatabaseInitializer
import app.n_zik.android.core.database.LyricsTable
import app.n_zik.android.enums.lyrics.LyricsType
import app.n_zik.android.models.Lyrics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LyricsTableTest {

    private lateinit var db: DatabaseInitializer
    private lateinit var lyricsDao: LyricsTable

    @BeforeEach
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DatabaseInitializer::class.java)
            .allowMainThreadQueries()
            .build()
        lyricsDao = db.lyricsTable
    }

    @AfterEach
    fun closeDb() {
        db.close()
    }

    @Test
    fun `upsert and retrieve 3 distinct modes for the same song`() = runBlocking {
        val mediaId = "song_123"

        lyricsDao.upsert(Lyrics(songId = mediaId, type = LyricsType.Karaoke.name, data = "karaoke data"))
        lyricsDao.upsert(Lyrics(songId = mediaId, type = LyricsType.Synced.name, data = "sync data"))
        lyricsDao.upsert(Lyrics(songId = mediaId, type = LyricsType.Unsynced.name, data = "unsync data"))

        val allLyrics = lyricsDao.findAllBySongId(mediaId).first()
        assertEquals(3, allLyrics.size)

        val karaoke = allLyrics.find { it.type == LyricsType.Karaoke.name }
        assertEquals("karaoke data", karaoke?.data)

        val sync = allLyrics.find { it.type == LyricsType.Synced.name }
        assertEquals("sync data", sync?.data)

        val unsync = allLyrics.find { it.type == LyricsType.Unsynced.name }
        assertEquals("unsync data", unsync?.data)
    }

    @Test
    fun `upsert overwrites existing data for the same songId and type`() = runBlocking {
        val mediaId = "song_123"

        lyricsDao.upsert(Lyrics(songId = mediaId, type = LyricsType.Karaoke.name, data = "initial data"))

        var lyrics = lyricsDao.findBySongIdAndType(mediaId, LyricsType.Karaoke.name).first()
        assertEquals("initial data", lyrics?.data)

        lyricsDao.upsert(Lyrics(songId = mediaId, type = LyricsType.Karaoke.name, data = "updated data"))

        lyrics = lyricsDao.findBySongIdAndType(mediaId, LyricsType.Karaoke.name).first()
        assertEquals("updated data", lyrics?.data)

        val allLyrics = lyricsDao.findAllBySongId(mediaId).first()
        assertEquals(1, allLyrics.size)
    }

    @Test
    fun `findAllBySongId does not return lyrics for other songs`() = runBlocking {
        lyricsDao.upsert(Lyrics(songId = "song_A", type = LyricsType.Karaoke.name, data = "A"))
        lyricsDao.upsert(Lyrics(songId = "song_B", type = LyricsType.Karaoke.name, data = "B"))

        val resultsForA = lyricsDao.findAllBySongId("song_A").first()
        assertEquals(1, resultsForA.size)
        assertEquals("A", resultsForA.first().data)
    }
}
