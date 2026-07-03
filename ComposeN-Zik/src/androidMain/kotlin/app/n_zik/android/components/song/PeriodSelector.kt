package app.n_zik.android.components.song

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.enums.MaxTopPlaylistItems
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.enums.StatisticsType
import app.n_zik.android.typography
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.n_zik.android.components.menu.ListMenu
import androidx.compose.material3.Icon
import app.it.fast4x.rimusic.utils.MaxTopPlaylistItemsKey
import app.it.fast4x.rimusic.utils.Preference
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold

class PeriodSelector private constructor(
    override val menuState: MenuState,
    periodState: MutableState<StatisticsType>,
    styleState: MutableState<MenuStyle>,
): MenuIcon, Descriptive, Menu {

    companion object {
        @Composable
        operator fun invoke( prefKey: Preference.Key<StatisticsType> ): PeriodSelector =
            PeriodSelector(
                LocalMenuState.current,
                Preference.remember( prefKey ),
                rememberPreference( menuStyleKey, MenuStyle.List )
            )
    }

    var period: StatisticsType by periodState

    override val iconId: Int = period.iconId
    override val messageId: Int = R.string.statistics
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override var menuStyle: MenuStyle by styleState

    fun onDismiss( period: StatisticsType ) {
        this.period = period
        menuState.hide()
    }

    override fun onShortClick() = openMenu()

    @Composable
    override fun ListMenu() { /* Does nothing */ }

    @Composable
    override fun GridMenu() { /* Does nothing */ }

    @Composable
    fun SettingIcon(@DrawableRes icon: Int) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = colorPalette().accent.copy(alpha = 0.1f),
                    shape = uiRoundnessShape()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                tint = colorPalette().accent,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    @Composable
    override fun MenuComponent() {
        val size by rememberPreference( MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10` )

        ListMenu.Menu(title = stringResource( R.string.header_view_top_of, size )) {
            StatisticsType.entries.forEach {
                ListMenu.Entry(
                    text = it.text,
                    icon = { SettingIcon(R.drawable.time) },
                    onClick = {
                        onDismiss( it )
                    }
                )
            }
        }
    }
}

