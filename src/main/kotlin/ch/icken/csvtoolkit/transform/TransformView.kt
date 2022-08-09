package ch.icken.csvtoolkit.transform

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.move
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.aggregate.Aggregate
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.ui.Tooltip
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransformView(
    instance: ToolkitInstance,
    onAddTransform: (Transform) -> Unit,
    onEditTransform: (Transform) -> Unit,
    onEditAggregate: (Aggregate) -> Unit,
    onEditCondition: (Condition) -> Unit
) = Card {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        var expanded by remember { mutableStateOf(false) }
        val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
            instance.transforms.move(from.index, to.index)
        })

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
            Row {
                when {
                    instance.isDoingTheThing -> {
                        CircularProgressIndicator(Modifier.padding(4.dp))
                    }
                    instance.allowDoingTheThing -> {
                        IconButton(
                            onClick = { instance.theThing() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Do the thing"
                            )
                        }
                    }
                }
                Box {
                    IconButton(
                        onClick = { expanded = true },
                        enabled = !instance.isDoingTheThing && instance.files.isNotEmpty()
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
                                    val transform = it.create(null)
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
        }
        Box {
            LazyColumn(
                modifier = Modifier.reorderable(reorderState)
                    .detectReorderAfterLongPress(reorderState),
                state = reorderState.listState
            ) {
                items(instance.transforms, { it }) {
                    ReorderableItem(reorderState, key = it) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                        TooltipArea(
                            tooltip = {
                                it.lastRunStats?.let { stats ->
                                    Tooltip(stats.text)
                                }
                            }
                        ) {
                            TransformItemView(
                                instance = instance,
                                transform = it,
                                onEditTransform = onEditTransform,
                                onEditAggregate = onEditAggregate,
                                onEditCondition = onEditCondition,
                                modifier = Modifier.shadow(elevation.value)
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(reorderState.listState),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
fun TransformItemView(
    instance: ToolkitInstance,
    transform: Transform,
    onEditTransform: (Transform) -> Unit,
    onEditAggregate: (Aggregate) -> Unit = {},
    onEditCondition: (Condition) -> Unit = {},
    modifier: Modifier = Modifier,
    stateContent: @Composable RowScope.() -> Unit = {
        when {
            transform is TransformCustomStateContent -> {
                transform.CustomStateContent(
                    instance = instance
                )
            }
            transform is ConditionalTransform && transform.parent != null -> {
                DefaultConditionalTransformStateContent(
                    instance = instance,
                    transform = transform
                )
            }
            else -> {
                DefaultTransformStateContent(
                    instance = instance,
                    transform = transform
                )
            }
        }
    }
) {
    if (transform is TransformCustomItemView) {
        transform.CustomItemView(
            instance = instance,
            onEditTransform = onEditTransform,
            onEditAggregate = onEditAggregate,
            onEditCondition = onEditCondition,
            modifier = modifier
        )
    } else {
        TransformDefaultItemView(
            instance = instance,
            transform = transform,
            onEditTransform = onEditTransform,
            modifier = modifier,
            stateContent = stateContent
        )
    }
}

@Composable
private fun TransformDefaultItemView(
    instance: ToolkitInstance,
    transform: Transform,
    onEditTransform: (Transform) -> Unit,
    modifier: Modifier,
    stateContent: @Composable RowScope.() -> Unit
) = Row(
    modifier = modifier
        .clickable(
            enabled = !instance.isDoingTheThing,
            onClick = { onEditTransform(transform) }
        )
        .fillMaxWidth()
        .padding(start = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = transform.description,
        modifier = Modifier.weight(1f)
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.body1
    )
    if (transform == instance.currentlyProcessingTransform) {
        CircularProgressIndicator(Modifier.padding(4.dp))
    } else {
        Row(
            modifier = Modifier.requiredHeight(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = stateContent
        )
    }
}

@Composable
fun DefaultTransformStateContent(
    instance: ToolkitInstance,
    transform: Transform
) {
    when {
        transform.isValid(instance) -> TransformValidIcon()
        else -> TransformInvalidIcon(transform.invalidMessage)
    }
}
@Composable
fun DefaultConditionalTransformStateContent(
    instance: ToolkitInstance,
    transform: ConditionalTransform
) {
    when {
        transform.parent == null -> TransformInvalidIcon("Transform is malformed")
        transform.isValidConditional(transform.parent.getConditionContext(instance)) -> TransformValidIcon()
        else -> TransformInvalidIcon(transform.invalidMessage)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransformValidIcon() {
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransformWarningIcon(message: String) {
    TooltipArea(
        tooltip = { Tooltip(message) }
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = message,
            tint = Color.Yellow
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransformInvalidIcon(message: String) {
    TooltipArea(
        tooltip = { Tooltip(message) }
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = message,
            tint = Color.Red
        )
    }
}

interface TransformCustomItemView {
    @Composable
    fun CustomItemView(
        instance: ToolkitInstance,
        onEditTransform: (Transform) -> Unit,
        onEditAggregate: (Aggregate) -> Unit,
        onEditCondition: (Condition) -> Unit,
        modifier: Modifier
    )
}
interface TransformCustomStateContent {
    @Composable
    fun CustomStateContent(
        instance: ToolkitInstance
    )
}