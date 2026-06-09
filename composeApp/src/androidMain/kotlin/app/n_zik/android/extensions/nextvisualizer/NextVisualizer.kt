package app.n_zik.android.extensions.nextvisualizer

import androidx.compose.ui.draw.clip

import app.n_zik.android.uiRoundnessShape

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.n_zik.android.R
import app.kreate.android.drawable.APP_ICON_BITMAP
import app.n_zik.android.core.coil.resize
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.colorPalette
import app.n_zik.android.extensions.nextvisualizer.painters.Painter
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftBar
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftCBar
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftCLine
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftCWave
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftCWaveRgb
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftLine
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftWave
import app.n_zik.android.extensions.nextvisualizer.painters.fft.FftWaveRgb
import app.n_zik.android.extensions.nextvisualizer.painters.misc.Gradient
import app.n_zik.android.extensions.nextvisualizer.painters.misc.Icon
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Beat
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Blend
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Compose
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Glitch
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Move
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Shake
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Scale
import app.n_zik.android.extensions.nextvisualizer.painters.waveform.WfmAnalog
import app.n_zik.android.extensions.nextvisualizer.utils.Preset
import app.n_zik.android.extensions.nextvisualizer.utils.VisualizerHelper
import app.n_zik.android.extensions.nextvisualizer.views.VisualizerView
import app.n_zik.android.typography
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.SecondaryTextButton
import app.it.fast4x.rimusic.utils.DisposableListener
import app.it.fast4x.rimusic.utils.currentVisualizerKey
import app.it.fast4x.rimusic.utils.getBitmapFromUrl
import app.it.fast4x.rimusic.utils.hasPermission
import app.it.fast4x.rimusic.utils.isCompositionLaunched
import app.it.fast4x.rimusic.utils.rememberPreference

