package ch.icken.csvtoolkit.transform.aggregate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ch.icken.csvtoolkit.transform.Transform.AggregateFosterParent
import ch.icken.csvtoolkit.transform.Transform.AggregateParentTransform
import ch.icken.csvtoolkit.transform.aggregate.MinMaxAggregate.MinMaxSerializer
import ch.icken.csvtoolkit.ui.Spinner
import ch.icken.csvtoolkit.util.InterpretAs
import ch.icken.csvtoolkit.util.interpret
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MinMaxSerializer::class)
class MinMaxAggregate(override val parentTransform: AggregateParentTransform) : Aggregate() {
    override val description get() = buildAnnotatedString {
        append(if (minimum) "Min" else "Max")
        append(" of ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" as ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(columnName)
        }
    }
    override val columnName get() = asColumnName.text.ifBlank { "${if (minimum) "MIN" else "MAX"}($column)" }
    override val surrogate get() = MinMaxSurrogate(column, asColumnName.text, minimum)

    private var column: String? by mutableStateOf(null)
    private var asColumnName by mutableStateOf(TextFieldValue())
    private var minimum by mutableStateOf(true)
    private var interpretAs by mutableStateOf(InterpretAs.TEXT)
    private var caseInsensitive by mutableStateOf(false)

    constructor(surrogate: MinMaxSurrogate) : this(AggregateFosterParent) {
        column = surrogate.column
        asColumnName = TextFieldValue(surrogate.asColumnName)
        minimum = surrogate.minimum
    }

    override fun aggregate(group: List<Map<String, String>>): String {
        val columnName = column ?: return Error.InvalidReference
        return group
            .run {
                val selector: (Map<String, String>) -> String? = { it[columnName] }
                val comparator = compareBy<String?> {
                    if (interpretAs == InterpretAs.TEXT) {
                        it?.lowercaseIf { caseInsensitive }
                    } else {
                        it?.interpret(interpretAs)
                    }
                }
                if (minimum) {
                    minOfWithOrNull(comparator, selector)
                } else {
                    maxOfWithOrNull(comparator, selector)
                }
            } ?: Error.NoReferencedData
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
        EditDialog(
            titleText = if (minimum) "Minimum" else "Maximum",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(420.dp, Dp.Unspecified)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { minimum = !minimum }
                    ) {
                        Text(if (minimum) "Min" else "Max")
                    }
                    Text("of")
                    Spinner(
                        items = context.headers,
                        selectedItem = { column },
                        onItemSelected = { column = it },
                        itemTransform = { it ?: "-" },
                        label = "Reference Column"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "As",
                        modifier = Modifier.width(20.dp)
                    )
                    OutlinedTextField(
                        value = asColumnName,
                        onValueChange = { asColumnName = it },
                        modifier = Modifier.padding(bottom = 8.dp),
                        label = { Text("Column Name") },
                        placeholder = { Text(columnName) },
                        singleLine = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spinner(
                        items = InterpretAs.values().asList(),
                        selectedItem = { interpretAs },
                        onItemSelected = { interpretAs = it },
                        itemTransform = { it.uiName },
                        label = "Interpret as"
                    )
                    if (interpretAs == InterpretAs.TEXT) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = caseInsensitive,
                                onCheckedChange = { isChecked ->
                                    caseInsensitive = isChecked
                                }
                            )
                            Text("Case Insensitive")
                        }
                    }
                }
            }
        }
    }

    @Serializable
    @SerialName("minmax")
    class MinMaxSurrogate(
        override val column: String?,
        override val asColumnName: String,
        val minimum: Boolean
    ) : AggregateSurrogate
    object MinMaxSerializer : KSerializer<MinMaxAggregate> {
        override val descriptor = MinMaxSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: MinMaxAggregate) {
            encoder.encodeSerializableValue(MinMaxSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): MinMaxAggregate {
            return MinMaxAggregate(decoder.decodeSerializableValue(MinMaxSurrogate.serializer()))
        }
    }
    override fun adopt(parentTransform: AggregateParentTransform): Aggregate {
        return MinMaxAggregate(parentTransform).also { copy ->
            copy.column = column
            copy.asColumnName = asColumnName
            copy.minimum = minimum
        }
    }
}