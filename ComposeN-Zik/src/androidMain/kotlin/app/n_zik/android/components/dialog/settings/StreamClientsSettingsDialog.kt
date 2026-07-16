package app.n_zik.android.components.dialog.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.ToggleItem
import app.n_zik.android.components.dialog.common.ToggleListDialog
import app.it.fast4x.rimusic.utils.streamClientWebRemixEnabledKey
import app.it.fast4x.rimusic.utils.streamClientVisionosEnabledKey
import app.it.fast4x.rimusic.utils.streamClientTvEmbeddedEnabledKey
import app.it.fast4x.rimusic.utils.streamClientTvHtml5EnabledKey
import app.it.fast4x.rimusic.utils.streamClientAndroidVrEnabledKey
import app.it.fast4x.rimusic.utils.streamClientAndroidCreatorEnabledKey
import app.it.fast4x.rimusic.utils.streamClientIosEnabledKey
import app.it.fast4x.rimusic.utils.streamClientIpadosEnabledKey
import app.it.fast4x.rimusic.utils.streamClientWebEnabledKey
import app.it.fast4x.rimusic.utils.streamClientWebCreatorEnabledKey
import app.it.fast4x.rimusic.utils.streamClientMobileEnabledKey
import app.it.fast4x.rimusic.utils.streamClientAndroidEnabledKey
import app.it.fast4x.rimusic.utils.streamClientRestartNeededKey
import app.it.fast4x.rimusic.utils.streamClientsOrderKey
import app.n_zik.android.playback.services.clearStreamCaches
import app.kreate.android.me.knighthat.utils.Toaster
import org.json.JSONArray
import sh.calvin.reorderable.rememberReorderableLazyListState

private val defaultClientOrder = listOf(
    "web_remix",
    "android_vr",
    "visionos",
    "tv_embedded",
    "tv_html5",
    "android_creator",
    "android",
    "ios",
    "ipados",
    "web",
    "web_creator",
    "mobile"
)

private val clientKeys = listOf(
    streamClientWebRemixEnabledKey,
    streamClientVisionosEnabledKey,
    streamClientTvEmbeddedEnabledKey,
    streamClientTvHtml5EnabledKey,
    streamClientAndroidVrEnabledKey,
    streamClientAndroidCreatorEnabledKey,
    streamClientAndroidEnabledKey,
    streamClientIosEnabledKey,
    streamClientIpadosEnabledKey,
    streamClientWebEnabledKey,
    streamClientWebCreatorEnabledKey,
    streamClientMobileEnabledKey
)

private data class ClientDef(
    val id: String,
    val preferenceKey: String,
    val iconRes: Int,
    val labelRes: Int,
    val defaultValue: Boolean,
    val descriptionRes: Int? = null
)

private fun buildClientDefs(): Map<String, ClientDef> = mapOf(
    "web_remix" to ClientDef("web_remix", streamClientWebRemixEnabledKey, R.drawable.ytmusic, R.string.stream_client_web_remix, true, R.string.client_youtube_music_web_desc),
    "android_vr" to ClientDef("android_vr", streamClientAndroidVrEnabledKey, R.drawable.musical_notes, R.string.stream_client_android_vr, true, R.string.client_android_vr_desc),
    "visionos" to ClientDef("visionos", streamClientVisionosEnabledKey, R.drawable.musical_notes, R.string.stream_client_visionos, true),
    "tv_embedded" to ClientDef("tv_embedded", streamClientTvEmbeddedEnabledKey, R.drawable.video, R.string.stream_client_tv_embedded, true),
    "tv_html5" to ClientDef("tv_html5", streamClientTvHtml5EnabledKey, R.drawable.video, R.string.stream_client_tv_html5, true),
    "android_creator" to ClientDef("android_creator", streamClientAndroidCreatorEnabledKey, R.drawable.musical_notes, R.string.stream_client_android_creator, true),
    "android" to ClientDef("android", streamClientAndroidEnabledKey, R.drawable.musical_notes, R.string.stream_client_android, true),
    "ios" to ClientDef("ios", streamClientIosEnabledKey, R.drawable.musical_notes, R.string.stream_client_ios, true),
    "ipados" to ClientDef("ipados", streamClientIpadosEnabledKey, R.drawable.musical_notes, R.string.stream_client_ipados, true),
    "web" to ClientDef("web", streamClientWebEnabledKey, R.drawable.musical_notes, R.string.stream_client_web, true),
    "web_creator" to ClientDef("web_creator", streamClientWebCreatorEnabledKey, R.drawable.musical_notes, R.string.stream_client_web_creator, true),
    "mobile" to ClientDef("mobile", streamClientMobileEnabledKey, R.drawable.musical_notes, R.string.stream_client_mobile, true)
)

