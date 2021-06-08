package ch.icken.csvtoolkit.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.ToolkitInstance

@Composable
fun FilesView(
    instance: ToolkitInstance,
    onAddFile: () -> Unit
) = Column(
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier.height(56.dp)
            .fillMaxWidth()
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
            FilesItemView(it)
        }
    }
}

@Composable
private fun FilesItemView(file: TabulatedFile) = Row(
    modifier = Modifier.height(48.dp)
        .fillMaxWidth()
        .padding(start = 16.dp, end = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = file.name,
        style = MaterialTheme.typography.body1
    )
    if (file.isDataLoaded.value) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Data is loaded",
            tint = Color.Green
        )
    }
}