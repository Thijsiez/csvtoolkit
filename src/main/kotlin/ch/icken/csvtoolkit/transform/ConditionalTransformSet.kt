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
import androidx.compose.foundation.rememberScrollbarAdapter
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
import ch.icken.csvtoolkit.flatMapToSet
import ch.icken.csvtoolkit.move
import ch.icken.csvtoolkit.transform.ConditionalTransformSet.ConditionalSetSerializer
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.transform.aggregate.Aggregate
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.transform.condition.ConditionItemView
import ch.icken.csvtoolkit.ui.Confirmation
import ch.icken.csvtoolkit.ui.DeleteConditionConfirmation
import ch.icken.csvtoolkit.ui.DeleteConfirmationContent
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

@Serializable(with = ConditionalSetSerializer::class)
class ConditionalTransformSet() : ConditionParentTransform(), TransformCustomItemView {
    override val description get() = buildAnnotatedString {
        append("Do ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(transforms.size.toString())
        }
        append(" ${if (transforms.size != 1) "things" else "thing"} when ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(conditions.size.toString())
        }
        append(" ${if (conditions.size != 1) "conditions are" else "condition is"} met")
    }
    override val surrogate get() = ConditionalSetSurrogate(conditions, transforms)
    override val usesFiles get() = transforms.flatMapToSet { it.usesFiles } + conditions.flatMapToSet { it.usesFiles }

    private val transforms = mutableStateListOf<ConditionalTransform>()

