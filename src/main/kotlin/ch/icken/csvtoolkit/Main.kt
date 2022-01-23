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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ch.icken.csvtoolkit.file.FileAddDialog
import ch.icken.csvtoolkit.file.FilesView
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.transform.ConditionalTransformSet
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.TransformView
import ch.icken.csvtoolkit.transform.aggregate.Aggregate
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.ui.Confirmation
import ch.icken.csvtoolkit.ui.DeleteAggregateConfirmation
import ch.icken.csvtoolkit.ui.DeleteConditionConfirmation
import ch.icken.csvtoolkit.ui.DeleteConfirmationContent
import ch.icken.csvtoolkit.ui.MapTable
import ch.icken.csvtoolkit.ui.OpenWindowConfirmation
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    val context = remember { ApplicationContext() }

    context.windows.forEach {
        key(it) { Window(it) }
    }
}

@Composable
private fun Window(context: WindowContext) = Window(
    onCloseRequest = context::closeProject,
    state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(1280.dp, 800.dp)
    ),
    title = "csvtoolkit",
    icon = painterResource("icon.png"),
    resizable = false
) {
    var showOpenProjectFileDialog by remember { mutableStateOf(false) }
    var showSaveProjectFileDialog by remember { mutableStateOf(false) }
    var showExportDataFileDialog by remember { mutableStateOf(false) }
    var showConfirmationDialogFor: Confirmation? by remember { mutableStateOf(null) }

    MenuBar {
        Menu(
            text = "Project",
            mnemonic = 'P'
        ) {
            Item(
                text = "New",
                mnemonic = 'N'
            ) {
                context.newProject()
            }
            Item(
                text = "Open",
                mnemonic = 'O'
            ) {
                showOpenProjectFileDialog = true
            }
            Item(
                text = "Save",
                mnemonic = 'S'
            ) {
                showSaveProjectFileDialog = true
            }
            Item(
                text = "Close",
                mnemonic = 'C'
            ) {
                context.closeProject()
            }
            Separator()
            Item(
                text = "Export",
                enabled = context.instance.allowDataExport,
                mnemonic = 'E'
            ) {
                showExportDataFileDialog = true
            }
        }
    }
    MaterialTheme {
        MainView(
            instance = context.instance,
            onExport = { showExportDataFileDialog = true }
        )

        if (showOpenProjectFileDialog) {
            OpenFileDialog("Open Project", "csvproj") { projectFile ->
                showOpenProjectFileDialog = false
                showConfirmationDialogFor = OpenWindowConfirmation(
                    onHide = { showConfirmationDialogFor = null },
                    newWindow = { context.openProject(projectFile) },
                    thisWindow = { context.replaceInstance(projectFile) }
                )
            }
        }
        if (showSaveProjectFileDialog) {
            SaveFileDialog("Save Project", "project", "csvproj") {
                showSaveProjectFileDialog = false
                context.instance.saveProject(it)
            }
        }
        if (showExportDataFileDialog) {
            SaveFileDialog("Export data to CSV", "data", "csv") {
                showExportDataFileDialog = false
                context.instance.exportData(it)
            }
        }
        showConfirmationDialogFor?.Dialog()
    }
}

