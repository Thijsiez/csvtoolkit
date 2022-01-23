package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.set
import ch.icken.csvtoolkit.transform.SelectTransform.SelectSerializer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SelectSerializer::class)
class SelectTransform() : Transform(), TransformCustomStateContent {
    override val description get() = buildAnnotatedString {
        when {
            numberOfColumnsSelected == selectColumns.size -> append("Keep all")
            numberOfColumnsSelected == 0 -> append("Drop all")
            numberOfColumnsSelected > selectColumns.size / 2 -> {
                append("Drop ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append((selectColumns.size - numberOfColumnsSelected).toString())
                }
            }
            else -> {
                append("Keep ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(numberOfColumnsSelected.toString())
                }
            }
        }
        append(" column")
        if (numberOfColumnsSelected != 1) append('s')
    }
    override val surrogate get() = SelectSurrogate(selectColumns)

    private val selectColumns = mutableStateListOf<Select>()
    private val numberOfColumnsSelected by derivedStateOf { selectColumns.count { it.select } }
    private val keepColumns by derivedStateOf { selectColumns.filter { it.select }.map { it.columnName } }

    constructor(surrogate: SelectSurrogate) : this() {
        selectColumns.clear()
        selectColumns.addAll(surrogate.selectColumns)
    }

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        if (numberOfColumnsSelected == selectColumns.size) return intermediate
        if (numberOfColumnsSelected == 0) return mutableListOf()
        return intermediate.apply {
            removeAll { it !in keepColumns }
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        if (numberOfColumnsSelected == selectColumns.size) return@coroutineScope intermediate
        if (numberOfColumnsSelected == 0) return@coroutineScope mutableListOf()
        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                chunk.onEach { row ->
                    row.keys.removeAll { it !in keepColumns }
                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val incomingColumnNames = instance.headersUpTo(this)
        val columnNames = selectColumns.map { it.columnName }

        //maybe update columns here and not report these two errors
        //but I'd rather have the user give this a look and check themselves
        if (!incomingColumnNames.all { it in columnNames }) {
            invalidMessage = "Select is under-defined"
            return false
        }
        if (!columnNames.all { it in incomingColumnNames }) {
            invalidMessage = "Select is over-defined"
            return false
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
            titleText = "Select",
            onHide = onHide,
            onDelete = onDelete,
            onOpen = {
                val retain = selectColumns.associate { it.columnName to it.select }
                selectColumns.clear()
                instance.headersUpTo(this).forEach { columnName ->
                    selectColumns.add(Select(columnName, retain[columnName] ?: true))
                }
            },
            state = rememberDialogState(
                size = DpSize(360.dp, Dp.Unspecified)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Columns",
                    fontWeight = FontWeight.Bold
                )
                Box {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(),
                        state = scrollState
                    ) {
                        itemsIndexed(selectColumns) { index, (columnName, checked) ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .height(40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectColumns.set(index) { it.copy(select = isChecked) }
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
            selectColumns.size -> TransformWarningIcon("Will be skipped")
            0 -> TransformWarningIcon("No data will pass this transform")
            else -> DefaultTransformStateContent(instance, this)
        }
    }

    @Serializable
    data class Select(
        val columnName: String,
        val select: Boolean
    )

    @Serializable
    @SerialName("select")
    class SelectSurrogate(
        val selectColumns: List<Select>
    ) : TransformSurrogate
    object SelectSerializer : KSerializer<SelectTransform> {
        override val descriptor = SelectSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SelectTransform) {
            encoder.encodeSerializableValue(SelectSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): SelectTransform {
            return SelectTransform(decoder.decodeSerializableValue(SelectSurrogate.serializer()))
        }
    }
}