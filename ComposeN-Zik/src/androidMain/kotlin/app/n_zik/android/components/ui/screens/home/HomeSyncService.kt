package app.n_zik.android.components.ui.screens.home

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import timber.log.Timber
import app.n_zik.android.R
import app.n_zik.android.appContext
import app.n_zik.android.core.database.Database
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.innertube.utils.from
import app.kreate.android.me.knighthat.utils.PropUtils
import app.it.fast4x.rimusic.models.Artist
import app.it.fast4x.rimusic.models.Album
import app.it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.innertube.models.bodies.BrowseBody
import app.it.fast4x.rimusic.utils.asSong
import kotlinx.coroutines.flow.first

class HomeSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_SYNC_ARTISTS = "app.n_zik.android.action.SYNC_ARTISTS"
        const val ACTION_SYNC_ALBUMS = "app.n_zik.android.action.SYNC_ALBUMS"
        const val ACTION_SYNC_PLAYLISTS = "app.n_zik.android.action.SYNC_PLAYLISTS"
        const val EXTRA_IDS = "EXTRA_IDS"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        
        val notificationId = when (action) {
            ACTION_SYNC_ARTISTS -> 1001
            ACTION_SYNC_ALBUMS -> 1002
            ACTION_SYNC_PLAYLISTS -> 1003
            else -> 1001
        }
        
        HomeSyncState.showSyncNotification(
            title = appContext().getString(R.string.sync_notifications),
            message = "Starting sync...",
            notificationId = notificationId,
            isOngoing = true
        )
        
        val notificationManager = appContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val builder = androidx.core.app.NotificationCompat.Builder(appContext(), "sync_channel_id")
            .setSmallIcon(R.drawable.sync)
            .setContentTitle(appContext().getString(R.string.sync_notifications))
            .setContentText("Syncing in background...")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        
        try {
            ServiceCompat.startForeground(
                this,
                notificationId,
                builder.build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to start foreground service")
        }

        val ids = intent.getStringArrayListExtra(EXTRA_IDS)
        
        serviceScope.launch {
            try {
                when (action) {
                    ACTION_SYNC_ARTISTS -> syncArtists(ids, notificationId)
                    ACTION_SYNC_ALBUMS -> syncAlbums(ids, notificationId)
                    ACTION_SYNC_PLAYLISTS -> syncPlaylists(ids, notificationId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Sync failed with exception")
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    private suspend fun syncArtists(ids: List<String>?, notificationId: Int) {
        if (HomeSyncState.isSyncingArtists) return
        HomeSyncState.isSyncingArtists = true
        HomeSyncState.artistSyncProgress = 0f

        val allArtists = (Database.artistTable.allFollowing().first() + Database.artistTable.allInLibrary().first()).distinctBy { it.id }
        val targetItems = if (ids.isNullOrEmpty()) allArtists else allArtists.filter { it.id in ids }

        val ytArtists = targetItems.filter { it.isYoutubeArtist || it.id.startsWith("UC") }
        val localArtists = targetItems.filterNot { it.isYoutubeArtist || it.id.startsWith("UC") }
        val totalArtists = ytArtists.size + localArtists.size

        Timber.tag("HomeSyncService").d("=== REFRESH START === Total: $totalArtists (YT: ${ytArtists.size}, Local: ${localArtists.size})")

        withContext(Dispatchers.Main) {
            if (totalArtists > 0) app.kreate.android.me.knighthat.utils.Toaster.i(appContext().getString(R.string.refreshing_artists, totalArtists))
            if (ids == null && totalArtists == 0) app.kreate.android.me.knighthat.utils.Toaster.w(appContext().getString(R.string.sync_no_items))
        }
        
        if (totalArtists == 0) {
            HomeSyncState.isSyncingArtists = false
            return
        }

        var failedCount = 0
        val failedList = mutableListOf<Artist>()
        HomeSyncState.artistSyncFailed = 0
        HomeSyncState.artistSyncTotal = totalArtists

        var abortSync = false
        for ((index, artist) in ytArtists.withIndex()) {
            if (abortSync) break
            HomeSyncState.artistSyncCurrentIndex = index + 1
            HomeSyncState.artistSyncCurrentName = artist.name ?: ""
            HomeSyncState.artistSyncProgress = index.toFloat() / ytArtists.size
            HomeSyncState.showSyncNotification(
                title = appContext().getString(R.string.sync_notifications),
                message = appContext().getString(R.string.sync_progress_artists, index + 1, totalArtists),
                notificationId = notificationId,
                isOngoing = true,
                maxProgress = totalArtists,
                currentProgress = index + 1
            )
            kotlinx.coroutines.delay((2000L..5000L).random())
            Timber.tag("HomeSyncService").d("[YT] Fetching by ID: ${artist.id} for '${artist.name}'")
            var status = 0 // 0=retry, 1=success
            for (attempt in 1..3) {
                YtMusic.getArtistPage(artist.id).onSuccess { online ->
                    val onlineArtist = online.artist
                    Timber.tag("HomeSyncService").d("[YT] Got response for '${artist.name}': onlineName='${onlineArtist.title}', thumbnail='${onlineArtist.thumbnail?.url}'")
                    Database.asyncTransaction {
                        Database.artistTable.upsert(Artist(
                            id = artist.id,
                            name = PropUtils.retainIfModified(artist.name, onlineArtist.title),
                            thumbnailUrl = onlineArtist.thumbnail?.url ?: artist.thumbnailUrl,
                            timestamp = artist.timestamp,
                            bookmarkedAt = artist.bookmarkedAt,
                            isYoutubeArtist = artist.isYoutubeArtist,
                            position = artist.position
                        ))
                    }
                    Timber.tag("HomeSyncService").d("Successfully refreshed artist: ${artist.name}")
                    status = 1
                }.onFailure {
                    Timber.tag("HomeSyncService").e(it, "Failed to refresh artist (attempt $attempt): ${artist.name}")
                    if (it is java.net.UnknownHostException || it is java.net.ConnectException) {
                        status = 3
                    }
                }
                if (status != 0) break
            }
            if (status == 3) {
                abortSync = true
                break
            }
            if (status != 1) {
                failedCount++
                HomeSyncState.artistSyncFailed = failedCount
                failedList.add(artist)
            }
        }

        for ((index, artist) in localArtists.withIndex()) {
            if (abortSync) break
            HomeSyncState.artistSyncCurrentIndex = ytArtists.size + index + 1
            HomeSyncState.artistSyncCurrentName = artist.name ?: ""
            HomeSyncState.artistSyncProgress = (ytArtists.size + index).toFloat() / totalArtists
            HomeSyncState.showSyncNotification(
                title = appContext().getString(R.string.sync_notifications),
                message = appContext().getString(R.string.sync_progress_artists, ytArtists.size + index + 1, totalArtists),
                notificationId = notificationId,
                isOngoing = true,
                maxProgress = totalArtists,
                currentProgress = ytArtists.size + index + 1
            )
            val query = artist.name?.trim()
            if (!query.isNullOrBlank()) {
                kotlinx.coroutines.delay((2000L..5000L).random())
                Timber.tag("HomeSyncService").d("[LOCAL] Searching YouTube: query='$query' for artist id=${artist.id}")
                var status = 0 // 0=retry, 1=success, 2=not found
                for (attempt in 1..3) {
                    try {
                        val searchResult = Innertube.searchPage<Innertube.ArtistItem>(
                            body = SearchBody(
                                query = query,
                                params = Innertube.SearchFilter.Artist.value
                            ),
                            fromMusicShelfRendererContent = { content -> Innertube.ArtistItem.from(content) }
                        )?.getOrNull()

                        val remoteId = searchResult?.items?.firstOrNull()?.info?.endpoint?.browseId
                        if (remoteId != null) {
                            Timber.tag("HomeSyncService").d("[LOCAL] Found matching ID on YouTube: $remoteId for '${artist.name}'")
                            YtMusic.getArtistPage(remoteId).onSuccess { online ->
                                val onlineArtist = online.artist
                                Database.asyncTransaction {
                                    Database.artistTable.upsert(Artist(
                                        id = artist.id,
                                        name = artist.name,
                                        thumbnailUrl = onlineArtist.thumbnail?.url ?: artist.thumbnailUrl,
                                        timestamp = artist.timestamp,
                                        bookmarkedAt = artist.bookmarkedAt,
                                        isYoutubeArtist = artist.isYoutubeArtist,
                                        position = artist.position
                                    ))
                                }
                            }
                            status = 1
                        } else {
                            Timber.tag("HomeSyncService").d("No matching artist found on YouTube for '${artist.name}'")
                            status = 2
                        }
                    } catch (it: Exception) {
                        Timber.tag("HomeSyncService").e(it, "Failed to search metadata for local artist (attempt $attempt): $query")
                        if (it is java.net.UnknownHostException || it is java.net.ConnectException) {
                            status = 3
                        }
                    }
                    if (status != 0) break
                }
                if (status == 3) {
                    abortSync = true
                    break
                }
                if (status != 1) {
                    failedCount++
                    HomeSyncState.artistSyncFailed = failedCount
                    failedList.add(artist)
                }
            }
        }

        withContext(Dispatchers.Main) {
            if (abortSync) {
                app.kreate.android.me.knighthat.utils.Toaster.e(appContext().getString(R.string.sync_failed))
                HomeSyncState.showSyncNotification(appContext().getString(R.string.sync_failed), "Sync aborted due to network error.", notificationId)
            } else if (failedCount > 0) {
                HomeSyncState.failedArtistsList = failedList
                val errorMessage = appContext().getString(R.string.failed_artists, failedCount)
                val notificationMessage = appContext().getString(R.string.sync_failed_notification_artists, failedCount)
                app.kreate.android.me.knighthat.utils.Toaster.e(errorMessage)
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_completed_with_errors),
                    message = notificationMessage,
                    notificationId = notificationId
                )
            } else if (totalArtists > 0 && !ids.isNullOrEmpty()) {
                app.kreate.android.me.knighthat.utils.Toaster.s(appContext().getString(R.string.found_all_artists))
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_successful),
                    message = appContext().getString(R.string.sync_success_notification_artists),
                    notificationId = notificationId
                )
            } else if (totalArtists > 0 && ids.isNullOrEmpty()) {
                app.kreate.android.me.knighthat.utils.Toaster.s(appContext().getString(R.string.found_all_artists))
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_successful),
                    message = appContext().getString(R.string.sync_success_notification_artists),
                    notificationId = notificationId
                )
            } else {
                HomeSyncState.clearSyncNotification(notificationId)
            }
            HomeSyncState.isSyncingArtists = false
            HomeSyncState.artistSyncProgress = 1f
        }
    }

    private suspend fun syncAlbums(ids: List<String>?, notificationId: Int) {
        if (HomeSyncState.isSyncingAlbums) return
        HomeSyncState.isSyncingAlbums = true
        HomeSyncState.albumSyncProgress = 0f
        
        val allAlbums = Database.albumTable.all().first()
        val targetItems = if (ids.isNullOrEmpty()) allAlbums else allAlbums.filter { it.id in ids }

        val ytAlbums = targetItems.filter { it.isYoutubeAlbum }
        val localAlbums = targetItems.filterNot { it.isYoutubeAlbum }
        val totalAlbums = ytAlbums.size + localAlbums.size

        Timber.tag("HomeSyncService").d("=== REFRESH START === Total: $totalAlbums (YT: ${ytAlbums.size}, Local: ${localAlbums.size})")

        withContext(Dispatchers.Main) {
            if (totalAlbums > 0) app.kreate.android.me.knighthat.utils.Toaster.i(appContext().getString(R.string.refreshing_albums, totalAlbums))
            if (ids == null && totalAlbums == 0) app.kreate.android.me.knighthat.utils.Toaster.w(appContext().getString(R.string.sync_no_items))
        }

        if (totalAlbums == 0) {
            HomeSyncState.isSyncingAlbums = false
            return
        }

        var failedCount = 0
        val failedList = mutableListOf<Album>()
        HomeSyncState.albumSyncFailed = 0
        HomeSyncState.albumSyncTotal = totalAlbums

        var abortSync = false
        for ((index, album) in ytAlbums.withIndex()) {
            if (abortSync) break
            HomeSyncState.albumSyncCurrentIndex = index + 1
            HomeSyncState.albumSyncCurrentName = album.title ?: ""
            HomeSyncState.albumSyncProgress = index.toFloat() / ytAlbums.size
            HomeSyncState.showSyncNotification(
                title = appContext().getString(R.string.sync_notifications),
                message = appContext().getString(R.string.sync_progress_albums, index + 1, totalAlbums),
                notificationId = notificationId,
                isOngoing = true,
                maxProgress = totalAlbums,
                currentProgress = index + 1
            )

            kotlinx.coroutines.delay((2000L..5000L).random())
            Timber.tag("HomeSyncService").d("[YT] Fetching by ID: ${album.id} for '${album.title}'")

            var status = 0 // 0=retry, 1=success
            for (attempt in 1..3) {
                YtMusic.getAlbum(album.id, true).onSuccess { online ->
                    val onlineAlbum = online.album
                    Timber.tag("HomeSyncService").d("[YT] Got response for '${album.title}': onlineTitle='${onlineAlbum.title}', thumbnail='${onlineAlbum.thumbnail?.url}'")
                    Database.asyncTransaction {
                        Database.albumTable.upsert(Album(
                            id = album.id,
                            title = PropUtils.retainIfModified(album.title, onlineAlbum.title),
                            year = onlineAlbum.year ?: album.year,
                            authorsText = onlineAlbum.authors?.joinToString("") { it.name.orEmpty() } ?: album.authorsText,
                            shareUrl = online.url ?: album.shareUrl,
                            thumbnailUrl = onlineAlbum.thumbnail?.url ?: album.thumbnailUrl,
                            timestamp = album.timestamp,
                            bookmarkedAt = album.bookmarkedAt,
                            isYoutubeAlbum = album.isYoutubeAlbum,
                            position = album.position
                        ))
                    }
                    Timber.tag("HomeSyncService").d("Successfully refreshed album: ${album.title}")
                    status = 1
                }.onFailure {
                    Timber.tag("HomeSyncService").e(it, "Failed to refresh album (attempt $attempt): ${album.title}")
                    if (it is java.net.UnknownHostException || it is java.net.ConnectException) {
                        status = 3
                    }
                }
                if (status != 0) break
            }
            if (status == 3) {
                abortSync = true
                break
            }
            if (status != 1) {
                failedCount++
                HomeSyncState.albumSyncFailed = failedCount
                failedList.add(album)
            }
        }

        for ((index, album) in localAlbums.withIndex()) {
            if (abortSync) break
            HomeSyncState.albumSyncCurrentIndex = ytAlbums.size + index + 1
            HomeSyncState.albumSyncCurrentName = album.title ?: ""
            HomeSyncState.albumSyncProgress = (ytAlbums.size + index).toFloat() / totalAlbums
            HomeSyncState.showSyncNotification(
                title = appContext().getString(R.string.sync_notifications),
                message = appContext().getString(R.string.sync_progress_albums, ytAlbums.size + index + 1, totalAlbums),
                notificationId = notificationId,
                isOngoing = true,
                maxProgress = totalAlbums,
                currentProgress = ytAlbums.size + index + 1
            )

            val query = album.title?.trim()
            if (!query.isNullOrBlank()) {
                kotlinx.coroutines.delay((2000L..5000L).random())
                Timber.tag("HomeSyncService").d("[LOCAL] Searching YouTube: query='$query' for album id=${album.id}")
                var status = 0 // 0=retry, 1=success, 2=not found
                for (attempt in 1..3) {
                    try {
                        val searchResult = Innertube.searchPage<Innertube.AlbumItem>(
                            body = SearchBody(
                                query = query,
                                params = Innertube.SearchFilter.Album.value
                            ),
                            fromMusicShelfRendererContent = { content -> Innertube.AlbumItem.from(content) }
                        )?.getOrNull()

                        val remoteId = searchResult?.items?.firstOrNull()?.info?.endpoint?.browseId
                        if (remoteId != null) {
                            Timber.tag("HomeSyncService").d("[LOCAL] Found matching ID on YouTube: $remoteId for '${album.title}'")
                            YtMusic.getAlbum(remoteId, true).onSuccess { online ->
                                val onlineAlbum = online.album
                                Database.asyncTransaction {
                                    Database.albumTable.upsert(Album(
                                        id = album.id,
                                        title = album.title,
                                        year = onlineAlbum.year ?: album.year,
                                        authorsText = onlineAlbum.authors?.joinToString("") { it.name.orEmpty() } ?: album.authorsText,
                                        shareUrl = online.url ?: album.shareUrl,
                                        thumbnailUrl = onlineAlbum.thumbnail?.url ?: album.thumbnailUrl,
                                        timestamp = album.timestamp,
                                        bookmarkedAt = album.bookmarkedAt,
                                        isYoutubeAlbum = album.isYoutubeAlbum,
                                        position = album.position
                                    ))
                                }
                            }
                            status = 1
                        } else {
                            Timber.tag("HomeSyncService").d("No matching album found on YouTube for '${album.title}'")
                            status = 2
                        }
                    } catch (it: Exception) {
                        Timber.tag("HomeSyncService").e(it, "Failed to search metadata for local album (attempt $attempt): $query")
                        if (it is java.net.UnknownHostException || it is java.net.ConnectException) {
                            status = 3
                        }
                    }
                    if (status != 0) break
                }
                if (status == 3) {
                    abortSync = true
                    break
                }
                if (status != 1) {
                    failedCount++
                    HomeSyncState.albumSyncFailed = failedCount
                    failedList.add(album)
                }
            }
        }

        withContext(Dispatchers.Main) {
            if (abortSync) {
                app.kreate.android.me.knighthat.utils.Toaster.e(appContext().getString(R.string.sync_failed))
                HomeSyncState.showSyncNotification(appContext().getString(R.string.sync_failed), "Sync aborted due to network error.", notificationId)
            } else if (failedCount > 0) {
                HomeSyncState.failedAlbumsList = failedList
                val errorMessage = appContext().getString(R.string.failed_albums, failedCount)
                val notificationMessage = appContext().getString(R.string.sync_failed_notification_albums, failedCount)
                app.kreate.android.me.knighthat.utils.Toaster.e(errorMessage)
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_completed_with_errors),
                    message = notificationMessage,
                    notificationId = notificationId
                )
            } else if (totalAlbums > 0 && !ids.isNullOrEmpty()) {
                app.kreate.android.me.knighthat.utils.Toaster.s(appContext().getString(R.string.found_all_albums))
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_successful),
                    message = appContext().getString(R.string.sync_success_notification_albums),
                    notificationId = notificationId
                )
            } else if (totalAlbums > 0 && ids.isNullOrEmpty()) {
                app.kreate.android.me.knighthat.utils.Toaster.s(appContext().getString(R.string.found_all_albums))
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_successful),
                    message = appContext().getString(R.string.sync_success_notification_albums),
                    notificationId = notificationId
                )
            } else {
                HomeSyncState.clearSyncNotification(notificationId)
            }
            HomeSyncState.isSyncingAlbums = false
            HomeSyncState.albumSyncProgress = 1f
        }
    }
    
    private suspend fun syncPlaylists(ids: List<String>?, notificationId: Int) {
        if (HomeSyncState.isSyncingPlaylists) return
        HomeSyncState.isSyncingPlaylists = true
        HomeSyncState.playlistSyncProgress = 0f

        val allPlaylistsPreviews = Database.playlistTable.allAsPreview().first()
        val targetPlaylists = if (ids.isNullOrEmpty()) allPlaylistsPreviews else allPlaylistsPreviews.filter { it.playlist.id.toString() in ids }

        val ytPlaylists = targetPlaylists.filter { 
            it.playlist.isYoutubePlaylist || 
            it.playlist.browseId?.startsWith("VL") == true || 
            it.playlist.browseId?.startsWith("PL") == true || 
            it.playlist.browseId?.startsWith("RD") == true || 
            it.playlist.browseId?.startsWith("OLAK") == true 
        }

        Timber.tag("HomeSyncService").d("=== REFRESH START === Total playlists: ${ytPlaylists.size}")

        withContext(Dispatchers.Main) {
            if (ytPlaylists.isNotEmpty()) app.kreate.android.me.knighthat.utils.Toaster.i(appContext().getString(R.string.refreshing_playlists, ytPlaylists.size))
        }

        if (ytPlaylists.isEmpty()) {
            HomeSyncState.isSyncingPlaylists = false
            return
        }

        var failedCount = 0
        val failedList = mutableListOf<app.it.fast4x.rimusic.models.PlaylistPreview>()
        HomeSyncState.playlistSyncFailed = 0
        HomeSyncState.playlistSyncTotal = ytPlaylists.size
        
        var abortSync = false
        for ((index, preview) in ytPlaylists.withIndex()) {
            if (abortSync) break
            HomeSyncState.playlistSyncCurrentIndex = index + 1
            HomeSyncState.playlistSyncCurrentName = preview.playlist.name
            HomeSyncState.playlistSyncProgress = index.toFloat() / ytPlaylists.size
            HomeSyncState.showSyncNotification(
                title = appContext().getString(R.string.sync_notifications),
                message = appContext().getString(R.string.sync_progress_playlists, index + 1, ytPlaylists.size),
                notificationId = notificationId,
                isOngoing = true,
                maxProgress = ytPlaylists.size,
                currentProgress = index + 1
            )
            val p = preview.playlist
            val browseId = p.browseId
            if (browseId == null || !browseId.startsWith("VL")) {
                Timber.tag("HomeSyncService").d("Skipping playlist (not a youtube playlist or no browseId): ${p.name}")
                continue
            }
            kotlinx.coroutines.delay((2000L..5000L).random())
            Timber.tag("HomeSyncService").d("[YT] Fetching playlist by browseId: $browseId for '${p.name}'")

            var status = 0 // 0=retry, 1=success
            for (attempt in 1..3) {
                val request = Innertube.playlistPage(BrowseBody(browseId = browseId))
                if (request == null) {
                    Timber.tag("HomeSyncService").d("[YT] Request returned null for '${p.name}' (browseId: $browseId)")
                    status = 2
                    break
                }
                request.getOrNull()?.let { playlistPage ->
                    Timber.tag("HomeSyncService").d("[YT] Got response for '${p.name}': title='${playlistPage.title}', songs=${playlistPage.songsPage?.items?.size ?: 0}")
                    Database.asyncTransaction {
                        playlistTable.update(p.copy(
                            name = app.kreate.android.me.knighthat.utils.PropUtils.retainIfModified(p.name, playlistPage.title) ?: p.name
                        ))
                        val songs = playlistPage.songsPage?.items?.mapNotNull { it.asSong?.copy(totalPlayTimeMs = 1L) }
                        if (songs != null) {
                            songTable.upsert(songs)
                            songs.forEach { song ->
                                songPlaylistMapTable.map(song.id, p.id)
                            }
                        }
                    }
                    Timber.tag("HomeSyncService").d("Successfully refreshed playlist: ${p.name}")
                    status = 1
                } ?: run {
                    Timber.tag("HomeSyncService").e("Failed to fetch playlist (attempt $attempt): ${p.name}")
                    status = 3 // or handle properly
                }
                if (status != 0) break
            }
            if (status == 3) {
                abortSync = true
                break
            }
            if (status != 1) {
                failedCount++
                HomeSyncState.playlistSyncFailed = failedCount
                failedList.add(preview)
            }
        }

        withContext(Dispatchers.Main) {
            if (abortSync) {
                app.kreate.android.me.knighthat.utils.Toaster.e(appContext().getString(R.string.sync_failed))
                HomeSyncState.showSyncNotification(appContext().getString(R.string.sync_failed), "Sync aborted due to network error.", notificationId)
            } else if (failedCount > 0) {
                HomeSyncState.failedPlaylistsList = failedList
                val errorMessage = appContext().getString(R.string.failed_playlists, failedCount)
                val notificationMessage = appContext().getString(R.string.sync_failed_notification_playlists, failedCount)
                app.kreate.android.me.knighthat.utils.Toaster.e(errorMessage)
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_failed),
                    message = notificationMessage,
                    notificationId = notificationId
                )
            } else if (ytPlaylists.isNotEmpty() && ids.isNullOrEmpty()) {
                app.kreate.android.me.knighthat.utils.Toaster.s(appContext().getString(R.string.found_all_playlists))
                HomeSyncState.showSyncNotification(
                    title = appContext().getString(R.string.sync_successful),
                    message = appContext().getString(R.string.sync_success_notification_playlists),
                    notificationId = notificationId
                )
            } else {
                HomeSyncState.clearSyncNotification(notificationId)
            }
        }

        HomeSyncState.playlistSyncProgress = 1f
        HomeSyncState.isSyncingPlaylists = false
    }
}
