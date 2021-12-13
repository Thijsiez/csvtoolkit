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

class RegexCondition(
    parentTransform: ConditionParentTransform,
    parentCondition: ConditionParent?
) : Condition(parentTransform, parentCondition) {
    override val description get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" matches ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(compareTo.text)
        }
    }

    private var column: String? by mutableStateOf(null)
    private var compareTo by mutableStateOf(TextFieldValue(""))
    private val compareRegex = derivedStateOf { Regex(compareTo.text) }

    override fun check(row: Map<String, String>): Boolean {
        val columnName = column ?: return false
        val referenceValue = row[columnName] ?: return false
        return referenceValue.matches(compareRegex.value)
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
            titleText = "RegEx Condition",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(480.dp, Dp.Unspecified)
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
                    onItemSelected = { column = it },
                    label = "Reference Column"
                ) {
                    Text(column ?: "-")
                }
                Text("matches")
                OutlinedTextField(
                    value = compareTo,
                    onValueChange = { compareTo = it },
                    modifier = Modifier.padding(bottom = 8.dp),
                    label = { Text("RegEx") },
                    placeholder = { Text("[0-9a-f]{36}") },
                    singleLine = true
                )
            }
        }
    }
}