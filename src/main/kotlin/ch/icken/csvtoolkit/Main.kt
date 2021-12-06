package ch.icken.csvtoolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ch.icken.csvtoolkit.file.FileAddDialog
import ch.icken.csvtoolkit.file.FilesView
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.TransformView
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.ui.MapTable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

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
    var showEditConditionDialogFor: Condition? by remember { mutableStateOf(null) }
    var showSaveFileDialog by remember { mutableStateOf(false) }

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
            onEditTransform = { showEditTransformDialogFor = it },
            onEditCondition = { showEditConditionDialogFor = it }
        )
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            instance.data?.let {
                MapTable(it)
            }
        }
        if (instance.data != null) {
            FloatingActionButton(
                onClick = { showSaveFileDialog = true },
                modifier = Modifier.padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save output"
                )
            }
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
    showEditTransformDialogFor?.let { transform ->
        if (transform is ConditionalTransform && transform.parent != null) {
            transform.ConditionalDialog(
                context = transform.parent.getContext(instance),
                onHide = { showEditTransformDialogFor = null }
            )
        } else {
            transform.Dialog(
                instance = instance,
                onHide = { showEditTransformDialogFor = null }
            )
        }

    }
    showEditConditionDialogFor?.let { condition ->
        condition.Dialog(
            context = condition.parent.getContext(instance),
            onHide = { showEditConditionDialogFor = null }
        )
    }
    if (showSaveFileDialog) {
        SaveFileDialog(
            onFileSpecified = { specifiedFile ->
                showSaveFileDialog = false
                instance.saveData(specifiedFile)
            }
        )
    }
}

@Composable
private fun SaveFileDialog(
    onFileSpecified: (specifiedFile: File) -> Unit,
    parent: Frame? = null
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Save CSV File", SAVE) {
            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (visible && file != null) {
                    onFileSpecified(File(directory, file))
                }
            }
        }
    },
    dispose = FileDialog::dispose
)