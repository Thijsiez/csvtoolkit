package ch.icken.csvtoolkit.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ui.DialogContent
import ch.icken.csvtoolkit.ui.Spinner
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
    val dialogState = rememberDialogState(
        size = WindowSize(960.dp, 640.dp)
    )
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
            else -> TODO("Implement other types")
        }
    } }

    Dialog(
        state = dialogState,
        title = titleText,
        undecorated = true,
        resizable = false,
        initialAlignment = Alignment.Center,
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
            modifier = Modifier.border(1.dp, MaterialTheme.colors.primary)
        ) {
            Column {
                FileRow(
                    fileName = fileName,
                    fileType = fileType,
                    fileTypeCsvDelimiter = fileTypeCsvDelimiter
                )
                Divider(Modifier.padding(vertical = 4.dp))
                if (fileIsValid.value) {
                    FilePreview(
                        file = file
                    )
                }
            }
        }

        dialog.contentPane.dropTarget = object : DropTarget() {
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = fileName.value,
            onValueChange = { fileName.value = it },
            modifier = Modifier.padding(bottom = 8.dp),
            label = { Text("File Location") },
            singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        Spinner(
            items = TabulatedFile.Type.values().asList(),
            itemTransform = { Text(it.uiName) },
            onItemSelected = { fileType.value = it },
            modifier = Modifier.requiredWidth(168.dp)
        ) {
            Text("Type: ${fileType.value.uiName}")
        }
        Spacer(Modifier.width(16.dp))
        when (fileType.value) {
            TabulatedFile.Type.CSV -> FileTypeCsv(
                delimiter = fileTypeCsvDelimiter
            )
            else -> Text(
                text = "No settings for this file type",
                color = Color(0f, 0f, 0f, .6f)
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
        modifier = Modifier.requiredWidth(224.dp)
    ) {
        Text("Delimiter: ${delimiter.value.uiName}")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilePreview(
    file: State<TabulatedFile>
) {
    val numberOfColumns = file.value.preview.firstOrNull()?.size ?: 1
    LazyVerticalGrid(
        cells = GridCells.Fixed(numberOfColumns)
    ) {
        itemsIndexed(file.value.preview.flatten()) { index, item ->
            Text(
                text = item,
                modifier = Modifier.padding(vertical = 4.dp),
                fontWeight = if (index < numberOfColumns) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}