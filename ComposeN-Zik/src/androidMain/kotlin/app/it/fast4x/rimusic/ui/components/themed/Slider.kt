package app.it.fast4x.rimusic.ui.components.themed

import androidx.annotation.IntRange
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import app.n_zik.android.colorPalette
import app.n_zik.android.uiRoundnessShape
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider(
    isEnabled: Boolean = true,
    state: Float,
    setState: (Float) -> Unit,
    onSlideComplete: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    @IntRange(from = 0) steps: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isDragged || isPressed

    val thumbWidth by animateDpAsState(
        targetValue = if (isInteracting) 12.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    val thumbHeight by animateDpAsState(
        targetValue = if (isInteracting) 24.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    val animatedValue by animateFloatAsState(
        targetValue = state,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    val stepSize = if (steps > 0) (range.endInclusive - range.start) / (steps + 1) else 0f
    val magneticThreshold = if (stepSize > 0) stepSize * 0.15f else 0f

    androidx.compose.material3.Slider(
        enabled = isEnabled,
        value = animatedValue,
        onValueChange = { newValue ->
            var finalValue = newValue
            if (steps > 0) {
                val fraction = (newValue - range.start) / stepSize
                val closestStepIdx = kotlin.math.round(fraction)
                val closestValue = range.start + closestStepIdx * stepSize
                if (kotlin.math.abs(newValue - closestValue) <= magneticThreshold) {
                    finalValue = closestValue
                }
            }
            setState(finalValue)
        },
        onValueChangeFinished = onSlideComplete,
        valueRange = range,
        modifier = modifier,
        steps = 0,
        interactionSource = interactionSource,
        thumb = {
            Box(
                modifier = Modifier
                    .size(width = thumbWidth, height = thumbHeight)
                    .background(
                        color = if (isEnabled) colorPalette().onAccent else colorPalette().text.copy(alpha = 0.4f),
                        shape = uiRoundnessShape()
                    )
            )
        },
        track = { sliderState ->
            Box(contentAlignment = Alignment.CenterStart) {
                SliderDefaults.Track(
                    colors = SliderDefaults.colors(
                        thumbColor = colorPalette().onAccent,
                        activeTrackColor = colorPalette().accent,
                        inactiveTrackColor = colorPalette().text.copy(alpha = 0.75f),
                        disabledThumbColor = colorPalette().text.copy(alpha = 0.4f),
                        disabledActiveTrackColor = colorPalette().text.copy(alpha = 0.4f),
                        disabledInactiveTrackColor = colorPalette().text.copy(alpha = 0.4f)
                    ),
                    enabled = isEnabled,
                    sliderState = sliderState,
                    modifier = Modifier.clip(uiRoundnessShape())
                )
                
                if (steps > 0) {
                    val dotColor = colorPalette().background0.copy(alpha = 0.6f)
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    ) {
                        val trackWidth = size.width
                        val numIntervals = steps + 1
                        val edgePaddingPx = 8.dp.toPx()
                        for (i in 0 until numIntervals) {
                            val fraction = i.toFloat() / numIntervals
                            val x = (fraction * trackWidth).coerceIn(edgePaddingPx, trackWidth - edgePaddingPx)
                            drawCircle(
                                color = dotColor,
                                radius = 2.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, size.height / 2f)
                            )
                        }
                    }
                }
            }
        }
    )
}
