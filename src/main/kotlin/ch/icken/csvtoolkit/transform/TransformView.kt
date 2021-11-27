package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.ui.Tooltip
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.move
import org.burnoutcrew.reorderable.rememberReorderState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun TransformView(
    instance: ToolkitInstance,
    onAddTransform: (Transform) -> Unit,
    onEditTransform: (Transform) -> Unit
) = Card {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        var expanded by remember { mutableStateOf(false) }
        val reorderState = rememberReorderState()

        Row(
            modifier = Modifier.fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transforms",
                style = MaterialTheme.typography.h6
            )
            Box {
                IconButton(
                    onClick = { expanded = true },
                    enabled = instance.files.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Transform"
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Transform.Type.values().forEach {
                        DropdownMenuItem(
                            onClick = {
                                val transform = Transform.create(it)
                                onAddTransform(transform)
                                onEditTransform(transform)
                                expanded = false
                            }
                        ) {
                            Text(it.uiName)
                        }
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .reorderable(
                    state = reorderState,
                    onMove = { from, to ->
                        instance.transforms.move(from.index, to.index)
                    }
                ),
            state = reorderState.listState
        ) {
            items(instance.transforms, { it }) {
                TransformItemView(
                    instance = instance,
                    transform = it,
                    onEditTransform = { onEditTransform(it) },
                    modifier = Modifier
                        .composed {
                            //Drag effect based off draggedItem()
                            Modifier.zIndex(1f)
                                .graphicsLayer {
                                    translationY = reorderState.offsetByKey(it) ?: 0f
                                }
                        }
                        .detectReorderAfterLongPress(reorderState)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransformItemView(
    instance: ToolkitInstance,
    transform: Transform,
    onEditTransform: () -> Unit,
    modifier: Modifier = Modifier
) = Row(
    modifier = modifier
        .combinedClickable(
            onClick = onEditTransform
        )
        .fillMaxWidth()
        .height(48.dp)
        .padding(start = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = transform.description,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.body1
    )
    if (transform == instance.currentlyProcessingTransform) {
        CircularProgressIndicator(Modifier.padding(4.dp))
    } else {
        Row(
            modifier = Modifier.requiredSize(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                transform.isValid(instance) -> {
                    TooltipArea(
                        tooltip = { Tooltip("Transform is valid") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Transform is valid",
                            tint = Color.Green
                        )
                    }
                }
                else -> {
                    TooltipArea(
                        tooltip = { Tooltip(transform.invalidMessage) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Transform is invalid",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}