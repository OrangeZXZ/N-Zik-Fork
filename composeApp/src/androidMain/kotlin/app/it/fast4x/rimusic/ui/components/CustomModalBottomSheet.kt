package app.it.fast4x.rimusic.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.rememberPreference
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModalBottomSheet(
    showSheet: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    ),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets.ime },
    content: @Composable ColumnScope.() -> Unit,
) {
    val bottomPadding = if(isLandscape) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (showSheet) {

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            dragHandle = dragHandle,
            contentWindowInsets = contentWindowInsets
        ) {
            val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
            val isPicthBlack = colorPaletteMode == ColorPaletteMode.PitchBlack
            val isDark =
                colorPaletteMode == ColorPaletteMode.Dark || isPicthBlack || (colorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme())

            Column(modifier = Modifier.padding(bottom = bottomPadding)) {

                val view = LocalView.current
                (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { window ->
                    androidx.compose.runtime.DisposableEffect(window, containerColor) {
                        val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(containerColor.toArgb())
                        val isLightBackground = luminance > 0.5 

                        val applyInsets = {
                            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                window.statusBarColor = android.graphics.Color.TRANSPARENT
                            }

                            val insetsController = WindowCompat.getInsetsController(window, view)
                            insetsController.isAppearanceLightNavigationBars = isLightBackground
                            insetsController.isAppearanceLightStatusBars = isLightBackground
                        }

                        if (view.isAttachedToWindow) {
                            applyInsets()
                            view.post { applyInsets() }
                        }

                        val listener = object : android.view.View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: android.view.View) {
                                applyInsets()
                                view.post { applyInsets() }
                            }
                            override fun onViewDetachedFromWindow(v: android.view.View) {}
                        }
                        view.addOnAttachStateChangeListener(listener)

                        onDispose {
                            view.removeOnAttachStateChangeListener(listener)
                        }
                    }
                }


                content()
            }
        }
    }
}


