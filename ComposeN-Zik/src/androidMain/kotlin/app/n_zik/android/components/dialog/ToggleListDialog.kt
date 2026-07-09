package app.n_zik.android.components.dialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

data class ToggleItem(
    val id: String,
    val iconRes: Int,
    val label: String,
    val preferenceKey: String,
    val defaultValue: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToggleListDialog(
    items: List<ToggleItem>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    reorderableState: ReorderableLazyListState? = null,
    contentHeight: Dp = 480.dp
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(contentHeight),
        state = lazyListState
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { index, item ->
            val isChecked by rememberPreference(item.preferenceKey, item.defaultValue)

            if (reorderableState != null) {
                ReorderableItem(reorderableState, key = item.id) { isDragging ->
                    ToggleRow(
                        icon = item.iconRes,
                        label = item.label,
                        isChecked = isChecked,
                        onCheckedChange = null,
                        preferenceKey = item.preferenceKey,
                        showReorder = true,
                        isDragging = isDragging,
                        dragModifier = Modifier.draggableHandle()
                    )
                }
            } else {
                ToggleRow(
                    icon = item.iconRes,
                    label = item.label,
                    isChecked = isChecked,
                    onCheckedChange = null,
                    preferenceKey = item.preferenceKey,
                    showReorder = false,
                    isDragging = false,
                    dragModifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: Int,
    label: String,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    preferenceKey: String,
    showReorder: Boolean,
    isDragging: Boolean,
    dragModifier: Modifier
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(uiRoundnessShape())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    val prefs = ctx.getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(preferenceKey, !isChecked).apply()
                }
            )
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colorPalette().accent,
            modifier = Modifier.size(22.dp)
        )

        BasicText(
            text = label,
            style = typography().xs.semiBold.copy(
                color = if (isDragging) colorPalette().accent else colorPalette().text
            ),
            modifier = Modifier.weight(1f)
        )

        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = colorPalette().accent,
                uncheckedColor = colorPalette().textDisabled,
                checkmarkColor = colorPalette().onAccent,
                disabledIndeterminateColor = Color.Transparent
            )
        )

        if (showReorder) {
            Icon(
                painter = painterResource(app.n_zik.android.R.drawable.reorder),
                contentDescription = null,
                tint = if (isDragging) colorPalette().accent else colorPalette().textDisabled,
                modifier = Modifier
                    .size(16.dp)
                    .then(dragModifier)
            )
        }
    }
}