@Composable
private fun MainView(
    instance: ToolkitInstance,
    onExport: () -> Unit
) = Row(
    modifier = Modifier.fillMaxSize()
        .padding(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showPreviewFileDialogFor: TabulatedFile? by remember { mutableStateOf(null) }
    var showEditTransformDialogFor: Transform? by remember { mutableStateOf(null) }
    var showEditAggregateDialogFor: Aggregate? by remember { mutableStateOf(null) }
    var showEditConditionDialogFor: Condition? by remember { mutableStateOf(null) }
    var showConfirmationDialogFor: Confirmation? by remember { mutableStateOf(null) }

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
            onEditAggregate = { showEditAggregateDialogFor = it },
            onEditCondition = { showEditConditionDialogFor = it }
        )
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            MapTable(instance.observableData)
        }
        if (instance.allowDataExport) {
            FloatingActionButton(
                onClick = onExport,
                modifier = Modifier.padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Export data"
                )
            }
        }
    }


    if (showAddFileDialog) {
        FileAddDialog(
            onAddFile = { instance.files.add(it.apply { watchForChanges() }) },
            onHide = { showAddFileDialog = false }
        )
    }
    showPreviewFileDialogFor?.Dialog(
        onHide = {
            showPreviewFileDialogFor?.unloadIfNecessary()
            showPreviewFileDialogFor = null
        }
    )
    showEditTransformDialogFor?.let { transform ->
        if (transform is ConditionalTransform && transform.parent != null) {
            transform.ConditionalDialog(
                context = transform.parent.getConditionContext(instance),
                onHide = { showEditTransformDialogFor = null },
                onDelete = {
                    showConfirmationDialogFor = Confirmation(
                        title = "Delete conditional transformation?",
                        onHide = { showConfirmationDialogFor = null },
                        positive = "DELETE" to {
                            if (transform.parent is ConditionalTransformSet) {
                                transform.parent.remove(transform)
                            }
                        }
                    ) {
                        DeleteConfirmationContent(transform.description)
                    }
                }
            )
        } else {
            transform.Dialog(
                instance = instance,
                onHide = { showEditTransformDialogFor = null },
                onDelete = {
                    showConfirmationDialogFor = Confirmation(
                        title = "Delete transformation?",
                        onHide = { showConfirmationDialogFor = null },
                        positive = "DELETE" to {
                            instance.transforms.remove(transform)
                        }
                    ) {
                        DeleteConfirmationContent(transform.description)
                    }
                }
            )
        }

    }
    showEditAggregateDialogFor?.let { aggregate ->
        aggregate.Dialog(
            context = aggregate.parentTransform.getAggregateContext(instance),
            onHide = { showEditAggregateDialogFor = null },
            onDelete = {
                showConfirmationDialogFor = DeleteAggregateConfirmation(
                    aggregate = aggregate,
                    onHide = { showConfirmationDialogFor = null }
                )
            }
        )
    }
    showEditConditionDialogFor?.let { condition ->
        condition.Dialog(
            context = condition.parentTransform.getConditionContext(instance),
            onHide = { showEditConditionDialogFor = null },
            onDelete = {
                showConfirmationDialogFor = DeleteConditionConfirmation(
                    condition = condition,
                    onHide = { showConfirmationDialogFor = null }
                )
            }
        )
    }
    showConfirmationDialogFor?.Dialog()
}

private class ApplicationContext {
    val windows = mutableStateListOf<WindowContext>()

    init { new() }

    fun new() = open(ToolkitInstance())
    fun open(instance: ToolkitInstance) {
        windows.add(WindowContext(
            instance = instance,
            onNew = ::new,
            onOpen = ::open,
            onClose = windows::remove
        ))
    }
}
private class WindowContext(
    instance: ToolkitInstance,
    private val onNew: () -> Unit,
    private val onOpen: (ToolkitInstance) -> Unit,
    private val onClose: (WindowContext) -> Unit
) {
    var instance by mutableStateOf(instance); private set

    fun newProject() = onNew()
    fun openProject(file: File) {
        instance.loadProject(file, onOpen)
    }
    fun closeProject() {
        instance.close()
        onClose(this)
    }

    fun replaceInstance(file: File) {
        instance.loadProject(file) {
            instance = it
        }
    }
}

@Composable
private fun OpenFileDialog(
    title: String,
    vararg allowedExtensions: String,
    parent: Frame? = null,
    onFileSpecified: (specifiedFile: File) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, title, LOAD) {
            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (visible && file != null) {
                    onFileSpecified(File(directory, file))
                }
            }
        }.apply {
            //This doesn't work in Windows
            if (allowedExtensions.isNotEmpty()) {
                setFilenameFilter { _, fileName ->
                    allowedExtensions.any {
                        fileName.endsWith(".$it")
                    }
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

@Composable
private fun SaveFileDialog(
    title: String,
    defaultFileName: String? = null,
    requiredExtension: String? = null,
    parent: Frame? = null,
    onFileSpecified: (specifiedFile: File) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, title, SAVE) {
            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (visible && file != null) {
                    if (requiredExtension != null && !file.endsWith(requiredExtension)) {
                        file += ".$requiredExtension"
                    }
                    onFileSpecified(File(directory, file))
                }
            }
        }.apply {
            file = defaultFileName
        }
    },
    dispose = FileDialog::dispose
)