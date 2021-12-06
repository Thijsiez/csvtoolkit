package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.TransformEditDialog
import ch.icken.csvtoolkit.ui.Spinner

class NumericalCondition(parent: Transform) : Condition(parent) {
    override val description: AnnotatedString get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column.value ?: "?")
        }
        append(" is ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(compareType.value.uiName.lowercase())
        }
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(compareTo.value.text)
        }
    }

    private val column: MutableState<String?> = mutableStateOf(null)
    private val compareType: MutableState<Type> = mutableStateOf(Type.EQ)
    private val compareTo: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    private val compareDouble = derivedStateOf { compareTo.value.text.toDoubleOrNull() ?: Double.NaN }
    private val valueInvalidCharacters = Regex("[^0-9.]")

    override fun check(row: Map<String, String>): Boolean {
        val columnName = column.value ?: return false
        val referenceDouble = row[columnName]?.toDoubleOrNull() ?: return false
        return when (compareType.value) {
            Type.EQ -> referenceDouble == compareDouble.value
            Type.NEQ -> referenceDouble != compareDouble.value
            Type.LT -> referenceDouble < compareDouble.value
            Type.GT -> referenceDouble > compareDouble.value
            Type.LTE -> referenceDouble <= compareDouble.value
            Type.GTE -> referenceDouble >= compareDouble.value
        }
    }

    @Composable
    override fun Dialog(
        context: ConditionalTransform.Context,
        onHide: () -> Unit
    ) {
        TransformEditDialog(
            titleText = "Numerical Condition",
            onHide = onHide,
            state = rememberDialogState(
                size = DpSize(640.dp, Dp.Unspecified)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    value = compareTo.value,
                    onValueChange = {
                        compareTo.value = it.copy(
                            text = it.text.replace(valueInvalidCharacters, "")
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    label = { Text("Number") },
                    placeholder = { Text("3.14") },
                    singleLine = true
                )
            }
        }
    }

    private enum class Type(
        val uiName: String
    ) {
        EQ("Equal to"),
        NEQ("Not equal to"),
        LT("Less than"),
        GT("Greater than"),
        LTE("Less than or equals"),
        GTE("Greater than or equals")
    }
}