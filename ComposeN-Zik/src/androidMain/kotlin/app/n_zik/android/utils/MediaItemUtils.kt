package app.n_zik.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import app.n_zik.android.core.database.Database
import app.it.fast4x.rimusic.models.Info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// ─── Composable fallbacks (for UI) ───

@Composable
fun MediaItem.artistTextWithFallback(): String {
    val artist = mediaMetadata.artist?.toString() ?: ""
    if (artist.isNotBlank() && artist != "null") return artist
    val dbSong by remember(mediaId) {
        Database.songTable.findById(mediaId)
    }.collectAsState(null, Dispatchers.IO)
    val dbText = dbSong?.artistsText
    println("NZIK_DB_TRACE artistTextWithFallback id=$mediaId mediaArtist='$artist' dbArtistsText='$dbText'")
    if (!dbText.isNullOrBlank() && dbText != "null") return dbText
    return artist
}

@Composable
fun MediaItem.artistIdsWithFallback(): List<Info> {
    val ids = mediaMetadata.extras?.getStringArrayList("artistIds")
    if (!ids.isNullOrEmpty()) {
        val names = mediaMetadata.extras?.getStringArrayList("artistNames")
        return ids.mapIndexed { i, id -> Info(id, names?.getOrNull(i)) }
    }
    val dbArtists by remember(mediaId) {
        Database.artistTable.findBySongId(mediaId)
    }.collectAsState(emptyList(), Dispatchers.IO)
    return dbArtists.map { Info(it.id, it.name) }
}

@Composable
fun MediaItem.albumIdWithFallback(): String? {
    val albumId = mediaMetadata.extras?.getString("albumId")
    if (!albumId.isNullOrBlank()) return albumId
    val dbAlbum by remember(mediaId) {
        Database.albumTable.findBySongId(mediaId)
    }.collectAsState(null, Dispatchers.IO)
    return dbAlbum?.id
}

// ─── Non-Composable fallbacks (for services, notifications, etc.) ───

fun MediaItem.artistTextOrDb(): String {
    val artist = mediaMetadata.artist?.toString() ?: ""
    if (artist.isNotBlank() && artist != "null") return artist
    val dbText = runBlocking {
        Database.songTable.findById(mediaId).first()?.artistsText
    }
    println("NZIK_DB_TRACE artistTextOrDb id=$mediaId mediaArtist='$artist' dbArtistsText='$dbText'")
    if (!dbText.isNullOrBlank() && dbText != "null") return dbText
    return artist
}

fun MediaItem.albumTitleOrDb(): String {
    val album = mediaMetadata.albumTitle?.toString() ?: ""
    if (album.isNotBlank() && album != "null") return album
    return runBlocking {
        Database.albumTable.findBySongId(mediaId).first()?.title ?: album
    }
}
