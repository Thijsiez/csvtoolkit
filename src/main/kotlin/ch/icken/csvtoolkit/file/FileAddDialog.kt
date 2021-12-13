package ch.icken.csvtoolkit.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.file.TabulatedFile.Type
import ch.icken.csvtoolkit.ui.DialogContent
import ch.icken.csvtoolkit.ui.ListTable
import ch.icken.csvtoolkit.ui.Spinner
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

@Composable
fun FileAddDialog(
    onAddFile: (TabulatedFile) -> Unit,
    onHide: () -> Unit,
    titleText: String = "Add File"
) {
    var showOpenFileDialog by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf(TextFieldValue("")) }
    var fileType by remember { mutableStateOf(Type.CSV) }
    var fileTypeCsvDelimiter by remember { mutableStateOf(CsvFile.Delimiter.COMMA) }
    val fileIsValid = derivedStateOf { File(fileName.text).run { exists() && isFile } }
    val file = derivedStateOf {
        when (fileType) {
            Type.CSV -> CsvFile(
                path = fileName.text,
                delimiter = fileTypeCsvDelimiter
            )
        }
    }

    Dialog(
        state = rememberDialogState(
            size = DpSize(960.dp, Dp.Unspecified)
        ),
        title = titleText,
        undecorated = true,
        resizable = false,
        onCloseRequest = onHide
    ) {
        DialogContent(
            titleText = titleText,
            confirmButton = {
                TextButton(
                    onClick = {
                        onHide()
                        onAddFile(file.value)
                    },
                    enabled = fileIsValid.value
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onHide
                ) {
                    Text("DISCARD")
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier.padding(bottom = 8.dp),
                        label = { Text("File Location") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showOpenFileDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Open file"
                                )
                            }
                        },
                        singleLine = true
                    )
                    Spinner(
                        items = Type.values().asList(),
                        itemTransform = { Text(it.uiName) },
                        onItemSelected = { fileType = it },
                        label = "File Type"
                    ) {
                        Text(fileType.uiName)
                    }
                    when (fileType) {
                        Type.CSV -> {
                            Spinner(
                                items = CsvFile.Delimiter.values().asList(),
                                itemTransform = { Text(it.uiName) },
                                onItemSelected = { fileTypeCsvDelimiter = it },
                                label = "Delimiter"
                            ) {
                                Text(fileTypeCsvDelimiter.uiName)
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (fileIsValid.value) {
                        ListTable(file.value.preview)
                    }
                }
            }
        }

        window.contentPane.dropTarget = object : DropTarget() {
            @Synchronized
            override fun drop(dtde: DropTargetDropEvent) {
                dtde.acceptDrop(DnDConstants.ACTION_REFERENCE)
                try {
                    val list = dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    list.filterIsInstance<File>().takeIf { it.size == list.size } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }.firstOrNull()?.let {
                    fileName = TextFieldValue(it.absolutePath)
                }
            }
        }
    }

    if (showOpenFileDialog) {
        OpenFileDialog(
            onFileSelected = { selectedFile ->
                showOpenFileDialog = false
                fileName = TextFieldValue(selectedFile.absolutePath)
                when (selectedFile.extension) {
                    in Type.CSV.extensions -> fileType = Type.CSV
                }
            }
        )
    }
}

@Composable
private fun OpenFileDialog(
    onFileSelected: (selectedFile: File) -> Unit,
    parent: Frame? = null
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Open File", LOAD) {
            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (visible && file != null) {
                    onFileSelected(File(directory, file))
                }
            }
        }.apply {
            setFilenameFilter { _, fileName ->
                Type.values().any { fileType ->
                    fileType.extensions.any {
                        fileName.endsWith(it)
                    }
                }
            }
        }
    },
    dispose = FileDialog::dispose
)