object StreamClientsSettingsDialog : Dialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.disabled_stream_clients_title)

    override var isActive: Boolean by mutableStateOf(false)

    private fun parseOrder(serialized: String): List<String> {
        if (serialized.isBlank()) return defaultClientOrder
        return try {
            val arr = JSONArray(serialized)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            val validIds = defaultClientOrder.filter { it in buildClientDefs().keys }
            val result = list.filter { it in validIds }.toMutableList()
            for (id in validIds) {
                if (id !in result) result.add(id)
            }
            result
        } catch (_: Exception) {
            defaultClientOrder
        }
    }

    private fun serializeOrder(order: List<String>): String {
        val arr = JSONArray()
        order.forEach { arr.put(it) }
        return arr.toString()
    }

    @Composable
    override fun DialogBody() {
        val context = androidx.compose.ui.platform.LocalContext.current
        val binder = LocalPlayerServiceBinder.current
        val prefs = remember { context.getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE) }
        val clientDefs = remember { buildClientDefs() }

        val savedStates = remember {
            clientKeys.associateWith { prefs.getBoolean(it, true) }
        }

        val orderSerialized = remember { mutableStateOf(prefs.getString(streamClientsOrderKey, "") ?: "") }
        val currentOrder = remember(orderSerialized.value) { parseOrder(orderSerialized.value) }
        var workingOrder by remember { mutableStateOf(currentOrder.toMutableList()) }

        val lazyListState = rememberLazyListState()

        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = workingOrder.indexOf(from.key)
            val toIndex = workingOrder.indexOf(to.key)
            if (fromIndex != -1 && toIndex != -1) {
                val item = workingOrder.removeAt(fromIndex)
                workingOrder.add(toIndex, item)
                orderSerialized.value = serializeOrder(workingOrder)
            }
        }

        val items = workingOrder.mapNotNull { clientId ->
            val def = clientDefs[clientId] ?: return@mapNotNull null
            ToggleItem(
                id = def.id,
                iconRes = def.iconRes,
                label = stringResource(def.labelRes),
                preferenceKey = def.preferenceKey,
                defaultValue = def.defaultValue,
                description = def.descriptionRes?.let { stringResource(it) }
            )
        }

        ToggleListDialog(
            items = items,
            lazyListState = lazyListState,
            reorderableState = reorderableState,
            enforceMinOneChecked = true,
            onReset = {
                clientKeys.forEach { key ->
                    prefs.edit().putBoolean(key, true).apply()
                }
            },
            onCancel = {
                savedStates.forEach { (key, value) ->
                    prefs.edit().putBoolean(key, value).apply()
                }
                hideDialog()
            },
            onConfirm = {
                val hasChanges = clientKeys.any { key ->
                    prefs.getBoolean(key, true) != savedStates[key]
                }
                if (hasChanges) {
                    prefs.edit().putBoolean(streamClientRestartNeededKey, true).apply()
                    clearStreamCaches()
                    binder?.cache?.let { cache ->
                        cache.keys.forEach { song -> cache.removeResource(song) }
                    }
                    Toaster.i(R.string.preferred_stream_client_changed)
                    Toaster.w(R.string.stream_client_redownload_recommendation)
                }
                hideDialog()
            }
        )
    }
}
