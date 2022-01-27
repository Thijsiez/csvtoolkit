package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.lowercaseIf
import ch.icken.csvtoolkit.transform.EditDialog
import ch.icken.csvtoolkit.transform.Transform.ConditionFosterParent
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.transform.condition.ListCondition.ListSerializer
import ch.icken.csvtoolkit.ui.Spinner
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.burnoutcrew.reorderable.move
import org.burnoutcrew.reorderable.rememberReorderState
import org.burnoutcrew.reorderable.reorderable

@Serializable(with = ListSerializer::class)
class ListCondition(
    override val parentTransform: ConditionParentTransform,
    override val parentCondition: ConditionParent?
) : Condition(), ConditionCustomStateContent {
    override val description get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" is one of ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(compareTo.size.toString())
        }
        append(" values")
    }
    override val surrogate get() = ListSurrogate(column, compareTo)

    private var column: String? by mutableStateOf(null)
    private val compareTo = mutableStateListOf<String>()
    private val compareValues by derivedStateOf { compareTo.map { it.lowercaseIf { caseInsensitive } } }
    private var caseInsensitive by mutableStateOf(false)

    constructor(surrogate: ListSurrogate) : this(ConditionFosterParent, null) {
        column = surrogate.column
        compareTo.clear()
        compareTo.addAll(surrogate.compareTo)
    }

    override fun check(row: Map<String, String>): Boolean {
        if (compareTo.isEmpty()) return false
        val columnName = column ?: return false
        val referenceText = row[columnName]?.lowercaseIf { caseInsensitive } ?: return false
        return compareValues.any { referenceText == it }
    }

    override fun isValid(context: Context): Boolean {
        val columnName = column

        if (columnName == null) {
            invalidMessage = "Missing reference column"
            return false
        }
        if (columnName !in context.headers) {
            invalidMessage = "Reference column not available"
            return false
        }

        return true
    }

    @Composable
    override fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        val reorderState = rememberReorderState()
        var showEditValueDialogForIndex by remember { mutableStateOf(-1) }
        var editValue by remember { mutableStateOf(TextFieldValue()) }

        EditDialog(
            titleText = "List Condition",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(320.dp, Dp.Unspecified)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spinner(
                        items = context.headers,
                        selectedItem = { column },
                        onItemSelected = { column = it },
                        itemTransform = { it ?: "-" },
                        label = "Reference Column"
                    )
                    Text("is one of")
                }
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.reorderable(
                            state = reorderState,
                            onMove = { from, to ->
                                compareTo.move(from.index, to.index)
                            }
                        ),
                        state = reorderState.listState
                    ) {
                        itemsIndexed(compareTo) { index, value ->
                            Row(
                                modifier = Modifier
                                    .clickable(
                                        onClick = { showEditValueDialogForIndex = index }
                                    )
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "-",
                                    color = Color.Gray
                                )
                                Text(
                                    text = value,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(reorderState.listState),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
                TextButton(
                    onClick = {
                        compareTo.add("")
                        showEditValueDialogForIndex = compareTo.size - 1
                    }
                ) {
                    Text("ADD VALUE")
                }
            }
        }

        showEditValueDialogForIndex.takeIf {
            it > -1 && it < compareTo.size
        }?.let { index ->
            EditDialog(
                titleText = "Edit Value",
                onHide = { showEditValueDialogForIndex = -1 },
                onDelete = { compareTo.removeAt(index) },
                onOpen = { editValue = TextFieldValue(compareTo[index]) },
                state = rememberDialogState(
                    size = DpSize(320.dp, Dp.Unspecified)
                )
            ) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = {
                        compareTo[index] = it.text
                        editValue = it
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    singleLine = true
                )
            }
        }
    }

    @Composable
    override fun CustomStateContent(
        context: Context
    ) {
        when {
            compareTo.isEmpty() -> ConditionWarningIcon("Will always be false")
            else -> DefaultConditionStateContent(context, this)
        }
    }

    @Serializable
    @SerialName("list")
    class ListSurrogate(
        val column: String?,
        val compareTo: List<String>
    ) : ConditionSurrogate
    object ListSerializer : KSerializer<ListCondition> {
        override val descriptor = ListSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: ListCondition) {
            encoder.encodeSerializableValue(ListSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): ListCondition {
            return ListCondition(decoder.decodeSerializableValue(ListSurrogate.serializer()))
        }
    }
    override fun adopt(parentTransform: ConditionParentTransform, parentCondition: ConditionParent?): Condition {
        return ListCondition(parentTransform, parentCondition).also { copy ->
            copy.column = column
            copy.compareTo.clear()
            copy.compareTo.addAll(compareTo)
        }
    }
}