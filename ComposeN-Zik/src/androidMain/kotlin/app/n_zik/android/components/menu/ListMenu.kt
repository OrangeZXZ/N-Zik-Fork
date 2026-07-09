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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.n_zik.android.colorPalette
import app.it.fast4x.rimusic.utils.conditional
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.n_zik.android.typography
import app.n_zik.android.components.menu.MenuConstants.CONTENT_HEIGHT_FRACTION
import androidx.compose.foundation.shape.CornerSize

object ListMenu {

    @Composable
    fun Menu( showDragHandle: Boolean = true, title: String? = null, content: @Composable ColumnScope.() -> Unit ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val hasHeader = showDragHandle || title != null

        Column(
            Modifier.heightIn( max = (screenHeight * CONTENT_HEIGHT_FRACTION).dp )
                    .fillMaxWidth()
                    .background(colorPalette().background0),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                if (hasHeader) {
                    // Header with handle bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(colorPalette().background1)
                    ) {
                        if (showDragHandle) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 18.dp, bottom = 6.dp)
                                    .size(width = 40.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }

                        title?.let {
                            Text(
                                text = it,
                                style = typography().m.copy(color = colorPalette().text),
                                modifier = Modifier.padding(top = 5.dp, bottom = 10.dp)
                            )
                        }

                        HorizontalDivider(Modifier.height(1.dp))
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(colorPalette().background0)
                        .conditional(!hasHeader) {
                            clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    content()
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        )
    }

    @Composable
    fun Entry(
        text: String,
        icon: @Composable RowScope.() -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        subtitle: String? = null,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
        trailingContent: @Composable () -> Unit = {}
    ) {
        val alpha = if (enabled) 1f else 0.5f
        val isScrollingTextDisabled by rememberPreference( disableScrollingTextKey, false )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy( 12.dp ),
            modifier = modifier
                .fillMaxWidth()
                .clip(uiRoundnessShape())
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding( vertical = 10.dp )
        ) {
            icon()

            Column(
                modifier = Modifier.weight( 1f )
            ) {
                Text(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    color = colorPalette().text.copy(alpha = alpha),
                    textAlign = TextAlign.Start,
                    style = typography().s.semiBold,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                                       .conditional( !isScrollingTextDisabled ) {
                                           basicMarquee( iterations = Int.MAX_VALUE )
                                       }
                )

                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        color = colorPalette().textSecondary.copy(alpha = alpha),
                        textAlign = TextAlign.Start,
                        style = typography().xs,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                                           .conditional( !isScrollingTextDisabled ) {
                                               basicMarquee( iterations = Int.MAX_VALUE )
                                           }
                    )
                }
            }

            trailingContent()
        }
    }
}