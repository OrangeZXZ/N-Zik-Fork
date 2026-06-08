package app.it.fast4x.rimusic.ui.styling

import app.n_zik.android.uiRoundnessShape

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.CornerSize

class BoundedCornerSize(private val dp: Dp, private val maxFraction: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float {
        val requestedPx = with(density) { dp.toPx() }
        val maxPx = shapeSize.minDimension * maxFraction
        return kotlin.math.min(requestedPx, maxPx)
    }
}

data class Appearance(
    val colorPalette: ColorPalette,
    val typography: Typography,
    val thumbnailShape: Shape,
    val uiRoundnessShape: Shape
) {
    companion object : Saver<Appearance, List<Any>> {
        @Suppress("UNCHECKED_CAST")
        override fun restore(value: List<Any>): Appearance {
            return Appearance(
                colorPalette = ColorPalette.restore(value[0] as List<Any>),
                typography = Typography.restore(value[1] as List<Any>),
                thumbnailShape = RoundedCornerShape((value[2] as Int).dp),
                uiRoundnessShape = RoundedCornerShape((value[3] as Int).dp)
            )
        }

        override fun SaverScope.save(value: Appearance): List<Any> {
            return listOf(
                with(ColorPalette.Companion) { save(value.colorPalette) },
                with(Typography.Companion) { save(value.typography) },
                0,
                when (value.uiRoundnessShape) {
                    is RoundedCornerShape -> {
                        // We serialize the float corner size
                        // A rough approximation for restoring, although this is just for Saver
                        // We will just return 0, the actual value comes from preferences in MainActivity
                        0
                    }
                    else -> 0
                }
            )
        }
    }
}

val LocalAppearance = staticCompositionLocalOf<Appearance> { TODO() }