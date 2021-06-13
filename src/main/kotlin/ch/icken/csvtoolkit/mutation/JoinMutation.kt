package ch.icken.csvtoolkit.mutation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.files.TabulatedFile
import ch.icken.csvtoolkit.ui.CheckButton
import ch.icken.csvtoolkit.ui.Spinner

class JoinMutation : Mutation(Type.JOIN) {
    override val description get() = buildAnnotatedString {
        append(if (innerJoin.value) "Inner join " else "Join ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column.value ?: "?")
        }
        append(" on ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(joinOnFile.value?.name ?: "?")
        }
        append("'s ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(joinOnColumn.value ?: "?")
        }
    }

    private val column: MutableState<String?> = mutableStateOf(null)
    private val innerJoin: MutableState<Boolean> = mutableStateOf(false)
    private val joinOnFile: MutableState<TabulatedFile?> = mutableStateOf(null)
    private val joinOnColumn: MutableState<String?> = mutableStateOf(null)

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        val joinOnFileValue = joinOnFile.value
        val joinOnColumnName = joinOnColumn.value

        if (joinOnFileValue == null ||
            joinOnColumnName == null) return intermediate

        return intermediate.apply {
            addAll(joinOnFileValue.headers.filterNot { it == joinOnColumnName })
        }
    }

    override fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> {
        val columnName = column.value
        val joinOnFileValue = joinOnFile.value
        val joinOnColumnName = joinOnColumn.value

        if (columnName == null ||
            joinOnFileValue == null ||
            joinOnColumnName == null) return intermediate

        return intermediate.onEach { row ->
            val matchValue = row[columnName]
            val joinData = joinOnFileValue.letData { data ->
                data.firstOrNull { it[joinOnColumnName] == matchValue }
            }
            val joinEmpty = joinOnFileValue.headers.associateWith { "" }
            //TODO implement left/inner join option checkbox
            row.putAll((joinData ?: joinEmpty).filterNot { (key, _) -> key == joinOnColumnName})
        }
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val columnName = column.value
        val joinOnFileValue = joinOnFile.value
        val joinOnColumnName = joinOnColumn.value
        return columnName != null &&
                joinOnFileValue != null &&
                joinOnColumnName != null &&
                columnName in instance.headersUpTo(this) &&
                joinOnFileValue in instance.files &&
                joinOnColumnName in joinOnFileValue.headers &&
                super.isValid(instance)
    }

    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit
    ) {
        val dialogState = rememberDialogState(
            size = WindowSize(480.dp, 240.dp)
        )

        MutationEditDialog(
            titleText = "Join",
            onHide = onHide,
            state = dialogState
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Spinner(
                        items = instance.headersUpTo(this@JoinMutation),
                        itemTransform = { Text(it) },
                        onItemSelected = { column.value = it },
                        modifier = Modifier.requiredWidth(180.dp)
                    ) {
                        Text(column.value ?: "-")
                    }
                    Spacer(Modifier.height(8.dp))
                    CheckButton(
                        checked = innerJoin.value,
                        onCheckedChange = { innerJoin.value = it }
                    ) {
                        Text("Inner Join")
                    }
                }
                Text("ON")
                Column {
                    Spinner(
                        items = instance.files,
                        itemTransform = { Text(it.name) },
                        onItemSelected = { joinOnFile.value = it },
                        modifier = Modifier.requiredWidth(180.dp)
                    ) {
                        Text(joinOnFile.value?.name ?: "-")
                    }
                    Spacer(Modifier.height(8.dp))
                    Spinner(
                        items = joinOnFile.value?.headers ?: emptyList(),
                        itemTransform = { Text(it) },
                        onItemSelected = { joinOnColumn.value = it },
                        modifier = Modifier.requiredWidth(180.dp),
                        enabled = joinOnFile.value != null
                    ) {
                        Text(joinOnColumn.value ?: "-")
                    }
                }
            }
        }
    }
}