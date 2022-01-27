package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.transform.EditDialog
import ch.icken.csvtoolkit.transform.Transform.ConditionFosterParent
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.transform.condition.Condition.ConditionParent
import ch.icken.csvtoolkit.transform.condition.OrCondition.OrSerializer
import ch.icken.csvtoolkit.ui.Confirmation
import ch.icken.csvtoolkit.ui.DeleteConditionConfirmation
import ch.icken.csvtoolkit.ui.reorderableItemModifier
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.burnoutcrew.reorderable.move
import org.burnoutcrew.reorderable.rememberReorderState
import org.burnoutcrew.reorderable.reorderable

@Serializable(with = OrSerializer::class)
class OrCondition(
    override val parentTransform: ConditionParentTransform,
    override val parentCondition: ConditionParent?
) : ConditionParent(), ConditionCustomItemView {
    override val description get() = buildAnnotatedString {
        append("At least one of the following")
    }
    override val surrogate get() = OrSurrogate(conditions)

    constructor(surrogate: OrSurrogate) : this(ConditionFosterParent, null) {
        conditions.addAll(surrogate.conditions)
    }

    override fun check(row: Map<String, String>) = conditions.any { it.check(row) }

    override fun isValid(context: Context): Boolean {
        if (!conditions.all { it.isValid(context) }) {
            invalidMessage = "One or more conditions are invalid"
            return false
        }
        return true
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun CustomItemView(
        context: Context,
        onEditCondition: (Condition) -> Unit,
        modifier: Modifier
    ) {
        Column(
            modifier = modifier
                .clickable(
                    enabled = context.allowChanges,
                    onClick = { onEditCondition(this) }
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
                Row(
                    modifier = Modifier.requiredHeight(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DefaultConditionStateContent(context, this@OrCondition)
                }
            }
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
                        context = context,
                        condition = it,
                        onEditCondition = onEditCondition
                    )
                }
            }
        }
    }

    @Composable
    override fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        val reorderState = rememberReorderState()
        var showEditConditionDialogFor: Condition? by remember { mutableStateOf(null) }
        var showConfirmationDialogFor: Confirmation? by remember { mutableStateOf(null) }

        EditDialog(
            titleText = "Or",
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
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.reorderable(
                            state = reorderState,
                            onMove = { from, to ->
                                conditions.move(from.index, to.index)
                            }
                        ),
                        state = reorderState.listState
                    ) {
                        items(conditions, { it }) { condition ->
                            ConditionItemView(
                                context = context,
                                condition = condition,
                                onEditCondition = { showEditConditionDialogFor = it },
                                modifier = Modifier.reorderableItemModifier(reorderState, condition)
                            )
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
                        Text("ADD CONDITION")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Type.values().forEach {
                            DropdownMenuItem(
                                onClick = {
                                    val condition = it.create(parentTransform, this@OrCondition)
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
                context = context,
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
    @SerialName("or")
    class OrSurrogate(
        override val conditions: List<Condition>
    ) : ConditionParentSurrogate
    object OrSerializer : KSerializer<OrCondition> {
        override val descriptor = OrSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: OrCondition) {
            encoder.encodeSerializableValue(OrSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): OrCondition {
            return OrCondition(decoder.decodeSerializableValue(OrSurrogate.serializer()))
        }
    }
    override fun adopt(parentTransform: ConditionParentTransform, parentCondition: ConditionParent?): Condition {
        return OrCondition(parentTransform, parentCondition).also { copy ->
            copy.conditions.addAll(conditions.map { it.adopt(parentTransform, this) })
        }
    }
}