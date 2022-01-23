package ch.icken.csvtoolkit.transform.aggregate

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.text.font.FontStyle
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
import ch.icken.csvtoolkit.transform.aggregate.CountAggregate.CountSerializer
import ch.icken.csvtoolkit.ui.Spinner
import ch.icken.csvtoolkit.ui.Tooltip
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = CountSerializer::class)
class CountAggregate(override val parentTransform: AggregateParentTransform) : Aggregate() {
    override val description get() = buildAnnotatedString {
        append("Count ")
        if (distinct) {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append("distinct ")
            }
        }
        append("by ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" as ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(columnName)
        }
    }
    override val columnName get() = asColumnName.text.ifBlank { "COUNT($column)" }
    override val surrogate get() = CountSurrogate(column, asColumnName.text, distinct, caseInsensitive)

    private var column: String? by mutableStateOf(null)
    private var asColumnName by mutableStateOf(TextFieldValue(""))
    private var distinct by mutableStateOf(false)
    private var caseInsensitive by mutableStateOf(false)

    constructor(surrogate: CountSurrogate) : this(AggregateFosterParent) {
        column = surrogate.column
        asColumnName = TextFieldValue(surrogate.asColumnName)
        distinct = surrogate.distinct
        caseInsensitive = surrogate.caseInsensitive
    }

    override fun aggregate(group: List<Map<String, String>>): String {
        val columnName = column ?: return Error.InvalidReference
        return if (distinct) {
            group
                .distinctBy {
                    it[columnName]?.lowercaseIf { caseInsensitive }
                }
                .size.toString()
        } else {
            group.size.toString()
        }
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        EditDialog(
            titleText = "Count",
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
                        text = "By",
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
                        placeholder = { Text(columnName)},
                        singleLine = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TooltipArea(
                        tooltip = { Tooltip("Only count unique values") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = distinct,
                                onCheckedChange = { isChecked ->
                                    distinct = isChecked
                                }
                            )
                            Text("Distinct")
                        }
                    }
                    if (distinct) {
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
    @SerialName("count")
    class CountSurrogate(
        override val column: String?,
        override val asColumnName: String,
        val distinct: Boolean,
        val caseInsensitive: Boolean
    ) : AggregateSurrogate
    object CountSerializer : KSerializer<CountAggregate> {
        override val descriptor = CountSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: CountAggregate) {
            encoder.encodeSerializableValue(CountSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): CountAggregate {
            return CountAggregate(decoder.decodeSerializableValue(CountSurrogate.serializer()))
        }
    }
    override fun adopt(parentTransform: AggregateParentTransform): Aggregate {
        return CountAggregate(parentTransform).also { copy ->
            copy.column = column
            copy.asColumnName = asColumnName
            copy.distinct = distinct
        }
    }
}