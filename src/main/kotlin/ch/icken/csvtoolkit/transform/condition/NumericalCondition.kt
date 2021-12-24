package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
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
import ch.icken.csvtoolkit.transform.EditDialog
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.ui.Spinner

class NumericalCondition(
    parentTransform: ConditionParentTransform,
    parentCondition: ConditionParent?
) : Condition(parentTransform, parentCondition) {
    override val description get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" is ")
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
    private val compareDouble = derivedStateOf { compareTo.text.toDoubleOrNull() ?: Double.NaN }
    private val valueInvalidCharacters = Regex("[^0-9.]")

    override fun check(row: Map<String, String>): Boolean {
        val columnName = column ?: return false
        val referenceDouble = row[columnName]?.toDoubleOrNull() ?: return false
        return when (compareType) {
            Type.EQ -> referenceDouble == compareDouble.value
            Type.NEQ -> referenceDouble != compareDouble.value
            Type.LT -> referenceDouble < compareDouble.value
            Type.GT -> referenceDouble > compareDouble.value
            Type.LTE -> referenceDouble <= compareDouble.value
            Type.GTE -> referenceDouble >= compareDouble.value
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
            titleText = "Numerical Condition",
            onHide = onHide,
            onDelete = onDelete,
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
                    selectedItem = { column },
                    onItemSelected = { column = it },
                    itemTransform = { it ?: "-" },
                    label = "Reference Column"
                )
                Text("is")
                Spinner(
                    items = Type.values().asList(),
                    selectedItem = { compareType },
                    onItemSelected = { compareType = it },
                    itemTransform = { it.uiName },
                    label = "Comparison Type"
                )
                OutlinedTextField(
                    value = compareTo,
                    onValueChange = {
                        compareTo = it.copy(
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