import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showVisualizerButtonsKey
import app.it.fast4x.rimusic.utils.visualizerEnabledKey
import app.n_zik.android.extensions.nextvisualizer.painters.modifier.Rotate
import app.n_zik.android.thumbnailShape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(UnstableApi::class)
@Composable
fun NextVisualizer() {

    val context = LocalContext.current
    val visualizerEnabled by rememberPreference(visualizerEnabledKey, false)

    if (visualizerEnabled) {

        val permission = Manifest.permission.RECORD_AUDIO

        var relaunchPermission by remember {
            mutableStateOf(false)
        }

        var hasPermission by remember(isCompositionLaunched()) {
            mutableStateOf(context.applicationContext.hasPermission(permission))
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { hasPermission = it }
        )

        if (!hasPermission) {

            LaunchedEffect(Unit, relaunchPermission) { launcher.launch(permission) }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    2.dp,
                    Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = stringResource(R.string.require_mic_permission),
                    modifier = Modifier.fillMaxWidth(0.75f),
                    style = typography().xs.semiBold
                )
                Spacer(modifier = Modifier.height(20.dp))
                SecondaryTextButton(
                    text = stringResource(R.string.open_permission_settings),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                setData(Uri.fromParts("package", context.packageName, null))
                            }
                        )
                    }
                )
            }

        } else {

            val binder = LocalPlayerServiceBinder.current
            val visualizerView = remember { VisualizerView(context) }
            val helper = remember(binder?.player?.audioSessionId) {
                VisualizerHelper(binder?.player?.audioSessionId ?: 0)
            }

            var bitmapCover by remember { mutableStateOf(APP_ICON_BITMAP) }
            var circleBitmap by remember { mutableStateOf(Icon.getCircledBitmap(APP_ICON_BITMAP)) }
            val visualizerWhiteColorOption by rememberPreference(app.it.fast4x.rimusic.utils.visualizerWhiteColorOptionKey, app.n_zik.android.extensions.nextvisualizer.enums.VisualizerWhiteColorOption.White)
            val visualizerCustomColor by rememberPreference(app.it.fast4x.rimusic.utils.visualizerCustomColorKey, android.graphics.Color.WHITE)
            var dominantColor by remember { mutableStateOf(android.graphics.Color.WHITE) }
            
            val isDarkTheme = colorPalette().isDark
            LaunchedEffect(bitmapCover, isDarkTheme) {
                kotlinx.coroutines.withContext(Dispatchers.Default) {
                    try {
                        val dynPalette = app.it.fast4x.rimusic.ui.styling.dynamicColorPaletteOf(bitmapCover, isDarkTheme)
                        if (dynPalette != null) {
                            dominantColor = dynPalette.accent.toArgb()
                        } else {
                            val palette = androidx.palette.graphics.Palette.from(bitmapCover).generate()
                            dominantColor = palette.getDominantColor(android.graphics.Color.WHITE)
                        }
                    } catch (e: Exception) {
                        dominantColor = android.graphics.Color.WHITE
                    }
                }
            }
            
            val accentColor = colorPalette().accent
            val color = remember(visualizerWhiteColorOption, visualizerCustomColor, accentColor, dominantColor) {
                when (visualizerWhiteColorOption) {
                    app.n_zik.android.extensions.nextvisualizer.enums.VisualizerWhiteColorOption.White -> android.graphics.Color.WHITE
                    app.n_zik.android.extensions.nextvisualizer.enums.VisualizerWhiteColorOption.Theme -> accentColor.toArgb()
                    app.n_zik.android.extensions.nextvisualizer.enums.VisualizerWhiteColorOption.Cover -> dominantColor
                    app.n_zik.android.extensions.nextvisualizer.enums.VisualizerWhiteColorOption.Custom -> visualizerCustomColor
                }
            }

            val coroutineScope = rememberCoroutineScope()
            val currentArtworkUri = binder?.player?.currentMediaItem?.mediaMetadata?.artworkUri

            LaunchedEffect(currentArtworkUri) {
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = getBitmapFromUrl(
                            context,
                            currentArtworkUri.toString().resize(1000, 1000)
                        )
                        withContext(Dispatchers.Main) {
                            bitmapCover = bitmap
                            circleBitmap = Icon.getCircledBitmap(bitmap)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e("Failed to get bitmap in NextVisualizer ${e.stackTraceToString()}")
                        withContext(Dispatchers.Main) {
                            bitmapCover = APP_ICON_BITMAP
                            circleBitmap = Icon.getCircledBitmap(APP_ICON_BITMAP)
                        }
                    }
                }
            }

            binder?.player?.DisposableListener {
                object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val bitmap = getBitmapFromUrl(
                                    context,
                                    mediaItem?.mediaMetadata?.artworkUri.toString()
                                        .resize(1000, 1000)
                                )
                                withContext(Dispatchers.Main) {
                                    bitmapCover = bitmap
                                    circleBitmap = Icon.getCircledBitmap(bitmap)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    bitmapCover = APP_ICON_BITMAP
                                    circleBitmap = Icon.getCircledBitmap(APP_ICON_BITMAP)
                                }
                                Timber.e("Failed to get bitmap in NextVisualizer ${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            }
            
            val visualizersList = remember(bitmapCover, circleBitmap, color) {
                createVisualizersList(bitmapCover, circleBitmap, color)
            }
            
            var currentVisualizer by rememberPreference(currentVisualizerKey, 0)
            if (currentVisualizer < 0 || currentVisualizer >= visualizersList.size) currentVisualizer = 0

            val showVisualizerButtons by rememberPreference(showVisualizerButtonsKey, true)

            var showControls by remember { mutableStateOf(true) }
            var controlsTimerKey by remember { mutableStateOf(0) }

            LaunchedEffect(controlsTimerKey) {
                showControls = true
                delay(3000)
                showControls = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(thumbnailShape()).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { controlsTimerKey++ }
            ) {

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    factory = { visualizerView },
                    update = {
                        it.setup(helper, visualizersList[currentVisualizer])
                    }
                )

                AnimatedVisibility(
                    visible = !showVisualizerButtons || showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(
                                bottom = if (app.n_zik.android.thumbnailShape() == androidx.compose.foundation.shape.CircleShape) 16.dp else 0.dp
                            )
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                    IconButton(
                        onClick = {
                            controlsTimerKey++
                            if (currentVisualizer > 0) currentVisualizer--
                            else currentVisualizer = visualizersList.lastIndex
                        },
                        icon = R.drawable.arrow_left,
                        color = colorPalette().text,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    BasicText(
                        text = "${currentVisualizer + 1}/${visualizersList.size}",
                        style = typography().xs.semiBold.copy(color = colorPalette().text),
                    )
                    
                    IconButton(
                        onClick = {
                            controlsTimerKey++
                            if (currentVisualizer < visualizersList.lastIndex) currentVisualizer++
                            else currentVisualizer = 0
                        },
                        icon = R.drawable.arrow_right,
                        color = colorPalette().text,
                        modifier = Modifier.size(32.dp)
                    )
                    }
                }
            }
        }
    }
}

