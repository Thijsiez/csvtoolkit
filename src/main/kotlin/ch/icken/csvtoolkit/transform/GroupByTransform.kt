package ch.icken.csvtoolkit.transform

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.move
import ch.icken.csvtoolkit.set
import ch.icken.csvtoolkit.transform.GroupByTransform.GroupSerializer
import ch.icken.csvtoolkit.transform.Transform.AggregateParentTransform
import ch.icken.csvtoolkit.transform.aggregate.Aggregate
import ch.icken.csvtoolkit.transform.aggregate.AggregateItemView
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.ui.Confirmation
import ch.icken.csvtoolkit.ui.DeleteAggregateConfirmation
import ch.icken.csvtoolkit.ui.VerticalDivider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Serializable(with = GroupSerializer::class)
class GroupByTransform() : AggregateParentTransform(), TransformCustomItemView {
    override val description get() = buildAnnotatedString {
        append("Group by ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(numberOfColumnsSelected.toString())
        }
        append(" column")
        if (numberOfColumnsSelected != 1) append('s')
        append(", then aggregate ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(aggregates.size.toString())
        }
        append(" value")
        if (aggregates.size != 1) append('s')
    }
    override val surrogate get() = GroupSurrogate(aggregates, groupByColumns)

    private val groupByColumns = mutableStateListOf<GroupBy>()
    private val numberOfColumnsSelected by derivedStateOf { groupByColumns.count { it.select } }
    private val keepColumns by derivedStateOf { groupByColumns.filter { it.select }.map { it.columnName } }

    constructor(surrogate: GroupSurrogate) : this() {
        aggregates.addAll(surrogate.aggregates.map { it.adopt(this) })
        groupByColumns.clear()
        groupByColumns.addAll(surrogate.groupByColumns)
    }

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        if (numberOfColumnsSelected == 0) return intermediate
        return intermediate.apply {
            removeAll { it !in keepColumns }
            addAll(aggregates.map { it.columnName })
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        if (numberOfColumnsSelected == 0) return@coroutineScope intermediate

        val groups = intermediate.groupBy {
            it.filterKeys { columnName -> columnName in keepColumns } as MutableMap
        } as MutableMap
        return@coroutineScope groups.entries.chunked(chunkSize(groups.size)).map { chunk ->
            async {
                chunk.onEach { (output, group) ->
                    output.putAll(aggregates.associate {
                        it.columnName to it.aggregate(group)
                    })
                }
            }
        }.awaitAll().flatten().map { it.key } as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val incomingColumnNames = instance.headersUpTo(this)
        val columnNames = groupByColumns.map { it.columnName }
        val context = getAggregateContext(instance)

        if (!incomingColumnNames.all { it in columnNames }) {
            invalidMessage = "Group by is under-defined"
            return false
        }
        if (!columnNames.all { it in incomingColumnNames }) {
            invalidMessage = "Group by is over-defined"
            return false
        }
        if (numberOfColumnsSelected == groupByColumns.size) {
            invalidMessage = "Grouping is not possible"
            return false
        }
        if (!aggregates.all { it.isValid(context) }) {
            invalidMessage = "One or more aggregates are invalid"
            return false
        }

        return super.isValid(instance)
    }

