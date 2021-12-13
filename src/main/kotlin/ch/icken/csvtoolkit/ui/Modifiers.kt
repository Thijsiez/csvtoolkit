package ch.icken.csvtoolkit.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import org.burnoutcrew.reorderable.ReorderableState
import org.burnoutcrew.reorderable.detectReorderAfterLongPress

fun Modifier.reorderableItemModifier(state: ReorderableState, key: Any) =
    composed {
        Modifier.zIndex(1f)
            .graphicsLayer {
                translationY = state.offsetByKey(key) ?: 0f
            }
    }.detectReorderAfterLongPress(state)