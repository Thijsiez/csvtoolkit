package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.set
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SelectTransform : Transform() {
    override val description get() = buildAnnotatedString {

    }

    private val selectColumns = mutableStateListOf<Pair<String, Boolean>>()

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {

        return intermediate.apply {

        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {

        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                chunk.onEach { row ->

                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {


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
            state = rememberDialogState(
                size = DpSize(360.dp, Dp.Unspecified)
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
                    .height(240.dp),
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
                                selectColumns.set(index) { it.copy(second = isChecked) }
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