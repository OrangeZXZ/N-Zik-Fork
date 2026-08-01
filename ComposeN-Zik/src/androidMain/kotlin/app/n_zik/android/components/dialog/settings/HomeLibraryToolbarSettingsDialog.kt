package app.n_zik.android.components.dialog.settings

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import app.n_zik.android.R
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.ToggleItem
import app.n_zik.android.components.dialog.common.ToggleListDialog
import app.it.fast4x.rimusic.utils.*
import app.kreate.android.me.knighthat.utils.Toaster
import org.json.JSONArray
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.unit.dp

private val allButtonIds = listOf(
    "sort", "position_lock", "sync", "search", "shuffle",
    "new_playlist_dialog", "import_menu", "item_size"
)

private val lockedIds = setOf("sort", "sync", "position_lock")

private fun getTabPrefix(tab: String): String = when (tab) {
    "pin" -> "pin"
    "mon" -> "mon"
    "yt" -> "yt"
    else -> "all"
}

object HomeLibraryToolbarSettingsDialog : Dialog {
    override val dialogTitle: String @Composable get() = stringResource(R.string.library) + " - Toolbar"
    override var isActive: Boolean by mutableStateOf(false)

    private fun parseOrder(s: String): List<String> {
        if (s.isBlank()) return allButtonIds
        return try {
            val a = JSONArray(s)
            val savedIds = mutableListOf<String>()
            val seen = mutableSetOf<String>()
            for (i in 0 until a.length()) {
                val id = a.getString(i)
                if (id in allButtonIds && seen.add(id)) savedIds.add(id)
            }
            val missingIds = allButtonIds.filter { it !in savedIds }
            savedIds + missingIds
        } catch (_: Exception) { allButtonIds }
    }

    private fun serializeOrder(order: List<String>): String { val a = JSONArray(); order.forEach { a.put(it) }; return a.toString() }

    @Composable
    override fun DialogBody() {
        val ctx = LocalContext.current
        val prefs = remember { ctx.getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE) }

        val tabs = listOf(
            "all" to homeLibraryToolbarOrderKey,
            "pin" to homeLibraryPinnedPlaylistToolbarOrderKey,
            "mon" to homeLibraryMonthlyPlaylistToolbarOrderKey,
            "yt" to homeLibraryYTPlaylistToolbarOrderKey
        )

        var selectedTabIndex by remember { mutableStateOf(0) }

        var workingOrders by remember {
            mutableStateOf(
                tabs.associate { (tab, key) -> tab to parseOrder(prefs.getString(key, "") ?: "").toMutableList() }.toMutableMap()
            )
        }

        val currentTabKey = tabs[selectedTabIndex].first
        val currentWorkingOrder = workingOrders[currentTabKey]!!
        val tabPrefix = getTabPrefix(currentTabKey)

        val sortLabel = stringResource(R.string.sorting_order)
        val positionLockLabel = stringResource(R.string.info_lock_unlock_reorder_songs)
        val syncLabel = stringResource(R.string.autosync)
        val searchLabel = stringResource(R.string.search)
        val shuffleLabel = stringResource(R.string.info_shuffle)
        val newPlaylistLabel = stringResource(R.string.create_new_playlist)
        val importMenuLabel = stringResource(R.string.import_playlist)
        val itemSizeLabel = stringResource(R.string.size)

        val currentLockedIds = lockedIds.map { "${tabPrefix}_$it" }.toSet()

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val o = currentWorkingOrder.toMutableList()
            val fi = o.indexOfFirst { "${tabPrefix}_$it" == from.key }
            val ti = o.indexOfFirst { "${tabPrefix}_$it" == to.key }
            if (fi != -1 && ti != -1) {
                val item = o.removeAt(fi)
                o.add(ti, item)
                workingOrders = workingOrders.toMutableMap().apply { this[currentTabKey] = o }
            }
        }

        val items = currentWorkingOrder.distinct().map { id ->
            val pk = "${tabPrefix}_lib_$id"
            val uid = "${tabPrefix}_$id"
            when (id) {
                "sort" -> ToggleItem(uid, R.drawable.arrow_up, sortLabel, pk, true)
                "position_lock" -> ToggleItem(uid, R.drawable.locked, positionLockLabel, pk, true)
                "sync" -> ToggleItem(uid, R.drawable.sync, syncLabel, pk, true)
                "search" -> ToggleItem(uid, R.drawable.search_circle, searchLabel, pk, true)
                "shuffle" -> ToggleItem(uid, R.drawable.shuffle, shuffleLabel, pk, true)
                "new_playlist_dialog" -> ToggleItem(uid, R.drawable.add_in_playlist, newPlaylistLabel, pk, true)
                "import_menu" -> ToggleItem(uid, R.drawable.import_outline, importMenuLabel, pk, true)
                "item_size" -> ToggleItem(uid, R.drawable.resize, itemSizeLabel, pk, true)
                else -> null
            }
        }.filterNotNull()

        Column {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                divider = {},
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(stringResource(when (tab.first) {
                                "pin" -> R.string.pinned_playlists
                                "mon" -> R.string.monthly_playlists
                                "yt" -> R.string.yt_playlists
                                else -> R.string.all
                            }))
                        }
                    )
                }
            }

            ToggleListDialog(
                items = items, lazyListState = lazyListState, reorderableState = reorderableState,
                enforceMinOneChecked = false,
                lockedCheckedIds = currentLockedIds,
                onReset = {
                    val edit = prefs.edit()
                    for (id in allButtonIds) {
                        edit.putBoolean("${tabPrefix}_lib_$id", true)
                    }
                    edit.apply()
                    workingOrders = workingOrders.toMutableMap().apply { this[currentTabKey] = allButtonIds.toMutableList() }
                },
                onCancel = { hideDialog() },
                onConfirm = {
                    val edit = prefs.edit()
                    tabs.forEach { (tab, key) ->
                        val tp = getTabPrefix(tab)
                        val order = workingOrders[tab]!!
                        val finalOrder = order.filter { id ->
                            prefs.getBoolean("${tp}_lib_$id", true) || id in lockedIds
                        }
                        edit.putString(key, serializeOrder(finalOrder))
                    }
                    edit.apply()
                    Toaster.s(R.string.toast_preference_saved); hideDialog()
                }
            )
        }
    }
}
