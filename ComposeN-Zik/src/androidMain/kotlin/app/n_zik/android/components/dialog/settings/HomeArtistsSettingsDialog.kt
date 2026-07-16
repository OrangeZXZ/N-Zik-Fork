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
import app.it.fast4x.rimusic.utils.showFavoritesArtistKey
import app.it.fast4x.rimusic.utils.homeArtistsOrderKey
import app.kreate.android.me.knighthat.utils.Toaster
import org.json.JSONArray
import sh.calvin.reorderable.rememberReorderableLazyListState

private val artistsDefaultOrder = listOf("all", "favorites")

object HomeArtistsSettingsDialog : Dialog {
    override val dialogTitle: String @Composable get() = stringResource(R.string.home_artists_settings)
    override var isActive: Boolean by mutableStateOf(false)

    private fun parseOrder(s: String): List<String> {
        if (s.isBlank()) return artistsDefaultOrder
        return try { val a = JSONArray(s); val l = mutableListOf<String>(); for (i in 0 until a.length()) l.add(a.getString(i)); val r = l.filter { it in artistsDefaultOrder }.toMutableList(); for (id in artistsDefaultOrder) { if (id !in r) r.add(id) }; r } catch (_: Exception) { artistsDefaultOrder }
    }
    private fun serializeOrder(order: List<String>): String { val a = JSONArray(); order.forEach { a.put(it) }; return a.toString() }

    @Composable
    override fun DialogBody() {
        val ctx = LocalContext.current
        val prefs = remember { ctx.getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE) }
        var workingOrder by remember { mutableStateOf(parseOrder(prefs.getString(homeArtistsOrderKey, "") ?: "").toMutableList()) }

        val allLabel = stringResource(R.string.all)
        val favLabel = stringResource(R.string.favorites)

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val o = workingOrder.toMutableList(); val fi = o.indexOf(from.key); val ti = o.indexOf(to.key)
            if (fi != -1 && ti != -1) { val item = o.removeAt(fi); o.add(ti, item); workingOrder = o }
        }

        val items = workingOrder.map { id ->
            when (id) {
                "all" -> ToggleItem(id, R.drawable.people, allLabel, "always_true_artists", true)
                "favorites" -> ToggleItem(id, R.drawable.heart, favLabel, showFavoritesArtistKey, true)
                else -> null
            }
        }.filterNotNull()

        ToggleListDialog(
            items = items, lazyListState = lazyListState, reorderableState = reorderableState,
            enforceMinOneChecked = true,
            lockedCheckedIds = setOf("all"),
            onReset = {
                workingOrder = artistsDefaultOrder.toMutableList()
                prefs.edit().putBoolean(showFavoritesArtistKey, true).apply()
            },
            onCancel = { hideDialog() },
            onConfirm = {
                prefs.edit().putString(homeArtistsOrderKey, serializeOrder(workingOrder)).apply()
                Toaster.s(R.string.toast_preference_saved); hideDialog()
            }
        )
    }
}
