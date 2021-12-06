package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
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
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.TransformEditDialog
import ch.icken.csvtoolkit.ui.Spinner

class TextCondition(parent: Transform) : Condition(parent) {
    override val description: AnnotatedString get() = buildAnnotatedString {
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

    private var column: String? by mutableStateOf(null)
    private var compareType by mutableStateOf(Type.EQ)
    private var compareTo by mutableStateOf(TextFieldValue(""))
    private val compareText = derivedStateOf { compareTo.text.lowercaseIf { caseInsensitive } }
    private var caseInsensitive by mutableStateOf(false)

    override fun check(row: Map<String, String>): Boolean {
        val columnName = column ?: return false
        val referenceText = row[columnName]?.lowercaseIf { caseInsensitive } ?: return false
        return when (compareType) {
            Type.EQ -> referenceText == compareText.value
            Type.NEQ -> referenceText != compareText.value
            Type.SW -> referenceText.startsWith(compareText.value)
            Type.EW -> referenceText.endsWith(compareText.value)
            Type.C -> referenceText.contains(compareText.value)
            Type.NC -> !referenceText.contains(compareText.value)
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
                        itemTransform = { Text(it) },
                        onItemSelected = { column = it },
                        label = "Reference Column"
                    ) {
                        Text(column ?: "-")
                    }
                    Spinner(
                        items = Type.values().toList(),
                        itemTransform = { Text(it.uiName) },
                        onItemSelected = { compareType = it },
                        label = "Comparison Type"
                    ) {
                        Text(compareType.uiName)
                    }
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