package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.ui.Spinner
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SetTransform(parent: Transform?) : ConditionalTransform(parent) {
    override val description get() = buildAnnotatedString {
        append("Set ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" to ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(setValue.text)
        }
    }

    private var column: String? by mutableStateOf(null)
    private var setValue by mutableStateOf(TextFieldValue(""))

    override fun doTheHeaderThing(intermediate: MutableList<String>) = intermediate

    override fun doTheConditionalHeaderThing(intermediate: MutableList<String>) = intermediate

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        val columnName = column
        if (columnName == null ||
            intermediate.firstOrNull()?.containsKey(columnName) == false) return@coroutineScope intermediate
        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                chunk.onEach { it[columnName] = setValue.text }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun doTheConditionalThing(intermediateRow: MutableMap<String, String>): MutableMap<String, String> {
        return intermediateRow.apply {
            val columnName = column
            if (columnName == null || !containsKey(columnName)) return@apply
            set(columnName, setValue.text)
        }
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val columnName = column

        if (columnName == null) {
            invalidMessage = "Missing reference column"
            return false
        }
        if (columnName !in instance.headersUpTo(this)) {
            invalidMessage = "Reference column not available"
            return false
        }

        return super.isValid(instance)
    }

    override fun isValidConditional(context: Condition.Context): Boolean {
        return column in context.headers
    }

    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        Dialog(
            headers = instance.headersUpTo(this),
            onHide = onHide,
            onDelete = onDelete
        )
    }

    @Composable
    override fun ConditionalDialog(
        context: Condition.Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        Dialog(
            headers = context.headers,
            onHide = onHide,
            onDelete = onDelete
        )
    }

    @Composable
    private fun Dialog(
        headers: List<String>,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        EditDialog(
            titleText = "Set",
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
                Text("Set")
                Spinner(
                    items = headers,
                    itemTransform = { Text(it) },
                    onItemSelected = { column = it },
                    label = "Reference Column"
                ) {
                    Text(column ?: "-")
                }
                Text("to")
                OutlinedTextField(
                    value = setValue,
                    onValueChange = { setValue = it },
                    modifier = Modifier.padding(bottom = 8.dp),
                    label = { Text("Value") },
                    singleLine = true
                )
            }
        }
    }
}