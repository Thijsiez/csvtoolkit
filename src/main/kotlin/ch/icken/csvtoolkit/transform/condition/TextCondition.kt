package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.transform.Transform.FosterParent
import ch.icken.csvtoolkit.transform.condition.TextCondition.TextSerializer
import ch.icken.csvtoolkit.ui.Spinner
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TextSerializer::class)
class TextCondition(
    override val parentTransform: ConditionParentTransform,
    override val parentCondition: ConditionParent?
) : Condition() {
    override val description get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(compareType.uiName.lowercase())
        }
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(compareTo.text)
        }
    }
    override val surrogate get() = TextSurrogate(column, compareType, compareTo.text)

    private var column: String? by mutableStateOf(null)
    private var compareType by mutableStateOf(Type.EQ)
    private var compareTo by mutableStateOf(TextFieldValue(""))
    private val compareText by derivedStateOf { compareTo.text.lowercaseIf { caseInsensitive } }
    private var caseInsensitive by mutableStateOf(false)

    constructor(surrogate: TextSurrogate) : this(FosterParent, null) {
        column = surrogate.column
        compareType = surrogate.compareType
        compareTo = TextFieldValue(surrogate.compareTo)
    }

    override fun check(row: Map<String, String>): Boolean {
        val columnName = column ?: return false
        val referenceText = row[columnName]?.lowercaseIf { caseInsensitive } ?: return false
        return when (compareType) {
            Type.EQ -> referenceText == compareText
            Type.NEQ -> referenceText != compareText
            Type.SW -> referenceText.startsWith(compareText)
            Type.EW -> referenceText.endsWith(compareText)
            Type.C -> referenceText.contains(compareText)
            Type.NC -> !referenceText.contains(compareText)
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

    @Composable
    override fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        EditDialog(
            titleText = "Text Condition",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(640.dp, Dp.Unspecified)
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
                    Spinner(
                        items = context.headers,
                        selectedItem = { column },
                        onItemSelected = { column = it },
                        itemTransform = { it ?: "-" },
                        label = "Reference Column"
                    )
                    Spinner(
                        items = Type.values().asList(),
                        selectedItem = { compareType },
                        onItemSelected = { compareType = it },
                        itemTransform = { it.uiName },
                        label = "Comparison Type"
                    )
                    OutlinedTextField(
                        value = compareTo,
                        onValueChange = { compareTo = it },
                        modifier = Modifier.padding(bottom = 8.dp),
                        label = { Text("Text") },
                        placeholder = { Text("Lorem ipsum") },
                        singleLine = true
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
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

    enum class Type(
        val uiName: String
    ) {
        EQ("Is equal to"),
        NEQ("Is not equal to"),
        SW("Starts with"),
        EW("Ends with"),
        C("Contains"),
        NC("Does not contain")
    }

    @Serializable
    @SerialName("text")
    class TextSurrogate(
        val column: String?,
        val compareType: Type,
        val compareTo: String
    ) : ConditionSurrogate
    object TextSerializer : KSerializer<TextCondition> {
        override val descriptor = TextSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: TextCondition) {
            encoder.encodeSerializableValue(TextSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): TextCondition {
            return TextCondition(decoder.decodeSerializableValue(TextSurrogate.serializer()))
        }
    }
    override fun adopt(parentTransform: ConditionParentTransform, parentCondition: ConditionParent?): Condition {
        return TextCondition(parentTransform, parentCondition).also { copy ->
            copy.column = column
            copy.compareType = compareType
            copy.compareTo = compareTo
        }
    }
}