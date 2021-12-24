package ch.icken.csvtoolkit.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.calculateOffset
import kotlin.math.round

@Composable
fun ListTable(
    data: List<List<String>>,
    cell: @Composable (row: Int, column: String, value: String) -> Unit = { _, _, value ->
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 8.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
) {
    if (data.isEmpty()) return
    val columnNamesAndWidths = data.first().map { it to mutableStateOf(DefaultColumnWidth) }
    Table(columnNamesAndWidths, data.subList(1, data.size)) { row, rowData ->
        columnNamesAndWidths.forEachIndexed { index, (columnName, width) ->
            Box(
                modifier = Modifier.requiredWidth(width.value.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                cell(row, columnName, rowData[index])
                VerticalDivider(Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

@Composable
fun <T> MapTable(
    data: List<Map<String, T>>,
    cell: @Composable (row: Int, column: String, value: T) -> Unit = { _, _, value ->
        Text(
            text = value.toString(),
            modifier = Modifier.padding(horizontal = 8.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
) {
    if (data.isEmpty()) return
    val columnNamesAndWidths = data.first().keys.map { it to mutableStateOf(DefaultColumnWidth) }
    Table(columnNamesAndWidths, data) { row, rowData ->
        columnNamesAndWidths.forEach { (columnName, width) ->
            Box(
                modifier = Modifier.requiredWidth(width.value.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                cell(row, columnName, rowData[columnName]!!)
                VerticalDivider(Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

private const val DefaultColumnWidth = 112f
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun <C> Table(
    headers: List<Pair<String, MutableState<Float>>>,
    data: List<C>,
    rowHoverColor: Color = MaterialTheme.colors.onSurface.copy(alpha = .06f),
    row: @Composable RowScope.(row: Int, rowData: C) -> Unit
) = Box {
    val horizontalState = rememberLazyListState()
    val verticalState = rememberLazyListState()

    Column {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.body2) {
            LazyRow(
                modifier = Modifier.height(28.dp),
                state = horizontalState,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(headers) { (columnName, width) ->
                    Box(
                        modifier = Modifier.requiredWidth(width.value.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = columnName,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        VerticalDivider(
                            modifier = Modifier.align(Alignment.CenterEnd)
                                .draggable(
                                    state = rememberDraggableState { delta ->
                                        width.value += delta
                                    },
                                    orientation = Orientation.Horizontal
                                )
                                .pointerHoverIcon(PointerIconDefaults.Hand)
                        )
                    }
                }
            }
            Divider(color = Color.Black)
            BoxWithConstraints {
                val parentWidth = with(LocalDensity.current) { maxWidth.toPx() }
                LazyColumn(
                    state = verticalState
                ) {
                    val itemWidths = headers.map { it.second.value }
                    val rowWidth = itemWidths.reduce { total, width -> total + width }
                    val offsetCorrection = if (rowWidth > parentWidth) (rowWidth - parentWidth) / 2 else 0f
                    itemsIndexed(data) { index, item ->
                        var isHovered by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.requiredWidth(rowWidth.dp)
                                .height(26.dp)
                                .offset(round(-horizontalState.calculateOffset(itemWidths) + offsetCorrection).dp)
                                .background(if (isHovered) rowHoverColor else Color.Transparent)
                                .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                                .onPointerEvent(PointerEventType.Enter) { isHovered = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row(index, item)
                        }
                    }
                }
            }
        }
    }
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(verticalState),
        modifier = Modifier.align(Alignment.CenterEnd)
    )
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(horizontalState),
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}