fun createVisualizersList(background: Bitmap, circleBitmap: Bitmap, color: Int): List<Painter> {
    val ampR = 6f
    val ampRfm = 3.3f
    val smoothWfm = 0.4f
    val smooth = 0.6f
    val ampRMin = 2f
    val yR = 0f
    return listOf(
        // Basic components
        Move(WfmAnalog(colorPaint = color, ampR = ampRfm, smooth = smoothWfm)),
        Move(FftBar(colorPaint = color, ampR = ampR), yR = 0.5f),
        Move(FftBar(colorPaint = color, side = "b", ampR = ampR), yR = -0.5f),
        Compose(
            Move(FftBar(colorPaint = color, ampR = 4f), yR = 0.5f),
            Scale(Move(FftBar(colorPaint = color, side = "b", ampR = 4f), yR = -0.5f), scaleX = -1f)
        ),
        Move(FftLine(colorPaint = color, ampR = ampR), yR = 0.5f),
        Move(FftLine(colorPaint = color, side = "b", ampR = ampR), yR = -0.5f),
        Compose(
            Move(FftLine(colorPaint = color, ampR = 4f), yR = 0.5f),
            Scale(Move(FftLine(colorPaint = color, side = "b", ampR = 4f), yR = -0.5f), scaleX = -1f)
        ),

        Move(FftWave(colorPaint = color, ampR = ampR), yR = 0.5f),
        Move(FftWave(colorPaint = color, side = "b", ampR = ampR), yR = -0.5f),
        Compose(
            Move(FftWave(colorPaint = color, ampR = 4f), yR = 0.5f),
            Scale(Move(FftWave(colorPaint = color, side = "b", ampR = 4f), yR = -0.5f), scaleX = -1f)
        ),

        Move(FftBar(colorPaint = color, side = "ab", ampR = 4f), yR = 0f),
        Move(FftLine(colorPaint = color, side = "ab", ampR = 4f), yR = 0f),
        Move(FftWave(colorPaint = color, side = "ab", ampR = 4f), yR = 0f),
        Compose(
            Move(FftBar(colorPaint = color, side = "ab"), yR = -.3f),
            Move(FftLine(colorPaint = color, side = "ab"), yR = 0f),
            Move(FftWave(colorPaint = color, side = "ab"), yR = .3f)
        ),
        // Basic components (Circle)
        Move(FftCLine(colorPaint = color, ampR = ampRMin)),
        FftCWave(colorPaint = color, ampR = ampRMin),
        FftCWave(side = "b", colorPaint = color, ampR = ampRMin),
        Compose(
            Move(FftCLine(colorPaint = color, side = "b", ampR = ampRMin)),
            FftCWave(side = "b", colorPaint = color, ampR = ampRMin)
        ),
        Compose(
            Move(FftCLine(colorPaint = color, side = "ab", ampR = ampRMin)),
            FftCWave(side = "ab", colorPaint = color, ampR = ampRMin)
        ),
        Move(
            FftCBar(colorPaint = color, side = "ab", gapX = 8f, ampR = ampRMin).apply {
                paint.style = Paint.Style.FILL
            }
        ),
        // Composition
        Compose(
            WfmAnalog(colorPaint = color, ampR = ampRfm, smooth = smoothWfm).apply { paint.alpha = 150 },
            Shake(
                Compose(
                    Rotate(FftCWave(colorPaint = color, ampR = ampRMin)),
                    app.n_zik.android.extensions.nextvisualizer.painters.misc.Icon(circleBitmap)
                )
            ).apply {
                animX.duration = 1000
                animY.duration = 2000
            }
        )
    )
}





