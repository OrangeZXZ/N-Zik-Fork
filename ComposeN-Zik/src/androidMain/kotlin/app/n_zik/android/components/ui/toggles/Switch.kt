package app.n_zik.android.components.ui.toggles

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.n_zik.android.colorPalette
import app.n_zik.android.exactUiRoundnessShape

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    checkedThumbColor: Color = colorPalette().onAccent,
    checkedTrackColor: Color = colorPalette().accent,
    uncheckedThumbColor: Color = colorPalette().textSecondary,
    uncheckedTrackColor: Color = colorPalette().textSecondary.copy(alpha = 0.3f),
    enabled: Boolean = true
) {
    val trackColor by animateColorAsState(if (checked) checkedTrackColor else uncheckedTrackColor)
    val thumbColor by animateColorAsState(if (checked) checkedThumbColor else uncheckedThumbColor)
    
    // M3 Switch track is 52.dp wide, 32.dp high.
    val thumbSize by animateDpAsState(if (checked) 24.dp else 16.dp, animationSpec = tween(150))
    
    // If thumb is 24.dp, its margin is 4.dp. So x offset ranges from 4.dp to 52-24-4=24.dp.
    // If thumb is 16.dp, its margin is 8.dp. So x offset ranges from 8.dp to 52-16-8=28.dp.
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 8.dp, animationSpec = tween(150))

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .background(trackColor, shape = exactUiRoundnessShape())
            .clip(exactUiRoundnessShape())
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(enabled = enabled) {
                        onCheckedChange.invoke(!checked)
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .background(thumbColor, shape = exactUiRoundnessShape())
        )
    }
}