    constructor(surrogate: ConditionalSetSurrogate) : this() {
        conditions.addAll(surrogate.conditions.map { it.adopt(this, null) })
        transforms.addAll(surrogate.transforms.map { it.adopt(this) })
    }

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        if (transforms.isEmpty()) return intermediate
        return transforms.fold(intermediate) { intermediateHeaders, transform ->
            transform.doTheConditionalHeaderThing(intermediateHeaders)
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        if (transforms.isEmpty()) return@coroutineScope intermediate
        if (!conditions.all { it.prepareChecks() }) return@coroutineScope intermediate
        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                chunk.onEach { row ->
                    if (conditions.all { it.check(row) }) {
                        transforms.fold(row) { intermediateRow, transform ->
                            transform.doTheConditionalThing(intermediateRow)
                        }
                    }
                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val context = getConditionContext(instance)

        if (!conditions.all { it.isValid(context) }) {
            invalidMessage = "One or more conditions are invalid"
            return false
        }
        if (!transforms.all { it.isValidConditional(context) }) {
            invalidMessage = "One or more transforms are invalid"
            return false
        }

        return super.isValid(instance)
    }

    fun remove(transform: ConditionalTransform) = transforms.remove(transform)

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
                if (this@ConditionalTransformSet == instance.currentlyProcessingTransform) {
                    CircularProgressIndicator(Modifier.padding(4.dp))
                } else {
                    Row(
                        modifier = Modifier.requiredHeight(48.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            transforms.isEmpty() -> TransformWarningIcon("Will be skipped")
                            conditions.isEmpty() -> TransformWarningIcon("Could be not-conditional")
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
                        context = getConditionContext(instance),
                        condition = it,
                        onEditCondition = onEditCondition
                    )
                }
            }
            Divider()
            Text(
                text = "Transforms",
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.caption
            )
            if (transforms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(48.dp)
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("None")
                }
            } else {
                transforms.forEach {
                    TransformItemView(
                        instance = instance,
                        transform = it,
                        onEditTransform = onEditTransform
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
        var conditionsExpanded by remember { mutableStateOf(false) }
        val conditionReorderState = rememberReorderableLazyListState(onMove = { from, to ->
            conditions.move(from.index, to.index)
        })
        var transformsExpanded by remember { mutableStateOf(false) }
        val transformReorderState = rememberReorderableLazyListState(onMove = { from, to ->
            transforms.move(from.index, to.index)
        })
        var showEditConditionDialogFor: Condition? by remember { mutableStateOf(null) }
        var showEditTransformDialogFor: ConditionalTransform? by remember { mutableStateOf(null) }
        var showConfirmationDialogFor: Confirmation? by remember { mutableStateOf(null) }

        EditDialog(
            titleText = "Conditional",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(720.dp, Dp.Unspecified)
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Conditions",
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.reorderable(conditionReorderState)
                                .detectReorderAfterLongPress(conditionReorderState),
                            state = conditionReorderState.listState
                        ) {
                            items(conditions, { it }) { condition ->
                                ReorderableItem(conditionReorderState, key = condition) { isDragging ->
                                    val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                    ConditionItemView(
                                        context = getConditionContext(instance),
                                        condition = condition,
                                        onEditCondition = { showEditConditionDialogFor = it },
                                        modifier = Modifier.shadow(elevation.value)
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(conditionReorderState.listState),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    Box {
                        TextButton(
                            onClick = { conditionsExpanded = true }
                        ) {
                            Text("ADD CONDITION")
                        }
                        DropdownMenu(
                            expanded = conditionsExpanded,
                            onDismissRequest = { conditionsExpanded = false }
                        ) {
                            Condition.Type.values().forEach {
                                DropdownMenuItem(
                                    onClick = {
                                        val condition = it.create(this@ConditionalTransformSet, null)
                                        conditions.add(condition)
                                        showEditConditionDialogFor = condition
                                        conditionsExpanded = false
                                    }
                                ) {
                                    Text(it.uiName)
                                }
                            }
                        }
                    }
                }
                VerticalDivider()
                Column(
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Transforms",
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.reorderable(transformReorderState)
                                .detectReorderAfterLongPress(transformReorderState),
                            state = transformReorderState.listState
                        ) {
                            items(transforms, { it }) { transform ->
                                ReorderableItem(transformReorderState, key = transform) { isDragging ->
                                    val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                    TransformItemView(
                                        instance = instance,
                                        transform = transform,
                                        onEditTransform = {
                                            if (it is ConditionalTransform) showEditTransformDialogFor = it
                                        },
                                        modifier = Modifier.shadow(elevation.value)
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(transformReorderState.listState),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    Box {
                        TextButton(
                            onClick = { transformsExpanded = true }
                        ) {
                            Text("ADD TRANSFORM")
                        }
                        DropdownMenu(
                            expanded = transformsExpanded,
                            onDismissRequest = { transformsExpanded = false }
                        ) {
                            Type.values().filter { it.isConditional }.forEach {
                                DropdownMenuItem(
                                    onClick = {
                                        val transform = it.create(this@ConditionalTransformSet)
                                        if (transform is ConditionalTransform) {
                                            transforms.add(transform)
                                            showEditTransformDialogFor = transform
                                            transformsExpanded = false
                                        }
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

        showEditConditionDialogFor?.let { condition ->
            condition.Dialog(
                context = getConditionContext(instance),
                onHide = { showEditConditionDialogFor = null },
                onDelete = {
                    showConfirmationDialogFor = DeleteConditionConfirmation(
                        condition = condition,
                        onHide = { showConfirmationDialogFor = null }
                    )
                }
            )
        }
        showEditTransformDialogFor?.let { transform ->
            if (transform.parent != null) {
                transform.ConditionalDialog(
                    context = transform.parent.getConditionContext(instance),
                    onHide = { showEditTransformDialogFor = null },
                    onDelete = {
                        showConfirmationDialogFor = Confirmation(
                            title = "Delete conditional transform?",
                            onHide = { showConfirmationDialogFor = null },
                            positive = "DELETE" to {
                                if (transform.parent is ConditionalTransformSet) {
                                    transform.parent.remove(transform)
                                }
                            }
                        ) {
                            DeleteConfirmationContent(transform.description)
                        }
                    }
                )
            }
        }
        showConfirmationDialogFor?.Dialog()
    }

    @Serializable
    @SerialName("conditional")
    class ConditionalSetSurrogate(
        override val conditions: List<Condition>,
        val transforms: List<ConditionalTransform>
    ) : ConditionParentTransformSurrogate
    object ConditionalSetSerializer : KSerializer<ConditionalTransformSet> {
        override val descriptor = ConditionalSetSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: ConditionalTransformSet) {
            encoder.encodeSerializableValue(ConditionalSetSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): ConditionalTransformSet {
            return ConditionalTransformSet(decoder.decodeSerializableValue(ConditionalSetSurrogate.serializer()))
        }
    }
    override fun postDeserialization(instance: ToolkitInstance) {
        transforms.forEach { it.postDeserialization(instance) }
        val context = getConditionContext(instance)
        conditions.forEach { it.postDeserialization(context) }
    }
}