package app.n_zik.android.playback.services.automotive.models

import app.it.fast4x.rimusic.models.Song
import it.fast4x.innertube.Innertube

object AutoSearchState {
    var searchedSongs: List<Song> = emptyList()
    var searchedArtists: List<Innertube.ArtistItem> = emptyList()
    var searchedVideos: List<Innertube.VideoItem> = emptyList()
    var searchedAlbums: List<Innertube.AlbumItem> = emptyList()

    fun clear() {
        searchedSongs = emptyList()
        searchedArtists = emptyList()
        searchedVideos = emptyList()
        searchedAlbums = emptyList()
    }
}
