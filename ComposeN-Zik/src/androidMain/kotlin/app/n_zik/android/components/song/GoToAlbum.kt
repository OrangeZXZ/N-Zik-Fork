package app.n_zik.android.components.song

import app.n_zik.android.core.database.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.n_zik.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import app.n_zik.android.core.database.Database
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.ui.components.MenuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import java.util.Optional

class GoToAlbum(
    private val navController: NavController,
    private val song: Song,
    private val menuState: MenuState
): MenuIcon, Descriptive {

    override val iconId: Int = R.drawable.album
    override val messageId: Int = R.string.go_to_album
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    private var albumId: Optional<String> = Optional.empty()

    init {
        CoroutineScope( Dispatchers.IO ).launch {
            Database.albumTable
                    .findBySongId( song.id )
                    .first()
                    ?.id
                    ?.let { albumId = Optional.of( it ) }
        }
    }


    override fun onShortClick() {
        menuState.hide()
        albumId.ifPresentOrElse(
            { NavRoutes.album.navigateHere( navController, it ) },
            {
                Toaster.i( R.string.looking_up_album_from_the_internet )

                CoroutineScope( Dispatchers.IO ).launch {
                    val endpoint = Innertube.nextPage(NextBody(videoId = song.id))
                             ?.onFailure {
                                 Timber.tag("go_to_album").e(it, "nextPage failed")
                                 Toaster.e( R.string.failed_to_fetch_original_property )
                             }
                             ?.getOrNull()
                             ?.itemsPage
                             ?.items
                             ?.firstOrNull()
                             ?.album
                             ?.endpoint
                             ?.takeIf { !it.browseId.isNullOrBlank() }

                    Timber.tag("go_to_album").d("Up Next API album endpoint: %s", endpoint?.browseId)

                    if (endpoint != null) {
                        val path = "${endpoint.browseId}?params=${endpoint.params.orEmpty()}"
                        NavRoutes.album.navigateHere( navController, path )
                    } else {
                        val query = "${song.title} ${song.artistsText ?: ""}".trim()
                        Timber.tag("go_to_album").d("Fallback search query: %s", query)

                        val searchResult = Innertube.searchPage<Innertube.SongItem>(
                            SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
                            { content -> Innertube.SongItem.from(content) }
                        )?.getOrNull()
                        
                        Timber.tag("go_to_album").d("Search result items count: %s", searchResult?.items?.size)
                        
                        val foundSong = searchResult?.items?.firstOrNull { it.key == song.id }
                        val albumEndpoint = foundSong?.album?.endpoint
                        
                        if (albumEndpoint != null && !albumEndpoint.browseId.isNullOrBlank()) {
                            Timber.tag("go_to_album").d("Found album: %s (ID: %s)", foundSong.album?.name, albumEndpoint.browseId)
                            val path = "${albumEndpoint.browseId}?params=${albumEndpoint.params.orEmpty()}"
                            Toaster.s( R.string.album_found_online_verify )
                            NavRoutes.album.navigateHere( navController, path )
                        } else {
                            Timber.tag("go_to_album").e("No album found in fallback search")
                            Toaster.e( R.string.failed_to_fetch_album )
                        }
                    }
                }
            }
        )
    }
}
