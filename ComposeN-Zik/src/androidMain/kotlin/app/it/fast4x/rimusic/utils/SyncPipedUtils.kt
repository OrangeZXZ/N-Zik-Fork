package app.it.fast4x.rimusic.utils

import app.n_zik.android.core.database.*

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.n_zik.android.R
import it.fast4x.piped.Piped
import it.fast4x.piped.models.Session
import app.n_zik.android.core.database.Database
import app.it.fast4x.rimusic.PIPED_PREFIX
import app.it.fast4x.rimusic.models.Playlist
import app.it.fast4x.rimusic.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import java.util.UUID

fun syncSongsInPipedPlaylist(context: Context,coroutineScope: CoroutineScope, pipedSession: Session, idPipedPlaylist: UUID, playlistId: Long) {

   if (!checkPipedAccount(context, pipedSession)) return

    coroutineScope.launch(Dispatchers.IO) {
        async {
            Piped.playlist.songs(
                session = pipedSession,
                id = idPipedPlaylist
            )
        }.await()?.map {playlist ->

            Timber.tag("SyncPipedUtils").d("pipedInfo syncSongsInPipedPlaylist playlistId $playlistId songs ${playlist.videos.size}")
            Timber.tag("SyncPipedUtils").d("syncSongsInPipedPlaylist playlistId $playlistId songs ${playlist.videos.size}")

            Database.playlistTable
                    .findById( playlistId )
                    .first()
                    ?.let { dbPlaylist ->
                        Database.songPlaylistMapTable.clear( playlistId )

                        playlist.videos
                                .mapNotNull video2Song@ {
                                    if( it.id == null ) return@video2Song null
                                    Song(
                                        id = it.id!!,
                                        title = it.cleanTitle,
                                        artistsText = it.cleanArtists,
                                        durationText = it.durationText,
                                        thumbnailUrl = it.thumbnailUrl.toString()
                                    )
                                }
                                .let {
                                    Database.mapIgnore( dbPlaylist, *it.toTypedArray() )
                                }
                    }
        }
    }
}


@Composable
fun ImportPipedPlaylists(){
    val isPipedEnabled by rememberPreference(isPipedEnabledKey, false)
    if (!isPipedEnabled) return

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val pipedSession = getPipedSession()
    if (pipedSession.token == "" || pipedSession.token.isEmpty()) {
        Toaster.w( R.string.info_connect_your_piped_account_first )
        return
    }

    LaunchedEffect(Unit) {
            async {
                Piped.playlist.list(session = pipedSession.toApiSession())
            }.await()?.map {
                Timber.tag("SyncPipedUtils").d("ImportPipedPlaylists playlists ${it.size}")
                //itemsPiped = it
                Database.asyncTransaction {
                    it.forEach {
                        val playlistExist = runBlocking {
                            playlistTable.exists( "$PIPED_PREFIX${it.name}" ).first()
                        }
                        if ( !playlistExist ) {
                            coroutineScope.launch(Dispatchers.IO) {
                                async {
                                    Piped.playlist.songs(
                                        session = pipedSession.toApiSession(),
                                        id = it.id
                                    )
                                }.await()?.map {playlist ->

                                    val innerPlaylist = Playlist(
                                        name = "$PIPED_PREFIX${it.name}",
                                        browseId = it.id.toString()
                                    )
                                    playlist.videos
                                            .mapNotNull video2Song@ { video ->
                                                if( video.id == null ) return@video2Song null

                                                Song(
                                                    id = video.id!!,
                                                    title = video.cleanTitle,
                                                    artistsText = video.cleanArtists,
                                                    durationText = video.durationText,
                                                    thumbnailUrl = video.thumbnailUrl.toString()
                                                )
                                            }
                                            .let {
                                                mapIgnore( innerPlaylist, *it.toTypedArray() )
                                            }
                                }
                            }
                        }
                    }
                }
            }
        }

}

fun addToPipedPlaylist(context: Context, coroutineScope: CoroutineScope, pipedSession: Session, id: UUID, videos: List<String>) {
    if (!checkPipedAccount(context, pipedSession)) return
    coroutineScope.launch(Dispatchers.IO) {
            Piped.playlist.add(session = pipedSession, id = id, videos = videos.map { it.toID() })
            Timber.tag("SyncPipedUtils").d("addToPipedPlaylist pipedSession $pipedSession, id $id, videos ${videos.size}")
    }

}

fun removeFromPipedPlaylist(context: Context, coroutineScope: CoroutineScope, pipedSession: Session, id: UUID, idx: Int) {
    if (!checkPipedAccount(context, pipedSession)) return
    coroutineScope.launch(Dispatchers.IO) {
        Piped.playlist.remove(session = pipedSession, id = id, idx = idx)
        Timber.tag("SyncPipedUtils").d("removeFromPipedPlaylist pipedSession $pipedSession, id $id, idx $idx")
    }

}

fun deletePipedPlaylist(context: Context, coroutineScope: CoroutineScope, pipedSession: Session, id: UUID) {
    if (!checkPipedAccount(context, pipedSession)) return
    coroutineScope.launch(Dispatchers.IO) {
        Piped.playlist.delete(session = pipedSession, id = id)
        Timber.tag("SyncPipedUtils").d("deletePipedPlaylist pipedSession $pipedSession, id $id")
    }

}

fun renamePipedPlaylist(context: Context, coroutineScope: CoroutineScope, pipedSession: Session, id: UUID, name: String) {
    if (!checkPipedAccount(context, pipedSession)) return
    coroutineScope.launch(Dispatchers.IO) {
        Piped.playlist.rename(session = pipedSession, id = id, name = name)
        Timber.tag("SyncPipedUtils").d("renamePipedPlaylist pipedSession $pipedSession, id $id, name $name")
    }

}

fun createPipedPlaylist(context: Context, coroutineScope: CoroutineScope, pipedSession: Session, name: String): Long {
    var playlistId: Long = -1
    var browseId: String = ""
    if (!checkPipedAccount(context, pipedSession)) return playlistId

    coroutineScope.launch(Dispatchers.IO) {
        async {
            Piped.playlist.create(session = pipedSession, name = name)
        }.await()?.map {
            val playlist = Playlist(name = "$PIPED_PREFIX$name", browseId = it.id.toString())
            playlistId = Database.playlistTable.insert( playlist)
            browseId = it.id.toString()
        }
        Timber.tag("SyncPipedUtils").d("createPipedPlaylist pipedSession $pipedSession, name $name new playlistId $playlistId browseId $browseId")
    }

    return playlistId
}

fun String.toID(): String {
    return this
        .replace("/watch?v=", "") // videos
        .replace("/channel/", "") // channels
        .replace("/playlist?list=", "") // playlists
}

fun checkPipedAccount(context: Context, pipedSession: Session): Boolean {
    val isPipedEnabled = context.preferences.getBoolean(isPipedEnabledKey, false)
    //Timber.d("mediaItem SyncPipedUtils checkPipedAccount isPipedEnabled $isPipedEnabled token ${pipedSession.token}")
    if (isPipedEnabled && pipedSession.token.isEmpty()) {
        Toaster.w( R.string.info_connect_your_piped_account_first )
        Timber.tag("SyncPipedUtils").d("checkPipedAccount Piped account not connected")
        return false
    }
    Timber.tag("SyncPipedUtils").d("checkPipedAccount Piped account connected")
    return true
}


