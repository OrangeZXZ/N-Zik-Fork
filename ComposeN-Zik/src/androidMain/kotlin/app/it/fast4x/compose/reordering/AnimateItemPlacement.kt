package app.it.fast4x.compose.reordering

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier

context(scope: LazyItemScope)
@ExperimentalFoundationApi
fun Modifier.localAnimateItemPlacement(reorderingState: ReorderingState) =
    if (reorderingState.draggingIndex == -1) {
        with(scope) { animateItem() }
    } else this



