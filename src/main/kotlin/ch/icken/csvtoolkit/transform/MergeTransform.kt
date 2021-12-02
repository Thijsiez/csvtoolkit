package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.set
import ch.icken.csvtoolkit.ui.Spinner
import ch.icken.csvtoolkit.ui.VerticalDivider
import kotlinx.coroutines.coroutineScope
import kotlin.math.max

class MergeTransform : Transform() {
    override val description get() = buildAnnotatedString {
        append(mergeType.value.uiName)
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            val numberOfColumns = mergeColumns.count { (_, merge) -> merge }
            append(if (numberOfColumns == mergeColumns.size) "all" else numberOfColumns.toString())
        }
        append(" of ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(mergeWithFile.value?.name ?: "?")
        }
        append("'s columns")
    }

    private val mergeType: MutableState<Type> = mutableStateOf(Type.RANDOM)
    private val mergeWithFile: MutableState<TabulatedFile?> = mutableStateOf(null)
    private val mergeColumns = mutableStateListOf<Pair<String, Boolean>>()

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        return intermediate.apply {
            mergeColumns.forEach { (columnName, merge) ->
                if (merge) add(columnName)
            }
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        val mergeData = mergeWithFile.value?.letData { data ->
            val keepColumns = mergeColumns.filter { (_, merge) -> merge }.map { (columnName, _) -> columnName }
            data.mapTo(ArrayList(max(data.size, intermediate.size))) {
                it.filter { (columnName, _) -> columnName in keepColumns }
            }.apply {
                if (size < intermediate.size) {
                    val initialSizeData = ArrayList(this)
                    repeat(intermediate.size / size - 1) { addAll(initialSizeData) }
                    addAll(initialSizeData.subList(0, intermediate.size % size))
                }
                if (mergeType.value == Type.RANDOM) shuffle()
            }
        } ?: return@coroutineScope intermediate
        //TODO use coroutines
        return@coroutineScope intermediate.onEachIndexed { index, row ->
            row.putAll(mergeData[index])
        }
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val mergeWithFileValue = mergeWithFile.value

        if (mergeWithFileValue == null) {
            invalidMessage = "Missing file to merge with"
            return false
        }
        if (mergeWithFileValue !in instance.files) {
            invalidMessage = "File to merge with not available"
            return false
        }
        if (mergeColumns.all { (_, merge) -> !merge }) {
            invalidMessage = "No columns selected to merge"
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
        onHide: () -> Unit
    ) {
        val scrollState = rememberLazyListState()

        TransformEditDialog(
            titleText = "Merge",
            onHide = onHide,
            state = rememberDialogState(
                size = DpSize(480.dp, 360.dp)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spinner(
                        items = Type.values().toList(),
                        itemTransform = { Text(it.uiName) },
                        onItemSelected = { mergeType.value = it },
                        label = "Merge Type"
                    ) {
                        Text(mergeType.value.uiName)
                    }
                    Spinner(
                        items = instance.files,
                        itemTransform = { Text(it.name) },
                        onItemSelected = { file ->
                            mergeWithFile.value = file
                            mergeColumns.clear()
                            mergeColumns.addAll(file.headers.map { it to true })
                        },
                        label = "Merge File"
                    ) {
                        Text(mergeWithFile.value?.name ?: "-")
                    }
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

    private enum class Type(
        val uiName: String
    ) {
        REGULAR("Merge"),
        RANDOM("Random Merge")
    }
}