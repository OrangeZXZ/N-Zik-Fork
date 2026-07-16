package app.n_zik.android.components.dialog.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.it.fast4x.rimusic.utils.showButtonPlayerAddToPlaylistKey
import app.it.fast4x.rimusic.utils.showButtonPlayerArrowKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDiscoverKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDownloadKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLoopKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLyricsKey
import app.it.fast4x.rimusic.utils.showButtonPlayerMenuKey
import app.it.fast4x.rimusic.utils.showButtonPlayerShuffleKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSleepTimerKey
import app.it.fast4x.rimusic.utils.showButtonPlayerStartRadioKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSystemEqualizerKey
import app.it.fast4x.rimusic.utils.showButtonPlayerVideoKey
import app.it.fast4x.rimusic.utils.expandedplayertoggleKey
import app.it.fast4x.rimusic.utils.visualizerEnabledKey
import app.it.fast4x.rimusic.utils.playerActionBarButtonOrderKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.ToggleItem
import app.n_zik.android.components.dialog.common.ToggleListDialog
import app.kreate.android.me.knighthat.utils.Toaster
import org.json.JSONArray
import sh.calvin.reorderable.rememberReorderableLazyListState

private val defaultButtonOrder = listOf(
    "video", "discover", "download", "add_to_playlist",
    "loop", "shuffle", "lyrics", "visualizer", "expanded_player",
    "sleep_timer", "equalizer", "arrow", "start_radio", "menu"
)

private data class ButtonDef(
    val id: String,
    val preferenceKey: String,
    val iconRes: Int,
    val labelRes: Int,
    val defaultValue: Boolean
)

private fun buildButtonDefs(): Map<String, ButtonDef> = mapOf(
    "video" to ButtonDef("video", showButtonPlayerVideoKey, R.drawable.video, R.string.action_bar_show_video_button, false),
    "discover" to ButtonDef("discover", showButtonPlayerDiscoverKey, R.drawable.discover, R.string.action_bar_show_discover_button, false),
    "download" to ButtonDef("download", showButtonPlayerDownloadKey, R.drawable.download, R.string.action_bar_show_download_button, true),
    "add_to_playlist" to ButtonDef("add_to_playlist", showButtonPlayerAddToPlaylistKey, R.drawable.add_in_playlist, R.string.action_bar_show_add_to_playlist_button, true),
    "loop" to ButtonDef("loop", showButtonPlayerLoopKey, R.drawable.repeat, R.string.action_bar_show_loop_button, true),
    "shuffle" to ButtonDef("shuffle", showButtonPlayerShuffleKey, R.drawable.shuffle, R.string.action_bar_show_shuffle_button, true),
    "lyrics" to ButtonDef("lyrics", showButtonPlayerLyricsKey, R.drawable.song_lyrics, R.string.action_bar_show_lyrics_button, true),
    "visualizer" to ButtonDef("visualizer", visualizerEnabledKey, R.drawable.sound_effect, R.string.txt_visualizerbackground, false),
    "expanded_player" to ButtonDef("expanded_player", expandedplayertoggleKey, R.drawable.maximize, R.string.expandedplayer, true),
    "sleep_timer" to ButtonDef("sleep_timer", showButtonPlayerSleepTimerKey, R.drawable.sleep, R.string.action_bar_show_sleep_timer_button, false),
    "equalizer" to ButtonDef("equalizer", showButtonPlayerSystemEqualizerKey, R.drawable.equalizer, R.string.show_equalizer, false),
    "arrow" to ButtonDef("arrow", showButtonPlayerArrowKey, R.drawable.chevron_up, R.string.action_bar_show_arrow_button_to_open_queue, true),
    "start_radio" to ButtonDef("start_radio", showButtonPlayerStartRadioKey, R.drawable.radio, R.string.action_bar_show_start_radio_button, false),
    "menu" to ButtonDef("menu", showButtonPlayerMenuKey, R.drawable.ellipsis_vertical, R.string.action_bar_show_menu_button, false)
)

object PlayerActionBarSettingsDialog : Dialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.player_action_bar_buttons)

    override var isActive: Boolean by mutableStateOf(false)

    private fun parseOrder(serialized: String): List<String> {
        if (serialized.isBlank()) return defaultButtonOrder
        return try {
            val arr = JSONArray(serialized)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            val validIds = defaultButtonOrder.filter { it in buildButtonDefs().keys }
            val result = list.filter { it in validIds }.toMutableList()
            for (id in validIds) {
                if (id !in result) result.add(id)
            }
            result
        } catch (_: Exception) {
            defaultButtonOrder
        }
    }

    private fun serializeOrder(order: List<String>): String {
        val arr = JSONArray()
        order.forEach { arr.put(it) }
        return arr.toString()
    }

    @Composable
    override fun DialogBody() {
        val buttonDefs = remember { buildButtonDefs() }
        var orderSerialized by rememberPreference(playerActionBarButtonOrderKey, "")
        val currentOrder = remember(orderSerialized) { parseOrder(orderSerialized) }
        val buttonOrder = remember { mutableStateListOf<String>().apply { addAll(currentOrder) } }

        val lazyListState = rememberLazyListState()

        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = buttonOrder.indexOf(from.key)
            val toIndex = buttonOrder.indexOf(to.key)
            if (fromIndex != -1 && toIndex != -1) {
                val item = buttonOrder.removeAt(fromIndex)
                buttonOrder.add(toIndex, item)
                orderSerialized = serializeOrder(buttonOrder.toList())
            }
        }

        val items = buttonOrder.mapNotNull { buttonId ->
            val def = buttonDefs[buttonId] ?: return@mapNotNull null
            ToggleItem(
                id = def.id,
                iconRes = def.iconRes,
                label = stringResource(def.labelRes),
                preferenceKey = def.preferenceKey,
                defaultValue = def.defaultValue
            )
        }

        ToggleListDialog(
            items = items,
            lazyListState = lazyListState,
            reorderableState = reorderableState,
            onReset = {
                buttonOrder.clear()
                buttonOrder.addAll(defaultButtonOrder)
                orderSerialized = serializeOrder(defaultButtonOrder)
            },
            onCancel = {
                hideDialog()
            },
            onConfirm = {
                hideDialog()
            }
        )
    }
}
