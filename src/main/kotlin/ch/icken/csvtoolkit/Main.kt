package ch.icken.csvtoolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ch.icken.csvtoolkit.file.FileAddDialog
import ch.icken.csvtoolkit.file.FilesView
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.TransformView
import ch.icken.csvtoolkit.ui.MapTable

fun main() = application {
    val instance = ToolkitInstance()

    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1280.dp, 800.dp)
        ),
        title = "csvtoolkit",
        resizable = false
    ) {
        MaterialTheme {
            MainView(instance)
        }
    }
}

@Composable
private fun MainView(instance: ToolkitInstance) = Row(
    modifier = Modifier.fillMaxSize()
        .padding(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showPreviewFileDialogFor: TabulatedFile? by remember { mutableStateOf(null) }
    var showEditTransformDialogFor: Transform? by remember { mutableStateOf(null) }
    val allowDoingTheThing = remember { derivedStateOf {
        instance.files.size >= 1 && instance.files.all { it.isValid } &&
                instance.transforms.size >= 1 && instance.transforms.all { it.isValid(instance) }
    } }

    Column(
        modifier = Modifier.width(320.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilesView(
            instance = instance,
            onAddFile = { showAddFileDialog = true },
            onPreviewFile = { showPreviewFileDialogFor = it }
        )
        TransformView(
            instance = instance,
            onAddTransform = { instance.transforms.add(it) },
            onEditTransform = { showEditTransformDialogFor = it }
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (allowDoingTheThing.value) {
                ExtendedFloatingActionButton(
                    text = { Text("DO THE THING") },
                    onClick = { instance.theThing() },
                    modifier = Modifier.align(Alignment.Center),
                    icon = { Icon(Icons.Default.PlayArrow, null) }
                )
            }
            if (instance.isDoingTheThing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp)
                        .align(Alignment.CenterEnd)
                )
            }
        }
    }
    Card {
        instance.data?.let {
            MapTable(it)
        }
    }


    if (showAddFileDialog) {
        FileAddDialog(
            onAddFile = { instance.files.add(it.apply { load() }) },
            onHide = { showAddFileDialog = false }
        )
    }
    showPreviewFileDialogFor?.Dialog(
        onHide = { showPreviewFileDialogFor = null }
    )
    showEditTransformDialogFor?.Dialog(
        instance = instance,
        onHide = { showEditTransformDialogFor = null }
    )
}