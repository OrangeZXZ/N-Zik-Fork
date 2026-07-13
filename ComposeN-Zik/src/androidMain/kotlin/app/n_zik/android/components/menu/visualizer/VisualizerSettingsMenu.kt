package app.n_zik.android.components.menu.visualizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import app.n_zik.android.components.ui.toggles.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import app.kreate.android.me.knighthat.utils.Toaster
import androidx.media3.common.util.UnstableApi
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.themed.DialogColorPicker
import app.it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog
import app.n_zik.android.components.menu.ListMenu
import app.n_zik.android.components.menu.GridMenu
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.n_zik.android.components.ui.sliders.SliderControl
import app.it.fast4x.rimusic.ui.components.themed.TitleMiniSection
import app.it.fast4x.rimusic.utils.blackBackgroundForVisThumbnailKey
import app.it.fast4x.rimusic.utils.currentVisualizerKey
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.visualizerWhiteColorOptionKey
import app.it.fast4x.rimusic.utils.visualizerCustomColorKey
import enums.VisualizerWhiteColorOption
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showVisualizerButtonsKey
import app.it.fast4x.rimusic.utils.showVisualizerStateKey
import app.it.fast4x.rimusic.utils.showvisthumbnailKey
import app.it.fast4x.rimusic.utils.visualizerLineThicknessKey
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape

