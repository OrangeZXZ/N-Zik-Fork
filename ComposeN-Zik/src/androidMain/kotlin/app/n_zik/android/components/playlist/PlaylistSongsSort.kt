package app.n_zik.android.components.playlist

import androidx.compose.ui.draw.clip

import app.n_zik.android.uiRoundnessShape

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.enums.PlaylistSongSortBy
import app.it.fast4x.rimusic.enums.SortOrder
import app.n_zik.android.typography
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.semiBold
import app.n_zik.android.components.Sort

class PlaylistSongsSort private constructor(
    menuState: MenuState,
    sortByState: MutableState<PlaylistSongSortBy>,
    sortOrderState: MutableState<SortOrder>,
    styleState: MutableState<MenuStyle>
): Sort<PlaylistSongSortBy>(menuState, sortByState, sortOrderState, styleState) {

    companion object {
        @Composable
        operator fun invoke(playlistId: Long) = PlaylistSongsSort(
            LocalMenuState.current,
            rememberPreference("PlaylistSongsSortBy_$playlistId", PlaylistSongSortBy.Title),
            rememberPreference("PlaylistSongsSortOrder_$playlistId", SortOrder.Ascending),
            rememberPreference( menuStyleKey, MenuStyle.List )
        )
    }

    override fun onLongClick() { /* Does nothing */ }

    @Composable
    override fun ToolBarButton() {
        super.ToolBarButton()

        BasicText(
            text = this.sortBy.text,
            style = typography().s.semiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clip(uiRoundnessShape()).clickable { super.onLongClick() }
        )
    }
}





