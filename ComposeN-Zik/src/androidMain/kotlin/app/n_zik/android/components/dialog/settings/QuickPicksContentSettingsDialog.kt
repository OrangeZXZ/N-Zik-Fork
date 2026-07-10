package app.n_zik.android.components.dialog.settings

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.it.fast4x.rimusic.utils.showTipsKey
import app.it.fast4x.rimusic.utils.showChartsKey
import app.it.fast4x.rimusic.utils.showRelatedAlbumsKey
import app.it.fast4x.rimusic.utils.showSimilarArtistsKey
import app.it.fast4x.rimusic.utils.showNewAlbumsArtistsKey
import app.it.fast4x.rimusic.utils.showNewAlbumsKey
import app.it.fast4x.rimusic.utils.showPlaylistMightLikeKey
import app.it.fast4x.rimusic.utils.showMoodsAndGenresKey
import app.it.fast4x.rimusic.utils.showMonthlyPlaylistInQuickPicksKey
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.ToggleItem
import app.n_zik.android.components.dialog.common.ToggleListDialog

object QuickPicksContentSettingsDialog : Dialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.quick_picks_content)

    override var isActive: Boolean by mutableStateOf(false)

    @Composable
    override fun DialogBody() {
        val items = listOf(
            ToggleItem(
                id = "tips",
                iconRes = R.drawable.person,
                label = stringResource(R.string.tips),
                preferenceKey = showTipsKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "charts",
                iconRes = R.drawable.trending,
                label = stringResource(R.string.charts),
                preferenceKey = showChartsKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "related_albums",
                iconRes = R.drawable.album,
                label = stringResource(R.string.related_albums),
                preferenceKey = showRelatedAlbumsKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "similar_artists",
                iconRes = R.drawable.people,
                label = stringResource(R.string.similar_artists),
                preferenceKey = showSimilarArtistsKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "new_albums_artists",
                iconRes = R.drawable.alternative_version,
                label = stringResource(R.string.new_albums_of_your_artists),
                preferenceKey = showNewAlbumsArtistsKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "new_albums",
                iconRes = R.drawable.album,
                label = stringResource(R.string.new_albums),
                preferenceKey = showNewAlbumsKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "playlists_might_like",
                iconRes = R.drawable.playlist,
                label = stringResource(R.string.playlists_you_might_like),
                preferenceKey = showPlaylistMightLikeKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "moods_genres",
                iconRes = R.drawable.moods,
                label = stringResource(R.string.moods_and_genres),
                preferenceKey = showMoodsAndGenresKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "monthly_playlists",
                iconRes = R.drawable.featured_playlist,
                label = stringResource(R.string.show_monthly_playlists_in_quick_picks),
                preferenceKey = showMonthlyPlaylistInQuickPicksKey,
                defaultValue = true
            )
        )

        ToggleListDialog(
            items = items,
            contentHeight = 480.dp
        )
    }
}
