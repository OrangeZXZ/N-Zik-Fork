package app.n_zik.android.components.menu.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.n_zik.android.R
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon

fun lyricsPickFromLrcLibMenuEntry(
    onShortClick: () -> Unit
): MenuIcon = object : MenuIcon, Descriptive, Clickable {
    override val iconId: Int = R.drawable.sync
    override val messageId: Int = R.string.pick_from
    
    @get:Composable
    override val menuIconTitle: String get() = stringResource(messageId) + " LrcLib.net"
    
    override fun onShortClick() = onShortClick()
    
    override fun onLongClick() {}
}
