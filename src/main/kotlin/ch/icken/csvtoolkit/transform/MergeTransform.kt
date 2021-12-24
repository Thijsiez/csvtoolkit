package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import ch.icken.csvtoolkit.set
import ch.icken.csvtoolkit.ui.Spinner
import ch.icken.csvtoolkit.ui.VerticalDivider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.max

class MergeTransform : Transform(), TransformCustomStateContent {
    override val description get() = buildAnnotatedString {
        append(mergeType.uiName)
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(if (numberOfColumnsSelected == mergeColumns.size) "all" else numberOfColumnsSelected.toString())
        }
        append(" of ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(mergeWithFile?.name ?: "?")
        }
        append('\'')
        if (mergeWithFile?.name?.endsWith('s') == false) append('s')
        append(" columns")
    }

    private var mergeType by mutableStateOf(Type.RANDOM)
    private var mergeWithFile: TabulatedFile? by mutableStateOf(null)
    private val mergeColumns = mutableStateListOf<Pair<String, Boolean>>()
    private val numberOfColumnsSelected by derivedStateOf { mergeColumns.count { it.second } }
    private val keepColumns by derivedStateOf { mergeColumns.filter { it.second }.map { it.first } }

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        if (numberOfColumnsSelected == 0) return intermediate
        return intermediate.apply {
            addAll(keepColumns)
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        if (numberOfColumnsSelected == 0) return@coroutineScope intermediate

        val mergeData = mergeWithFile?.letData { data ->
            data.mapTo(ArrayList(max(data.size, intermediate.size))) {
                it.filter { (columnName, _) -> columnName in keepColumns }
            }.apply {
                if (size < intermediate.size) {
                    val initialSizeData = ArrayList(this)
                    repeat(intermediate.size / size - 1) { addAll(initialSizeData) }
                    addAll(initialSizeData.subList(0, intermediate.size % size))
                }
                if (mergeType == Type.RANDOM) shuffle()
            }
        } ?: return@coroutineScope intermediate

        val chunkSize = chunkSize(intermediate.size)
        return@coroutineScope intermediate.chunked(chunkSize).mapIndexed { chunkIndex, chunk ->
            async {
                chunk.onEachIndexed { rowIndex, row ->
                    row.putAll(mergeData[chunkIndex * chunkSize + rowIndex])
                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val mergeWithFileValue = mergeWithFile

        if (mergeWithFileValue == null) {
            invalidMessage = "Missing file to merge with"
            return false
        }
        if (mergeWithFileValue !in instance.files) {
            invalidMessage = "File to merge with not available"
            return false
        }
        if (mergeColumns.any { (columnName, _) -> columnName !in mergeWithFileValue.headers }) {
            invalidMessage = "Column not available"
        }

        return super.isValid(instance)
    }

    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        val scrollState = rememberLazyListState()

        EditDialog(
            titleText = "Merge",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(480.dp, Dp.Unspecified)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spinner(
                        items = Type.values().asList(),
                        selectedItem = { mergeType },
                        onItemSelected = { mergeType = it },
                        itemTransform = { it.uiName },
                        label = "Merge Type"
                    )
                    Spinner(
                        items = instance.files,
                        selectedItem = { mergeWithFile },
                        onItemSelected = { file ->
                            mergeWithFile = file
                            mergeColumns.clear()
                            if (file != null) {
                                mergeColumns.addAll(file.headers.map { it to true })
                            }
                        },
                        itemTransform = { it?.name ?: "-" },
                        label = "Merge File"
                    )
                    Text(
                        text = "Data will be merged more than once " +
                                "if not enough unique rows are available, round-robin-style",
                        modifier = Modifier.requiredWidth(180.dp),
                        style = MaterialTheme.typography.caption
                    )
                }
                VerticalDivider()
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(),
                        state = scrollState
                    ) {
                        itemsIndexed(mergeColumns) { index, (columnName, checked) ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .height(40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        mergeColumns.set(index) { it.copy(second = isChecked) }
                                    }
                                )
                                Text(
                                    text = columnName,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }

    @Composable
    override fun CustomStateContent(
        instance: ToolkitInstance
    ) {
        when (numberOfColumnsSelected) {
            0 -> TransformWarningIcon("Will be skipped")
            else -> DefaultTransformStateContent(instance, this)
        }
    }

    private enum class Type(
        val uiName: String
    ) {
        REGULAR("Merge"),
        RANDOM("Random Merge")
    }
}