package app.n_zik.android.extensions.audiobar.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import app.it.fast4x.rimusic.models.ui.UiMedia
import app.n_zik.android.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.ui.components.ProgressPercentage
import app.it.fast4x.rimusic.ui.components.SeekBarAudioWaves
import app.n_zik.android.colorPalette
import app.n_zik.android.download.utils.MyDownloadHelper
import app.n_zik.android.extensions.audiobar.utils.WaveformExtractor
import kotlin.math.abs

@Composable
fun SeekBarStaticAudioWaves(
    modifier: Modifier = Modifier,
    uiMedia: UiMedia?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onPositionChange: (Long) -> Unit,
    onPositionChangeFinished: (() -> Unit)? = null,
    audioSessionId: () -> Int = { -1 }
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val caches = remember { 
        listOfNotNull(
            binder?.downloadCache ?: MyDownloadHelper.getDownloadCache(context),
            binder?.cache
        )
    }
    
    var amplitudes by remember { mutableStateOf<List<Int>?>(null) }
    var isExtracting by remember { mutableStateOf(false) }

    LaunchedEffect(uiMedia?.id) {
        if (uiMedia != null) {
            amplitudes = null // CLEAR previous waveform
            isExtracting = true
            
            // Retry loop in case the stream hasn't started caching yet
            var extracted: List<Int>? = null
            for (i in 0..5) { // Try 6 times (up to 3 seconds)
                extracted = WaveformExtractor.getOrExtractWaveform(context, uiMedia.id, caches)
                if (extracted != null) break
                kotlinx.coroutines.delay(500)
            }
            amplitudes = extracted
            isExtracting = false
        } else {
            amplitudes = null
            isExtracting = false
        }
    }

    val playedColor = colorPalette().accent
    val unplayedColor = colorPalette().textSecondary.copy(alpha = 0.3f)

    if (amplitudes != null) {
        // Draw static waves
        val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
        
        val updatedOnPositionChange by rememberUpdatedState(onPositionChange)
        val updatedOnPositionChangeFinished by rememberUpdatedState(onPositionChangeFinished)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(duration) {
                    detectTapGestures(
                        onPress = { offset ->
                            val newPosition = (offset.x / size.width) * duration
                            updatedOnPositionChange(newPosition.toLong())
                        },
                        onTap = { offset ->
                            val newPosition = (offset.x / size.width) * duration
                            updatedOnPositionChange(newPosition.toLong())
                            updatedOnPositionChangeFinished?.invoke()
                        }
                    )
                }
                .pointerInput(duration) {
                    var dragPosition: Float = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragPosition = offset.x
                            val newPosition = (dragPosition / size.width) * duration
                            updatedOnPositionChange(newPosition.toLong())
                        },
                        onDragEnd = {
                            updatedOnPositionChangeFinished?.invoke()
                        },
                        onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                            change.consume()
                            dragPosition += dragAmount
                            dragPosition = dragPosition.coerceIn(0f, size.width.toFloat())
                            val newPosition = (dragPosition / size.width) * duration
                            updatedOnPositionChange(newPosition.toLong())
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                val maxAmp = amplitudes!!.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                
                val barCount = 100 // We draw 100 bars across the width
                val barWidth = canvasWidth / (barCount * 1.5f) // Leave space between bars
                val spacing = (canvasWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)
                
                // Group the amplitudes into 100 chunks
                val chunkSize = (amplitudes!!.size.toFloat() / barCount).coerceAtLeast(1f)
                
                for (i in 0 until barCount) {
                    val startIdx = (i * chunkSize).toInt().coerceIn(0, amplitudes!!.size - 1)
                    val endIdx = ((i + 1) * chunkSize).toInt().coerceIn(0, amplitudes!!.size)
                    
                    var chunkMax = 0
                    if (startIdx < endIdx) {
                        for (j in startIdx until endIdx) {
                            if (amplitudes!![j] > chunkMax) chunkMax = amplitudes!![j]
                        }
                    } else {
                        chunkMax = amplitudes!![startIdx]
                    }
                    
                    // Normalize chunk amplitude between 0.1 and 1.0
                    val normalizedAmp = (chunkMax / maxAmp).coerceIn(0.1f, 1.0f)
                    val barHeight = canvasHeight * normalizedAmp
                    
                    val x = i * (barWidth + spacing)
                    val y = (canvasHeight - barHeight) / 2f
                    
                    val isPlayed = (i.toFloat() / barCount) <= progress
                    val barColor = if (isPlayed) playedColor else unplayedColor
                    
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }
        }
    } else {
        val currentPosition by rememberUpdatedState(position)
        val currentDuration by rememberUpdatedState(duration)

        // Fallback to real-time fake audio waves
        SeekBarAudioWaves(
            modifier = modifier.height(40.dp),
            audioSessionIdProvider = { null },
            isPlaying = isPlaying,
            isRealtime = false,
            isFake = true,
            progressPercentage = { ProgressPercentage.safeValue((if (currentDuration > 0) currentPosition.toFloat() / currentDuration.toFloat() else 0f).coerceIn(0f, 1f)) },
            playedColor = playedColor,
            notPlayedColor = unplayedColor,
            waveInteraction = {
                val newPosition = (it.value * currentDuration).toLong()
                onPositionChange(newPosition)
                onPositionChangeFinished?.invoke()
            }
        )
    }
}
