package ch.icken.csvtoolkit.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Files",
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.weight(1f))
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
        .padding(start = 16.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = file.path,
        style = MaterialTheme.typography.body1
    )
}