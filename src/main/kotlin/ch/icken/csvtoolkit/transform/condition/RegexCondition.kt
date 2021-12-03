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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform.Context
import ch.icken.csvtoolkit.transform.TransformEditDialog
import ch.icken.csvtoolkit.ui.Spinner

class RegexCondition(parent: Transform) : Condition(parent) {
    override val description: AnnotatedString get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column.value ?: "?")
        }
        append(" matches ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(compareTo.value.text)
        }
    }

    private val column: MutableState<String?> = mutableStateOf(null)
    private val compareTo: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    private val compareRegex = derivedStateOf { Regex(compareTo.value.text) }

    override fun check(row: Map<String, String>): Boolean {
        val columnName = column.value ?: return false
        val referenceValue = row[columnName] ?: return false
        return referenceValue.matches(compareRegex.value)
    }

    @Composable
    override fun Dialog(
        context: Context,
        onHide: () -> Unit
    ) {
        TransformEditDialog(
            titleText = "RegEx Condition",
            onHide = onHide,
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
                    onItemSelected = { column.value = it },
                    label = "Reference Column"
                ) {
                    Text(column.value ?: "-")
                }
                Text("matches")
                OutlinedTextField(
                    value = compareTo.value,
                    onValueChange = { compareTo.value = it },
                    modifier = Modifier.padding(bottom = 8.dp),
                    label = { Text("RegEx") },
                    placeholder = { Text("[0-9a-f]{36}") },
                    singleLine = true
                )
            }
        }
    }
}