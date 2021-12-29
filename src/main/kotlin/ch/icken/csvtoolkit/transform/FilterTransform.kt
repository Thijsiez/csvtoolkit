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
import androidx.compose.foundation.layout.requiredHeight
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
import ch.icken.csvtoolkit.transform.FilterTransform.FilterSerializer
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.transform.condition.ConditionItemView
import ch.icken.csvtoolkit.ui.Confirmation
import ch.icken.csvtoolkit.ui.DeleteConditionConfirmation
import ch.icken.csvtoolkit.ui.reorderableItemModifier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.burnoutcrew.reorderable.move
import org.burnoutcrew.reorderable.rememberReorderState
import org.burnoutcrew.reorderable.reorderable

@Serializable(with = FilterSerializer::class)
class FilterTransform() : ConditionParentTransform(), TransformCustomItemView {
    override val description get() = buildAnnotatedString {
        append("Filter on ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(conditions.size.toString())
        }
        append(" condition")
        if (conditions.size != 1) append('s')
    }
    override val surrogate get() = FilterSurrogate(conditions)

    constructor(surrogate: FilterSurrogate) : this() {
        conditions.addAll(surrogate.conditions.map { it.adopt(this, null) })
    }

    override fun doTheHeaderThing(intermediate: MutableList<String>) = intermediate

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        if (conditions.isEmpty()) return@coroutineScope intermediate
        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                (chunk as MutableList).onEach { row, iterator ->
                    if (conditions.any { !it.check(row) }) iterator.remove()
                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val context = getContext(instance)

        if (!conditions.all { it.isValid(context) }) {
            invalidMessage = "One or more conditions are invalid"
            return false
        }

        return super.isValid(instance)
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
                if (this@FilterTransform == instance.currentlyProcessingTransform) {
                    CircularProgressIndicator(Modifier.padding(4.dp))
                } else {
                    Row(
                        modifier = Modifier.requiredHeight(48.dp)
                            .padding(horizontal = 12.dp),
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
                modifier = Modifier.padding(vertical = 6.dp),
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
                        context = getContext(instance),
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
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        val reorderState = rememberReorderState()
        var showEditConditionDialogFor: Condition? by remember { mutableStateOf(null) }
        var showConfirmationDialogFor: Confirmation? by remember { mutableStateOf(null) }

        EditDialog(
            titleText = "Filter",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(360.dp, Dp.Unspecified)
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
                            context = getContext(instance),
                            condition = condition,
                            onEditCondition = { showEditConditionDialogFor = it },
                            modifier = Modifier.reorderableItemModifier(reorderState, condition)
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
                                    val condition = it.create(this@FilterTransform, null)
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

        showEditConditionDialogFor?.let { condition ->
            condition.Dialog(
                context = getContext(instance),
                onHide = { showEditConditionDialogFor = null },
                onDelete = {
                    showConfirmationDialogFor = DeleteConditionConfirmation(
                        condition = condition,
                        onHide = { showConfirmationDialogFor = null }
                    )
                }
            )
        }
        showConfirmationDialogFor?.Dialog()
    }

    @Serializable
    @SerialName("filter")
    class FilterSurrogate(
        override val conditions: List<Condition>
    ) : ConditionParentTransformSurrogate
    object FilterSerializer : KSerializer<FilterTransform> {
        override val descriptor = FilterSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: FilterTransform) {
            encoder.encodeSerializableValue(FilterSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): FilterTransform {
            return FilterTransform(decoder.decodeSerializableValue(FilterSurrogate.serializer()))
        }
    }
}