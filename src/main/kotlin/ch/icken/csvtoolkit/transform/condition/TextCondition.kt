package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.TransformEditDialog
import ch.icken.csvtoolkit.ui.Spinner

class TextCondition(parent: Transform) : Condition(parent) {
    override val description: AnnotatedString get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column.value ?: "?")
        }
        append(" ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(compareType.value.uiName.lowercase())
        }
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(compareValue.value.text)
        }
    }

    private val column: MutableState<String?> = mutableStateOf(null)
    private val compareType: MutableState<Type> = mutableStateOf(Type.EQ)
    private val compareValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))

    override fun check(row: Map<String, String>): Boolean {
        val columnName = column.value ?: return false
        val referenceValue = row[columnName] ?: return false
        val compareText = compareValue.value.text
        return when(compareType.value) {
            Type.EQ -> referenceValue == compareText
            Type.NEQ -> referenceValue != compareText
            Type.SW -> referenceValue.startsWith(compareText)
            Type.EW -> referenceValue.endsWith(compareText)
            Type.C -> referenceValue.contains(compareText)
            Type.NC -> !referenceValue.contains(compareText)
        }
    }

    @Composable
    override fun Dialog(
        context: ConditionalTransform.Context,
        onHide: () -> Unit
    ) {
        TransformEditDialog(
            titleText = "Text Condition",
            onHide = onHide,
            state = rememberDialogState(
                size = DpSize(720.dp, 210.dp)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spinner(
                    items = context.headers,
                    itemTransform = { Text(it) },
                    onItemSelected = { column.value = it },
                    label = "Reference Column"
                ) {
                    Text(column.value ?: "-")
                }
                Text("is")
                Spinner(
                    items = Type.values().toList(),
                    itemTransform = { Text(it.uiName) },
                    onItemSelected = { compareType.value = it },
                    label = "Comparison Type"
                ) {
                    Text(compareType.value.uiName)
                }
                OutlinedTextField(
                    value = compareValue.value,
                    onValueChange = { compareValue.value = it },
                    modifier = Modifier.padding(bottom = 8.dp),
                    label = { Text("Comparison Value") },
                    singleLine = true
                )
            }
        }
    }

    private enum class Type(
        val uiName: String
    ) {
        EQ("Is equal to"),
        NEQ("Is not equal to"),
        SW("Starts with"),
        EW("Ends with"),
        C("Contains"),
        NC("Does not contain")
    }
}