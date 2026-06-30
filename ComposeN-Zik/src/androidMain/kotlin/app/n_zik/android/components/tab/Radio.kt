package app.n_zik.android.components.tab

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.n_zik.android.R
import app.n_zik.android.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.models.Song
import app.n_zik.android.playback.services.PlayerServiceModern
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon

@UnstableApi
class Radio private constructor(
    private val binder: PlayerServiceModern.Binder?,
    private val menuState: MenuState,
    private val songs: () -> List<Song>
): MenuIcon, Descriptive {

    companion object {
        @Composable
        operator fun invoke( songs: () -> List<Song> ): Radio =
            Radio(
                LocalPlayerServiceBinder.current,
                LocalMenuState.current,
                songs
            )
    }

    override val iconId: Int = R.drawable.radio
    override val color: androidx.compose.ui.graphics.Color
        @Composable
        get() = if (binder?.isRadioActive == true) app.n_zik.android.colorPalette().accent else app.n_zik.android.colorPalette().text
    override val messageId: Int = R.string.start_radio
    override val menuIconTitle: String
        @Composable
        get() = stringResource(binder?.radioActionTextRes ?: messageId)

    override fun onShortClick() {
        binder?.startRadio( songs().random(), false, null, true )

        menuState.hide()
    }
}

