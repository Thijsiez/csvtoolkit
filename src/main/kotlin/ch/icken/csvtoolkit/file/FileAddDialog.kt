package ch.icken.csvtoolkit.file

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ui.DialogContent
import ch.icken.csvtoolkit.ui.ListTable
import ch.icken.csvtoolkit.ui.Spinner
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
    val fileName = remember { mutableStateOf(TextFieldValue("")) }
    val fileType = remember { mutableStateOf(TabulatedFile.Type.CSV) }
    val fileTypeCsvDelimiter = remember { mutableStateOf(CsvFile.Delimiter.COMMA) }
    val fileIsValid = remember { derivedStateOf {
        File(fileName.value.text).run { exists() && isFile }
    } }
    val file = remember { derivedStateOf {
        when (fileType.value) {
            TabulatedFile.Type.CSV -> CsvFile(
                path = fileName.value.text,
                delimiter = fileTypeCsvDelimiter.value
            )
        }
    } }

    Dialog(
        state = rememberDialogState(
            size = DpSize(960.dp, 640.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileRow(
                    fileName = fileName,
                    fileType = fileType,
                    fileTypeCsvDelimiter = fileTypeCsvDelimiter
                )
                if (fileIsValid.value) {
                    Card {
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
}

@Composable
private fun FileRow(
    fileName: MutableState<TextFieldValue>,
    fileType: MutableState<TabulatedFile.Type>,
    fileTypeCsvDelimiter: MutableState<CsvFile.Delimiter>
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
            singleLine = true
        )
        Spinner(
            items = TabulatedFile.Type.values().asList(),
            itemTransform = { Text(it.uiName) },
            onItemSelected = { fileType.value = it },
            label = "File Type"
        ) {
            Text(fileType.value.uiName)
        }
        when (fileType.value) {
            TabulatedFile.Type.CSV -> FileTypeCsv(
                delimiter = fileTypeCsvDelimiter
            )
        }
    }
}

@Composable
private fun FileTypeCsv(
    delimiter: MutableState<CsvFile.Delimiter>
) {
    Spinner(
        items = CsvFile.Delimiter.values().asList(),
        itemTransform = { Text(it.uiName) },
        onItemSelected = { delimiter.value = it },
        label = "Delimiter"
    ) {
        Text(delimiter.value.uiName)
    }
}