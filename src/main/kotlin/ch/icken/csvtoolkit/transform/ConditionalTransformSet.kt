package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.transform.condition.ConditionItemView
import ch.icken.csvtoolkit.ui.VerticalDivider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.burnoutcrew.reorderable.move
import org.burnoutcrew.reorderable.rememberReorderState
import org.burnoutcrew.reorderable.reorderable

class ConditionalTransformSet : Transform(), TransformCustomItemView {
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

    private val conditions = mutableStateListOf<Condition>()
    private val transforms = mutableStateListOf<ConditionalTransform>()

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        return transforms.fold(intermediate) { intermediateHeaders, transform ->
            transform.doTheConditionalHeaderThing(intermediateHeaders)
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
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
        if (!transforms.all { it.isValidConditional(getContext(instance)) }) {
            invalidMessage = "One or more transforms are invalid"
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
                .combinedClickable (
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
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body1
                )
                if (this@ConditionalTransformSet == instance.currentlyProcessingTransform) {
                    CircularProgressIndicator(Modifier.padding(4.dp))
                } else {
                    Row(
                        modifier = Modifier.requiredSize(48.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
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
            Divider()
            Text(
                text = "Transforms",
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
                        onEditTransform = onEditTransform,
                        stateContent = {
                            DefaultConditionalTransformStateContent(instance, it)
                        }
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
        var conditionsExpanded by remember { mutableStateOf(false) }
        val conditionReorderState = rememberReorderState()
        var transformsExpanded by remember { mutableStateOf(false) }
        val transformReorderState = rememberReorderState()
        var showEditConditionDialogFor: Condition? by remember { mutableStateOf(null) }
        var showEditTransformDialogFor: ConditionalTransform? by remember { mutableStateOf(null) }

        TransformEditDialog(
            titleText = "Conditional",
            onHide = onHide,
            state = rememberDialogState(
                size = DpSize(720.dp, 480.dp)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
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
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                            .reorderable(
                                state = conditionReorderState,
                                onMove = { from, to ->
                                    conditions.move(from.index, to.index)
                                }
                            ),
                        state = conditionReorderState.listState
                    ) {
                        items(conditions, { it }) { condition ->
                            ConditionItemView(
                                condition = condition,
                                onEditCondition = { showEditConditionDialogFor = it },
                                modifier = reorderableItemModifier(conditionReorderState, condition)
                            )
                        }
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
                                        val condition = it.create(this@ConditionalTransformSet)
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
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                            .reorderable(
                                state = transformReorderState,
                                onMove = { from, to ->
                                    transforms.move(from.index, to.index)
                                }
                            ),
                        state = transformReorderState.listState
                    ) {
                        items(transforms, { it }) { transform ->
                            TransformItemView(
                                instance = instance,
                                transform = transform,
                                onEditTransform = { if (it is ConditionalTransform) showEditTransformDialogFor = it },
                                modifier = reorderableItemModifier(transformReorderState, transform)
                            ) {
                                DefaultConditionalTransformStateContent(instance, transform)
                            }
                        }
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

        showEditConditionDialogFor?.Dialog(
            context = getContext(instance),
            onHide = { showEditConditionDialogFor = null }
        )
        showEditTransformDialogFor?.let { transform ->
            if (transform.parent != null) {
                transform.ConditionalDialog(
                    context = transform.parent.getContext(instance),
                    onHide = { showEditTransformDialogFor = null }
                )
            }
        }
    }
}