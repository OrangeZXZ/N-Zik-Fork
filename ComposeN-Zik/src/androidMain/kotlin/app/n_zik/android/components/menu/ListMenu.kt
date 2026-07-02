package app.n_zik.android.components.menu

import androidx.compose.ui.draw.clip

import app.n_zik.android.uiRoundnessShape

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.n_zik.android.colorPalette
import app.it.fast4x.rimusic.utils.conditional
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.n_zik.android.components.menu.MenuConstants.CONTENT_HEIGHT_FRACTION
import app.n_zik.android.components.menu.MenuConstants.CONTENT_TOP_PADDING

object ListMenu {

    @Composable
    fun Menu( showDragHandle: Boolean = true, content: @Composable ColumnScope.() -> Unit ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp

        Column(
            Modifier.heightIn( max = (screenHeight * CONTENT_HEIGHT_FRACTION).dp )
                    .padding(
                        top = CONTENT_TOP_PADDING.dp
                        // bottom padding is handled by [Modifier#navigationBarsPadding]
                    )
                    .verticalScroll( rememberScrollState() )
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                if (showDragHandle) {
                    Box(
                        modifier = Modifier
                            .padding(top = 18.dp, bottom = 6.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
                content()
            }
        )
    }

    @Composable
    fun Entry(
        text: String,
        icon: @Composable RowScope.() -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
        trailingContent: @Composable () -> Unit = {}
    ) = Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy( 24.dp ),
        modifier = modifier.fillMaxWidth()
                           .alpha( if (enabled) 1f else 0.4f )
                           .clip(uiRoundnessShape()).combinedClickable(
                               enabled = enabled,
                               onClick = onClick,
                               onLongClick = onLongClick
                           )
                           .padding( horizontal = 24.dp )
    ) {
        icon()

        Column(
            modifier = Modifier.padding( vertical = 16.dp )
                               .weight( 1f )
        ) {
            val isScrollingTextDisabled by rememberPreference( disableScrollingTextKey, false )

            Text(
                text = text,
                overflow = TextOverflow.Ellipsis,
                color = colorPalette().text,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
                                   .conditional( !isScrollingTextDisabled ) {
                                       basicMarquee( iterations = Int.MAX_VALUE )
                                   }
            )
        }

        trailingContent()
    }
}






