package app.it.fast4x.rimusic.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.utils.medium
import app.it.fast4x.rimusic.utils.secondary
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import app.n_zik.android.uiRoundnessShape

@Composable
inline fun Menu(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .padding(top = 48.dp)
            .let {
                 if (isLandscape) it.padding(horizontal = 72.dp) else it
            }
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .let {
                if (isLandscape) it.clip(uiRoundnessShape())
                else it.clip(uiRoundnessShape())
            }
            .background(colorPalette().background1)
            .padding(top = 2.dp)
            .padding(vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
            content()
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuEntry(
    painter: Painter,
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    secondaryText: String? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    disableScrollingText: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .clip(uiRoundnessShape()).combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 24.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette().text),
            modifier = Modifier
                .size(15.dp)
        )

        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .weight(1f)
        ) {
            BasicText(
                text = text,
                style = typography().xs.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (!disableScrollingText) it.basicMarquee(iterations = Int.MAX_VALUE) else it }
            )

            secondaryText?.let { secondaryText ->
                BasicText(
                    text = secondaryText,
                    style = typography().xxs.medium.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (!disableScrollingText) it.basicMarquee(iterations = Int.MAX_VALUE) else it }
                )
            }
        }

        trailingContent?.invoke()
    }
}

@Composable
fun MenuEntry(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    secondaryText: String? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    disableScrollingText: Boolean = false
) {
    MenuEntry(
        painterResource( icon ),
        text,
        onClick,
        onLongClick,
        secondaryText,
        enabled,
        trailingContent,
        disableScrollingText
    )
}

@Composable
inline fun <T> LazyMenu(
    items: List<T>,
    modifier: Modifier = Modifier,
    crossinline itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LazyColumn(
        modifier = modifier
            .padding(top = 48.dp)
            .let {
                if (isLandscape) it.padding(horizontal = 72.dp) else it
            }
            .fillMaxWidth()
            .let {
                if (isLandscape) it.clip(uiRoundnessShape())
                else it.clip(uiRoundnessShape())
            }
            .background(colorPalette().background1)
            .padding(top = 2.dp)
            .padding(vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
        }
        items(
            count = items.size,
            itemContent = { index ->
                itemContent(items[index])
            }
        )
    }
}



