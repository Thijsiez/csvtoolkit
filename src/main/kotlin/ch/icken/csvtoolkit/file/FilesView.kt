package ch.icken.csvtoolkit.file

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.file.TabulatedFile.State
import ch.icken.csvtoolkit.ui.Tooltip

@Composable
fun FilesView(
    instance: ToolkitInstance,
    onAddFile: () -> Unit,
    onPreviewFile: (TabulatedFile) -> Unit
) = Card {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files",
                style = MaterialTheme.typography.h6
            )
            IconButton(
                onClick = onAddFile
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add File"
                )
            }
        }
        LazyColumn {
            items(instance.files) {
                FilesItemView(
                    instance = instance,
                    file = it,
                    onPreviewFile = { onPreviewFile(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilesItemView(
    instance: ToolkitInstance,
    file: TabulatedFile,
    onPreviewFile: () -> Unit
) = Row(
    modifier = Modifier
        .combinedClickable(
            onClick = onPreviewFile,
            onLongClick = { instance.baseFileOverride = file }
        )
        .fillMaxWidth()
        .height(48.dp)
        .padding(start = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = file.name,
        modifier = Modifier.weight(1f),
        fontWeight = if (file == instance.baseFile.value) FontWeight.Bold else FontWeight.Normal,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = MaterialTheme.typography.body1
    )
    Row(
        modifier = Modifier.requiredSize(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (file.state) {
            State.NOT_LOADED -> {
                CircularProgressIndicator()
            }
            State.LOADED -> {
                TooltipArea(
                    tooltip = { Tooltip("Data is loaded") }
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Data is loaded",
                        tint = Color.Green
                    )
                }
            }
            State.INVALID -> {
                TooltipArea(
                    tooltip = { Tooltip("File is invalid") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "File is invalid",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}