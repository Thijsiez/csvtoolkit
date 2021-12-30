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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.file.TabulatedFile.Type
import ch.icken.csvtoolkit.isDown
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

@OptIn(ExperimentalComposeUiApi::class)
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
    val fileIsValid by derivedStateOf { File(fileName.text).run { exists() && isFile } }
    val file by derivedStateOf {
        when (fileType) {
            Type.CSV -> CsvFile(
                path = fileName.text,
                delimiter = fileTypeCsvDelimiter
            )
        }
    }

    Dialog(
        onCloseRequest = onHide,
        state = rememberDialogState(
            size = DpSize(960.dp, Dp.Unspecified)
        ),
        title = titleText,
        undecorated = true,
        resizable = false,
        onKeyEvent = {
            it.isDown(Key.Escape, onHide)
        }
    ) {
        DialogContent(
            titleText = titleText,
            confirmButton = {
                TextButton(
                    onClick = {
                        onHide()
                        onAddFile(file)
                    },
                    enabled = fileIsValid
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
                        selectedItem = { fileType },
                        onItemSelected = { fileType = it },
                        itemTransform = { it.uiName },
                        label = "File Type"
                    )
                    when (fileType) {
                        Type.CSV -> {
                            Spinner(
                                items = CsvFile.Delimiter.values().asList(),
                                selectedItem = { fileTypeCsvDelimiter },
                                onItemSelected = { fileTypeCsvDelimiter = it },
                                itemTransform = { it.uiName },
                                label = "Delimiter"
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (fileIsValid) {
                        ListTable(file.observablePreview)
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
        OpenTabulatedFileDialog { selectedFile ->
            showOpenFileDialog = false
            fileName = TextFieldValue(selectedFile.absolutePath)
            when (selectedFile.extension) {
                in Type.CSV.extensions -> fileType = Type.CSV
            }
        }
    }
}

@Composable
private fun OpenTabulatedFileDialog(
    parent: Frame? = null,
    onFileSelected: (selectedFile: File) -> Unit
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
            //This doesn't work in Windows
            val extensions = Type.values().flatMap {
                it.extensions.asList()
            }
            setFilenameFilter { _, fileName ->
                extensions.any {
                    fileName.endsWith(".$it")
                }
            }
        }
    },
    dispose = FileDialog::dispose
)