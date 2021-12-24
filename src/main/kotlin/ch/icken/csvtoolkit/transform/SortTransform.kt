package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import ch.icken.csvtoolkit.ui.Spinner
import ch.icken.csvtoolkit.ui.Tooltip
import kotlinx.coroutines.coroutineScope

class SortTransform : Transform() {
    override val description get() = buildAnnotatedString {
        append("Sort by ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(if (ascending) ", ascending" else ", descending")
    }

    private var column: String? by mutableStateOf(null)
    private var ascending by mutableStateOf(true)

    override fun doTheHeaderThing(intermediate: MutableList<String>) = intermediate

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        val columnName = column
        if (columnName == null ||
            intermediate.firstOrNull()?.containsKey(columnName) == false) return@coroutineScope intermediate
        return@coroutineScope intermediate.apply {
            val selector: (MutableMap<String, String>) -> String? = { it[columnName]?.lowercase() }
            if (ascending) sortBy(selector) else sortByDescending(selector)
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        EditDialog(
            titleText = "Sort",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(360.dp, Dp.Unspecified)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort by")
                Spinner(
                    items = instance.headersUpTo(this@SortTransform),
                    selectedItem = { column },
                    onItemSelected = { column = it },
                    itemTransform = { it ?: "-" },
                    label = "Reference Column"
                )
                TooltipArea(
                    tooltip = { Tooltip(if (ascending) "Ascending" else "Descending") }
                ) {
                    IconToggleButton(
                        checked = ascending,
                        onCheckedChange = { ascending = it }
                    ) {
                        Icon(
                            imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = if (ascending) "Ascending" else "Descending"
                        )
                    }
                }
            }
        }
    }
}