@UnstableApi
class VisualizerSettingsMenu private constructor(
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
) : Menu {

    companion object {
        @Composable
        operator fun invoke(): VisualizerSettingsMenu =
            VisualizerSettingsMenu(
                menuState = LocalMenuState.current,
                styleState = rememberPreference(menuStyleKey, MenuStyle.List)
            )
    }

    override var menuStyle: MenuStyle by styleState

    @Composable
    override fun ListMenu() { /* Handled in MenuComponent */ }

    @Composable
    override fun GridMenu() { /* Handled in MenuComponent */ }

    @Composable
    override fun MenuComponent() {
        var showvisthumbnail by rememberPreference(showvisthumbnailKey, true)
        var blackBackgroundForVisThumbnail by rememberPreference(blackBackgroundForVisThumbnailKey, true)
        var showVisualizerButtons by rememberPreference(showVisualizerButtonsKey, true)
        var showVisualizerStateKeyPref by rememberPreference(showVisualizerStateKey, false)
        
        var visualizerLineThickness by rememberPreference(visualizerLineThicknessKey, 6f)
        var currentVisualizer by rememberPreference(currentVisualizerKey, 0)
        
        var visualizerWhiteColorOption by rememberPreference(visualizerWhiteColorOptionKey, VisualizerWhiteColorOption.White)
        var visualizerCustomColor by rememberPreference(visualizerCustomColorKey, android.graphics.Color.WHITE)
        var isShowingCustomColorPicker by remember { mutableStateOf(false) }

        if (isShowingCustomColorPicker) {
            val customColorString = stringResource(R.string.color_custom)
            DialogColorPicker(onDismiss = { isShowingCustomColorPicker = false }) {
                visualizerCustomColor = it.toArgb()
                isShowingCustomColorPicker = false
                Toaster.n(R.string.info_color_s_applied, customColorString)
            }
        }

        @Composable
        fun settingsContent() {
            // Section Title for General settings
            SectionTitle(stringResource(R.string.visualizer))

            // Colors Option
            EnumSettingEntry(
                title = stringResource(R.string.visualizer_white_color_option),
                icon = R.drawable.color_palette,
                selectedValue = visualizerWhiteColorOption,
                values = VisualizerWhiteColorOption.entries.toList(),
                valueText = { value ->
                    stringResource(when(value) { 
                        VisualizerWhiteColorOption.White -> R.string.color_white
                        VisualizerWhiteColorOption.Theme -> R.string.bg_colors_background_from_theme
                        VisualizerWhiteColorOption.Cover -> R.string.bg_colors_background_from_cover
                        VisualizerWhiteColorOption.Custom -> R.string.color_custom
                    })
                },
                onValueSelected = { visualizerWhiteColorOption = it }
            )
            
            AnimatedVisibility(visible = visualizerWhiteColorOption == VisualizerWhiteColorOption.Custom) {
                ListMenu.Entry(
                    text = stringResource(R.string.color_custom),
                    icon = { SettingIcon(R.drawable.color_palette) },
                    onClick = { isShowingCustomColorPicker = true },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(uiRoundnessShape())
                                .background(Color(visualizerCustomColor))
                                .border(BorderStroke(1.dp, colorPalette().textSecondary.copy(alpha = 0.3f)), uiRoundnessShape())
                        )
                    }
                )
            }

            // Save Visualizer State
            ToggleSettingEntry(
                title = stringResource(R.string.save_visualizer_state),
                icon = R.drawable.bookmark,
                isChecked = showVisualizerStateKeyPref,
                onCheckedChange = { showVisualizerStateKeyPref = it }
            )
            
            // Show Thumbnail
            ToggleSettingEntry(
                title = stringResource(R.string.showvisthumbnail),
                icon = R.drawable.equalizer,
                isChecked = showvisthumbnail,
                onCheckedChange = { showvisthumbnail = it }
            )

            AnimatedVisibility(visible = showvisthumbnail) {
                ToggleSettingEntry(
                    title = stringResource(R.string.black_background_for_visualizer),
                    icon = R.drawable.images_sharp,
                    isChecked = blackBackgroundForVisThumbnail,
                    onCheckedChange = { blackBackgroundForVisThumbnail = it }
                )
            }

            // Auto-hide buttons
            ToggleSettingEntry(
                title = stringResource(R.string.show_visualizer_buttons),
                icon = R.drawable.menu,
                isChecked = showVisualizerButtons,
                onCheckedChange = { showVisualizerButtons = it }
            )
            
            // Line thickness slider
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.visualizer_line_thickness))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (currentVisualizer == 0 || currentVisualizer == 1 || currentVisualizer == 21) 1f else 0.5f)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingIcon(R.drawable.sound_effect)
                Spacer(modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    SliderControl(
                        state = visualizerLineThickness,
                        onSlide = { if (currentVisualizer == 0 || currentVisualizer == 1 || currentVisualizer == 21) visualizerLineThickness = it },
                        onSlideComplete = {},
                        toDisplay = { "%.0f".format(it) },
                        range = 1f..20f,
                        stepSize = 1f
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        fun androidx.compose.foundation.lazy.grid.LazyGridScope.settingsGridContent() {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.visualizer))
            }

            item {
                EnumSettingEntry(
                    title = stringResource(R.string.visualizer_white_color_option),
                    icon = R.drawable.color_palette,
                    selectedValue = visualizerWhiteColorOption,
                    values = VisualizerWhiteColorOption.entries.toList(),
                    valueText = { value ->
                        stringResource(when(value) { 
                            VisualizerWhiteColorOption.White -> R.string.color_white
                            VisualizerWhiteColorOption.Theme -> R.string.bg_colors_background_from_theme
                            VisualizerWhiteColorOption.Cover -> R.string.bg_colors_background_from_cover
                            VisualizerWhiteColorOption.Custom -> R.string.color_custom
                        })
                    },
                    onValueSelected = { visualizerWhiteColorOption = it }
                )
            }
            
            if (visualizerWhiteColorOption == VisualizerWhiteColorOption.Custom) {
                item {
                    GridMenu.Entry(
                        text = stringResource(R.string.color_custom),
                        icon = { SettingIcon(R.drawable.color_palette) },
                        onClick = { isShowingCustomColorPicker = true },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(uiRoundnessShape())
                                    .background(Color(visualizerCustomColor))
                                    .border(BorderStroke(1.dp, colorPalette().textSecondary.copy(alpha = 0.3f)), uiRoundnessShape())
                            )
                        }
                    )
                }
            }

            item {
                ToggleSettingEntry(
                    title = stringResource(R.string.save_visualizer_state),
                    icon = R.drawable.bookmark,
                    isChecked = showVisualizerStateKeyPref,
                    onCheckedChange = { showVisualizerStateKeyPref = it }
                )
            }
            
            item {
                ToggleSettingEntry(
                    title = stringResource(R.string.showvisthumbnail),
                    icon = R.drawable.equalizer,
                    isChecked = showvisthumbnail,
                    onCheckedChange = { showvisthumbnail = it }
                )
            }

            if (showvisthumbnail) {
                item {
                    ToggleSettingEntry(
                        title = stringResource(R.string.black_background_for_visualizer),
                        icon = R.drawable.images_sharp,
                        isChecked = blackBackgroundForVisThumbnail,
                        onCheckedChange = { blackBackgroundForVisThumbnail = it }
                    )
                }
            }

            item {
                ToggleSettingEntry(
                    title = stringResource(R.string.show_visualizer_buttons),
                    icon = R.drawable.menu,
                    isChecked = showVisualizerButtons,
                    onCheckedChange = { showVisualizerButtons = it }
                )
            }
            
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionTitle(stringResource(R.string.visualizer_line_thickness))
                }
            }
            
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (currentVisualizer == 0 || currentVisualizer == 1 || currentVisualizer == 21) 1f else 0.5f)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingIcon(R.drawable.sound_effect)
                    Spacer(modifier = Modifier.size(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        SliderControl(
                            state = visualizerLineThickness,
                            onSlide = { if (currentVisualizer == 0 || currentVisualizer == 1 || currentVisualizer == 21) visualizerLineThickness = it },
                            onSlideComplete = {},
                            toDisplay = { "%.0f".format(it) },
                            range = 1f..20f,
                            stepSize = 1f
                        )
                    }
                }
            }
            
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (menuStyle == MenuStyle.List) {
            ListMenu.Menu(title = stringResource(R.string.visualizer)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    settingsContent()
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        } else {
            GridMenu.Menu(title = stringResource(R.string.visualizer)) {
                settingsGridContent()
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }


    // Helper composables

    @Composable
    private fun SettingIcon(icon: Int) {
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
    private fun SectionTitle(title: String) {
        BasicText(
            text = title,
            style = typography().xxs.semiBold.copy(
                color = colorPalette().accent,
                textAlign = TextAlign.Start
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp)
        )
    }

    @Composable
    private fun ToggleSettingEntry(
        title: String,
        icon: Int,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        subtitle: String? = null
    ) {
        if (menuStyle == MenuStyle.List) {
            ListMenu.Entry(
                text = title,
                icon = { SettingIcon(icon) },
                subtitle = subtitle,
                onClick = { onCheckedChange(!isChecked) },
                trailingContent = {
                    Switch(
                        checked = isChecked,
                        onCheckedChange = null,
                        modifier = Modifier.scale(0.8f),
                        checkedThumbColor = colorPalette().textSecondary,
                        checkedTrackColor = colorPalette().accent.copy(alpha = 0.3f),
                        uncheckedThumbColor = colorPalette().textSecondary,
                        uncheckedTrackColor = colorPalette().textSecondary.copy(alpha = 0.3f)
                    )
                }
            )
        } else {
            GridMenu.Entry(
                text = title,
                icon = { SettingIcon(icon) },
                subtitle = subtitle,
                onClick = { onCheckedChange(!isChecked) },
                trailingContent = {
                    Switch(
                        checked = isChecked,
                        onCheckedChange = null,
                        modifier = Modifier.scale(0.8f),
                        checkedThumbColor = colorPalette().textSecondary,
                        checkedTrackColor = colorPalette().accent.copy(alpha = 0.3f),
                        uncheckedThumbColor = colorPalette().textSecondary,
                        uncheckedTrackColor = colorPalette().textSecondary.copy(alpha = 0.3f)
                    )
                }
            )
        }
    }

    @Composable
    private inline fun <reified T : Enum<T>> EnumSettingEntry(
        title: String,
        icon: Int,
        selectedValue: T,
        values: List<T>,
        noinline valueText: @Composable (T) -> String,
        noinline onValueSelected: (T) -> Unit,
        noinline trailingContent: @Composable () -> Unit = {
            Icon(
                painter = painterResource(R.drawable.chevron_forward),
                tint = colorPalette().textSecondary,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    ) {
        var isShowingDialog by remember { mutableStateOf(false) }

        if (isShowingDialog) {
            ValueSelectorDialog(
                onDismiss = { isShowingDialog = false },
                title = title,
                selectedValue = selectedValue,
                values = values,
                onValueSelected = {
                    onValueSelected(it)
                    isShowingDialog = false
                },
                valueText = valueText
            )
        }

        if (menuStyle == MenuStyle.List) {
            ListMenu.Entry(
                text = title,
                icon = { SettingIcon(icon) },
                subtitle = valueText(selectedValue),
                onClick = { isShowingDialog = true },
                trailingContent = trailingContent
            )
        } else {
            GridMenu.Entry(
                text = title,
                icon = { SettingIcon(icon) },
                subtitle = valueText(selectedValue),
                onClick = { isShowingDialog = true },
                trailingContent = trailingContent
            )
        }
    }
}
