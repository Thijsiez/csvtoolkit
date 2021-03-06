package ch.icken.csvtoolkit.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

@Composable
fun ListTable(
    data: State<List<List<String>>>,
    cell: @Composable (row: Int, column: String, value: String) -> Unit = { _, _, value ->
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 8.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
) {
    if (data.value.isEmpty() || data.value.first().isEmpty()) return
    val columnNamesAndWidths by derivedStateOf {
        data.value.first().map { it to mutableStateOf(DefaultColumnWidth) }
    }
    Table(columnNamesAndWidths, data.value.subList(1, data.value.size)) { row, rowData ->
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
    data: State<List<Map<String, T>>>,
    cell: @Composable (row: Int, column: String, value: T) -> Unit = { _, _, value ->
        Text(
            text = value.toString(),
            modifier = Modifier.padding(horizontal = 8.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
) {
    if (data.value.isEmpty() || data.value.first().isEmpty()) return
    val columnNamesAndWidths by derivedStateOf {
        data.value.first().keys.map { it to mutableStateOf(DefaultColumnWidth) }
    }
    Table(columnNamesAndWidths, data.value) { row, rowData ->
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
    rowHoverColor: Color = MaterialTheme.colors.onSurface.copy(alpha = .04f),
    row: @Composable RowScope.(row: Int, rowData: C) -> Unit
) = Box {
    val horizontalState = rememberLazyListState()
    val verticalState = rememberLazyListState()
    val localDensity = LocalDensity.current.density
    val rowNumberColumnWidth = data.size.length() * 10 + 16

    Row {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.body2) {
            Surface(
                //Draw the row numbers above the headers and data
                modifier = Modifier.zIndex(2f),
                elevation = if (!horizontalState.isHome) 8.dp else 2.dp
            ) {
                Column(
                    modifier = Modifier.requiredWidth(rowNumberColumnWidth.dp)
                ) {
                    Spacer(Modifier.height(28.dp))
                    Divider(color = Color.Black)
                    LazyColumn(
                        //Sync the scroll with the data's vertical scroll
                        state = LazyListState(
                            verticalState.firstVisibleItemIndex,
                            verticalState.firstVisibleItemScrollOffset
                        )
                    ) {
                        items(data.size) { index ->
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .height(26.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colors.onSurface.copy(alpha = .38f)
                                )
                            }
                        }
                    }
                }
                //Dirty trick to intercept scroll from the LazyColumn
                //TODO remove when `userScrollEnabled` is added
                Box(
                    modifier = Modifier.width(rowNumberColumnWidth.dp)
                        .fillMaxHeight()
                        .verticalScroll(ScrollState(0))
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    //Draw the headers above the data
                    modifier = Modifier.zIndex(1f),
                    elevation = if (!verticalState.isHome) 8.dp else 2.dp
                ) {
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
                                                //width is in Dp, delta is in Px, convert using density
                                                width.value += delta / localDensity
                                            },
                                            orientation = Orientation.Horizontal
                                        )
                                        .pointerHoverIcon(PointerIconDefaults.Hand)
                                )
                            }
                        }
                    }
                }
                Divider(color = Color.Black)
                BoxWithConstraints {
                    val parentWidth = maxWidth.value
                    LazyColumn(
                        state = verticalState
                    ) {
                        val itemWidths = headers.map { it.second.value }
                        val rowWidth = itemWidths.reduce { total, width -> total + width }
                        val offset = round(-horizontalState.calculateOffset(itemWidths, localDensity) +
                                if (rowWidth > parentWidth) (rowWidth - parentWidth) / 2 else 0f)
                        itemsIndexed(data) { index, item ->
                            var isHovered by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.requiredWidth(max(rowWidth, parentWidth).dp)
                                    .height(26.dp)
                                    .offset(offset.dp)
                                    .background(if (isHovered) rowHoverColor else Color.Transparent)
                                    .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                                    .onPointerEvent(PointerEventType.Enter) { isHovered = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row(index, item)
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
            }
        }
    }
}

private fun Int.length(): Int = when {
    this in 0..9 -> 1
    this in -9..-1 -> 2
    this < 0 -> 2 + abs(this / 10).length()
    else -> 1 + (this / 10).length()
}
private val LazyListState.isHome get() = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
private fun LazyListState.calculateOffset(itemWidths: List<Float>, density: Float): Float {
    if (itemWidths.isEmpty()) return 0f
    return (itemWidths.subList(0, firstVisibleItemIndex).takeIf { it.isNotEmpty() }
        ?.reduce { total, width -> total + width } ?: 0f) + (firstVisibleItemScrollOffset / density)
}