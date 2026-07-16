package app.n_zik.android.components.dialog.settings

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import app.n_zik.android.R
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.ToggleItem
import app.n_zik.android.components.dialog.common.ToggleListDialog
import app.it.fast4x.rimusic.utils.showFavoritesPlaylistKey
import app.it.fast4x.rimusic.utils.showCachedPlaylistKey
import app.it.fast4x.rimusic.utils.showMyTopPlaylistKey
import app.it.fast4x.rimusic.utils.showDownloadedPlaylistKey
import app.it.fast4x.rimusic.utils.showOnDevicePlaylistKey
import app.it.fast4x.rimusic.utils.homeSongsOrderKey
import app.kreate.android.me.knighthat.utils.Toaster
import org.json.JSONArray
import sh.calvin.reorderable.rememberReorderableLazyListState

private val songsDefaultOrder = listOf("all", "favorites", "cached", "downloaded", "top", "on_device")

object HomeSongsSettingsDialog : Dialog {
    override val dialogTitle: String @Composable get() = stringResource(R.string.home_songs_settings)
    override var isActive: Boolean by mutableStateOf(false)

    private fun parseOrder(s: String): List<String> {
        if (s.isBlank()) return songsDefaultOrder
        return try { val a = JSONArray(s); val l = mutableListOf<String>(); for (i in 0 until a.length()) l.add(a.getString(i)); val v = songsDefaultOrder; val r = l.filter { it in v }.toMutableList(); for (id in v) { if (id !in r) r.add(id) }; r } catch (_: Exception) { songsDefaultOrder }
    }
    private fun serializeOrder(order: List<String>): String { val a = JSONArray(); order.forEach { a.put(it) }; return a.toString() }

    @Composable
    override fun DialogBody() {
        val ctx = LocalContext.current
        val prefs = remember { ctx.getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE) }
        var workingOrder by remember { mutableStateOf(parseOrder(prefs.getString(homeSongsOrderKey, "") ?: "").toMutableList()) }

        val allLabel = stringResource(R.string.all)
        val favoritesLabel = stringResource(R.string.favorites)
        val cachedLabel = stringResource(R.string.cached)
        val downloadedLabel = stringResource(R.string.downloaded)
        val topLabel = stringResource(R.string.playlist_top)
        val onDeviceLabel = stringResource(R.string.on_device)

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val o = workingOrder.toMutableList(); val fi = o.indexOf(from.key); val ti = o.indexOf(to.key)
            if (fi != -1 && ti != -1) { val item = o.removeAt(fi); o.add(ti, item); workingOrder = o }
        }

        val items = workingOrder.map { id ->
            when (id) {
                "all" -> ToggleItem(id, R.drawable.musical_notes, allLabel, "always_true_songs", true)
                "favorites" -> ToggleItem(id, R.drawable.heart, favoritesLabel, showFavoritesPlaylistKey, true)
                "cached" -> ToggleItem(id, R.drawable.server, cachedLabel, showCachedPlaylistKey, true)
                "downloaded" -> ToggleItem(id, R.drawable.downloaded, downloadedLabel, showDownloadedPlaylistKey, true)
                "top" -> ToggleItem(id, R.drawable.trending, topLabel, showMyTopPlaylistKey, true)
                "on_device" -> ToggleItem(id, R.drawable.folder, onDeviceLabel, showOnDevicePlaylistKey, true)
                else -> null
            }
        }.filterNotNull()

        ToggleListDialog(
            items = items, lazyListState = lazyListState, reorderableState = reorderableState,
            enforceMinOneChecked = true,
            lockedCheckedIds = setOf("all"),
            onReset = {
                workingOrder = songsDefaultOrder.toMutableList()
                prefs.edit()
                    .putBoolean(showFavoritesPlaylistKey, true)
                    .putBoolean(showCachedPlaylistKey, true)
                    .putBoolean(showDownloadedPlaylistKey, true)
                    .putBoolean(showMyTopPlaylistKey, true)
                    .putBoolean(showOnDevicePlaylistKey, true)
                    .apply()
            },
            onCancel = { hideDialog() },
            onConfirm = {
                prefs.edit().putString(homeSongsOrderKey, serializeOrder(workingOrder)).apply()
                Toaster.s(R.string.toast_preference_saved); hideDialog()
            }
        )
    }
}