    @Composable
    override fun CustomItemView(
        instance: ToolkitInstance,
        onEditTransform: (Transform) -> Unit,
        onEditAggregate: (Aggregate) -> Unit,
        onEditCondition: (Condition) -> Unit,
        modifier: Modifier
    ) {
        Column(
            modifier = modifier
                .clickable(
                    enabled = !instance.isDoingTheThing,
                    onClick = { onEditTransform(this) }
                )
                .fillMaxWidth()
                .padding(start = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = description,
                    modifier = Modifier.weight(1f)
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.body1
                )
                if (this@GroupByTransform == instance.currentlyProcessingTransform) {
                    CircularProgressIndicator(Modifier.padding(4.dp))
                } else {
                    Row(
                        modifier = Modifier.requiredHeight(48.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (numberOfColumnsSelected) {
                            0 -> TransformWarningIcon("Will be skipped")
                            else -> DefaultTransformStateContent(instance, this@GroupByTransform)
                        }
                    }
                }
            }
            Divider()
            Text(
                text = "Columns",
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.caption
            )
            Text(
                text = keepColumns.joinToString(),
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp)
            )
            Divider()
            Text(
                text = "Aggregates",
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.caption
            )
            if (aggregates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(48.dp)
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("None")
                }
            } else {
                aggregates.forEach {
                    AggregateItemView(
                        context = getAggregateContext(instance),
                        aggregate = it,
                        onEditAggregate = onEditAggregate
                    )
                }
            }
        }
    }

    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        val scrollState = rememberLazyListState()
        var expanded by remember { mutableStateOf(false) }
        val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
            aggregates.move(from.index, to.index)
        })
        var showEditAggregateDialogFor: Aggregate? by remember { mutableStateOf(null) }
        var showConfirmationDialogFor: Confirmation? by remember { mutableStateOf(null) }

        EditDialog(
            titleText = "Group By",
            onHide = onHide,
            onDelete = onDelete,
            onOpen = {
                val retain = groupByColumns.associate { it.columnName to it.select }
                groupByColumns.clear()
                instance.headersUpTo(this).forEach { columnName ->
                    groupByColumns.add(GroupBy(columnName, retain[columnName] ?: false))
                }
            },
            state = rememberDialogState(
                size = DpSize(640.dp, Dp.Unspecified)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Columns",
                        fontWeight = FontWeight.Bold
                    )
                    Box {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            state = scrollState
                        ) {
                            itemsIndexed(groupByColumns) { index, (columnName, checked) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .height(40.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            groupByColumns.set(index) { it.copy(select = isChecked) }
                                        }
                                    )
                                    Text(
                                        text = columnName,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
                VerticalDivider()
                Column(
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Aggregates",
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.reorderable(reorderState)
                                .detectReorderAfterLongPress(reorderState),
                            state = reorderState.listState
                        ) {
                            items(aggregates, { it }) { aggregate ->
                                ReorderableItem(reorderState, key = aggregate) { isDragging ->
                                    val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                    AggregateItemView(
                                        context = getAggregateContext(instance),
                                        aggregate = aggregate,
                                        onEditAggregate = { showEditAggregateDialogFor = it },
                                        modifier = Modifier.shadow(elevation.value)
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(reorderState.listState),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    Box {
                        TextButton(
                            onClick = { expanded = true }
                        ) {
                            Text("ADD AGGREGATE")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Aggregate.Type.values().forEach {
                                DropdownMenuItem(
                                    onClick = {
                                        val aggregate = it.create(this@GroupByTransform)
                                        aggregates.add(aggregate)
                                        showEditAggregateDialogFor = aggregate
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
        }

        showEditAggregateDialogFor?.let { aggregate ->
            aggregate.Dialog(
                context = getAggregateContext(instance),
                onHide = { showEditAggregateDialogFor = null },
                onDelete = {
                    showConfirmationDialogFor = DeleteAggregateConfirmation(
                        aggregate = aggregate,
                        onHide = { showConfirmationDialogFor = null }
                    )
                }
            )
        }
        showConfirmationDialogFor?.Dialog()
    }

    @Serializable
    data class GroupBy(
        val columnName: String,
        val select: Boolean
    )

    @Serializable
    @SerialName("group")
    class GroupSurrogate(
        override val aggregates: List<Aggregate>,
        val groupByColumns: List<GroupBy>
    ) : AggregateParentTransformSurrogate
    object GroupSerializer : KSerializer<GroupByTransform> {
        override val descriptor = GroupSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: GroupByTransform) {
            encoder.encodeSerializableValue(GroupSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): GroupByTransform {
            return GroupByTransform(decoder.decodeSerializableValue(GroupSurrogate.serializer()))
        }
    }
}