package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.onEach
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.transform.condition.ConditionItemView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.burnoutcrew.reorderable.move
import org.burnoutcrew.reorderable.rememberReorderState
import org.burnoutcrew.reorderable.reorderable

class FilterTransform : Transform(), TransformCustomItemView {
    override val description get() = buildAnnotatedString {
        append("Filter on ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(conditions.size.toString())
        }
        append(" conditions")
    }

    private val conditions = mutableStateListOf<Condition>()

    override fun doTheHeaderThing(intermediate: MutableList<String>) = intermediate

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                (chunk as MutableList).onEach { row, iterator ->
                    if (conditions.any { !it.check(row) }) iterator.remove()
                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun CustomItemView(
        instance: ToolkitInstance,
        onEditTransform: (Transform) -> Unit,
        onEditCondition: (Condition) -> Unit,
        modifier: Modifier
    ) {
        Column(
            modifier = modifier
                .combinedClickable(
                    onClick = { onEditTransform(this) }
                )
                .fillMaxWidth()
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = description,
                    modifier = modifier.weight(1f),
                    style = MaterialTheme.typography.body1
                )
                if (this@FilterTransform == instance.currentlyProcessingTransform) {
                    CircularProgressIndicator(Modifier.padding(4.dp))
                } else {
                    Row(
                        modifier = Modifier.requiredSize(48.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            conditions.isEmpty() -> TransformWarningIcon("Will be skipped")
                            isValid(instance) -> TransformValidIcon()
                            else -> TransformInvalidIcon(invalidMessage)
                        }
                    }
                }
            }
            Divider()
            Text(
                text = "Conditions",
                style = MaterialTheme.typography.caption
            )
            if (conditions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(48.dp)
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("None")
                }
            } else {
                conditions.forEach {
                    ConditionItemView(
                        condition = it,
                        onEditCondition = onEditCondition
                    )
                }
            }
        }
    }

    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        val reorderState = rememberReorderState()
        var showEditConditionDialogFor: Condition? by remember { mutableStateOf(null) }

        TransformEditDialog(
            titleText = "Filter",
            onHide = onHide,
            state = rememberDialogState(
                size = DpSize(480.dp, Dp.Unspecified)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Conditions",
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.weight(1f)
                        .reorderable(
                            state = reorderState,
                            onMove = { from, to ->
                                conditions.move(from.index, to.index)
                            }
                        ),
                    state = reorderState.listState
                ) {
                    items(conditions, { it }) { condition ->
                        ConditionItemView(
                            condition = condition,
                            onEditCondition = { showEditConditionDialogFor = it },
                            modifier = reorderableItemModifier(reorderState, condition)
                        )
                    }
                }
                Box {
                    TextButton(
                        onClick = { expanded = true }
                    ) {
                        Text("ADD CONDITION")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Condition.Type.values().forEach {
                            DropdownMenuItem(
                                onClick = {
                                    val condition = it.create(this@FilterTransform)
                                    conditions.add(condition)
                                    showEditConditionDialogFor = condition
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

        showEditConditionDialogFor?.Dialog(
            context = getContext(instance),
            onHide = { showEditConditionDialogFor = null }
        )
    }
}