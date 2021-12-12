package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.lowercaseIf
import ch.icken.csvtoolkit.onEach
import ch.icken.csvtoolkit.ui.Spinner
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class JoinTransform : Transform() {
    override val description get() = buildAnnotatedString {
        append(joinType.uiName)
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" on ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(joinOnFile?.name ?: "?")
        }
        append("'s ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(joinOnColumn ?: "?")
        }
    }

    private var column: String? by mutableStateOf(null)
    private var joinType by mutableStateOf(Type.INNER)
    private var joinOnFile: TabulatedFile? by mutableStateOf(null)
    private var joinOnColumn: String? by mutableStateOf(null)
    private var caseInsensitive by mutableStateOf(false)

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        val joinOnFileValue = joinOnFile
        val joinOnColumnName = joinOnColumn

        if (joinOnFileValue == null ||
            joinOnColumnName == null) return intermediate

        return intermediate.apply {
            addAll(joinOnFileValue.headers.filterNot { it == joinOnColumnName })
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        val columnName = column
        val joinOnFileValue = joinOnFile
        val joinOnColumnName = joinOnColumn

        if (columnName == null ||
            joinOnFileValue == null ||
            joinOnColumnName == null) return@coroutineScope intermediate

        val joinDataLookup = joinOnFileValue.letData { data ->
            data.associate {
                val key = it[joinOnColumnName]?.lowercaseIf { caseInsensitive }
                key to it.filterNot { (columnName, _) -> columnName == joinOnColumnName }
            }
        }
        val joinLeftEmpty = joinOnFileValue.headers.filterNot { it == joinOnColumnName }.associateWith { "" }
        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                when (joinType) {
                    Type.INNER -> {
                        (chunk as MutableList).onEach { row, iterator ->
                            val joinData = joinDataLookup?.get(row[columnName]?.lowercaseIf { caseInsensitive })
                            if (joinData != null) row.putAll(joinData) else iterator.remove()
                        }
                    }
                    Type.LEFT -> {
                        chunk.onEach { row ->
                            val joinData = joinDataLookup?.get(row[columnName]?.lowercaseIf { caseInsensitive })
                            row.putAll(joinData ?: joinLeftEmpty)
                        }
                    }
                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val columnName = column
        val joinOnFileValue = joinOnFile
        val joinOnColumnName = joinOnColumn

        if (columnName == null) {
            invalidMessage = "Missing left column"
            return false
        }
        if (joinOnFileValue == null) {
            invalidMessage = "Missing file to join on"
            return false
        }
        if (joinOnColumnName == null) {
            invalidMessage = "Missing right column"
            return false
        }
        if (columnName !in instance.headersUpTo(this)) {
            invalidMessage = "Left column not available"
            return false
        }
        if (joinOnFileValue !in instance.files) {
            invalidMessage = "File to join on not available"
            return false
        }
        if (joinOnColumnName !in joinOnFileValue.headers) {
            invalidMessage = "Right column not available"
            return false
        }

        return super.isValid(instance)
    }

    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit
    ) {
        EditDialog(
            titleText = "Join",
            onHide = onHide,
            state = rememberDialogState(
                size = DpSize(480.dp, Dp.Unspecified)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spinner(
                            items = instance.headersUpTo(this@JoinTransform),
                            itemTransform = { Text(it) },
                            onItemSelected = { column = it },
                            label = "Reference Column"
                        ) {
                            Text(column ?: "-")
                        }
                        Spinner(
                            items = Type.values().toList(),
                            itemTransform = { Text(it.uiName) },
                            onItemSelected = { joinType = it },
                            label = "Join Type"
                        ) {
                            Text(joinType.uiName)
                        }
                    }
                    Text("ON")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spinner(
                            items = instance.files,
                            itemTransform = { Text(it.name) },
                            onItemSelected = { joinOnFile = it },
                            label = "Join File"
                        ) {
                            Text(joinOnFile?.name ?: "-")
                        }
                        Spinner(
                            items = joinOnFile?.headers ?: emptyList(),
                            itemTransform = { Text(it) },
                            onItemSelected = { joinOnColumn = it },
                            label = "Join Column",
                            enabled = joinOnFile != null
                        ) {
                            Text(joinOnColumn ?: "-")
                        }
                    }
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
        INNER("Inner Join"),
        LEFT("Left Join")
    }
}