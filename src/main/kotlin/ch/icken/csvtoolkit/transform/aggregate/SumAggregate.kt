package ch.icken.csvtoolkit.transform.aggregate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import ch.icken.csvtoolkit.transform.EditDialog
import ch.icken.csvtoolkit.transform.Transform.AggregateFosterParent
import ch.icken.csvtoolkit.transform.Transform.AggregateParentTransform
import ch.icken.csvtoolkit.transform.aggregate.SumAggregate.SumSerializer
import ch.icken.csvtoolkit.ui.Spinner
import ch.icken.csvtoolkit.util.interpretAsNumber
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SumSerializer::class)
class SumAggregate(override val parentTransform: AggregateParentTransform) : Aggregate() {
    override val description get() = buildAnnotatedString {
        append("Sum of ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" as ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(columnName)
        }
    }
    override val columnName get() = asColumnName.text.ifBlank { "SUM($column)" }
    override val surrogate get() = SumSurrogate(column, asColumnName.text)

    private var column: String? by mutableStateOf(null)
    private var asColumnName by mutableStateOf(TextFieldValue(""))

    constructor(surrogate: SumSurrogate) : this(AggregateFosterParent) {
        column = surrogate.column
        asColumnName = TextFieldValue(surrogate.asColumnName)
    }

    override fun aggregate(group: List<Map<String, String>>): String {
        val columnName = column ?: return Error.InvalidReference
        return group
            .mapNotNull { it[columnName]?.interpretAsNumber() }
            .sum().toString()
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
            titleText = "Sum",
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
                    Text(
                        text = "Of",
                        modifier = Modifier.width(20.dp)
                    )
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
                //TODO format
            }
        }
    }

    @Serializable
    @SerialName("sum")
    class SumSurrogate(
        override val column: String?,
        override val asColumnName: String
    ) : AggregateSurrogate
    object SumSerializer : KSerializer<SumAggregate> {
        override val descriptor = SumSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SumAggregate) {
            encoder.encodeSerializableValue(SumSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): SumAggregate {
            return SumAggregate(decoder.decodeSerializableValue(SumSurrogate.serializer()))
        }
    }
    override fun adopt(parentTransform: AggregateParentTransform): Aggregate {
        return SumAggregate(parentTransform).also { copy ->
            copy.column = column
            copy.asColumnName = asColumnName
        }
    }
}