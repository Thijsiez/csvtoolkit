package ch.icken.csvtoolkit.file

import androidx.compose.foundation.border
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val showOpenFileDialog = remember { mutableStateOf(false) }
    val fileName = remember { mutableStateOf(TextFieldValue("")) }
    val fileType = remember { mutableStateOf(Type.CSV) }
    val fileTypeCsvDelimiter = remember { mutableStateOf(CsvFile.Delimiter.COMMA) }
    val fileIsValid = remember { derivedStateOf {
        File(fileName.value.text).run { exists() && isFile }
    } }
    val file = remember { derivedStateOf {
        when (fileType.value) {
            Type.CSV -> CsvFile(
                path = fileName.value.text,
                delimiter = fileTypeCsvDelimiter.value
            )
        }
    } }

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
                        onAddFile(file.value)
                        onHide()
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
            },
            modifier = Modifier.border(Dp.Hairline, MaterialTheme.colors.primary)
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
                        value = fileName.value,
                        onValueChange = { fileName.value = it },
                        modifier = Modifier.padding(bottom = 8.dp),
                        label = { Text("File Location") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showOpenFileDialog.value = true }
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
                        onItemSelected = { fileType.value = it },
                        label = "File Type"
                    ) {
                        Text(fileType.value.uiName)
                    }
                    when (fileType.value) {
                        Type.CSV -> {
                            Spinner(
                                items = CsvFile.Delimiter.values().asList(),
                                itemTransform = { Text(it.uiName) },
                                onItemSelected = { fileTypeCsvDelimiter.value = it },
                                label = "Delimiter"
                            ) {
                                Text(fileTypeCsvDelimiter.value.uiName)
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
                    fileName.value = TextFieldValue(it.absolutePath)
                }
            }
        }
    }

    if (showOpenFileDialog.value) {
        OpenFileDialog(
            onFileSelected = { selectedFile ->
                showOpenFileDialog.value = false
                fileName.value = TextFieldValue(selectedFile.absolutePath)
                when (selectedFile.extension) {
                    in Type.CSV.extensions -> fileType.value = Type.CSV
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