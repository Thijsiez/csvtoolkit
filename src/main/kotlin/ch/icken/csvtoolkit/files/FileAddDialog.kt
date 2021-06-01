package ch.icken.csvtoolkit.files

import androidx.compose.desktop.AppManager
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.icken.csvtoolkit.ui.CheckButton
import ch.icken.csvtoolkit.ui.DialogContent
import ch.icken.csvtoolkit.ui.Spinner
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

@Composable
fun FileAddDialog(
    onHide: () -> Unit,
    titleText: String = "Add File"
) {
    val fileName = remember { mutableStateOf(TextFieldValue("")) }
    val fileType = remember { mutableStateOf(TabulatedFile.Type.CSV) }
    val fileTypeCsvDelimiter = remember { mutableStateOf(TabulatedFile.TypeCsvDelimiter.COMMA) }
    val fileTypeCsvHasHeaders = remember { mutableStateOf(true) }
    val allowAdd = remember { derivedStateOf {
        File(fileName.value.text).run { exists() && isFile }
    } }

    Dialog(
        onDismissRequest = { onHide() },
        properties = DialogProperties(
            title = titleText,
            size = IntSize(960, 640),
            resizable = false,
            undecorated = true
        )
    ) {
        DialogContent(
            titleText = titleText,
            confirmButton = {
                TextButton(
                    onClick = {
                        onHide()
                    },
                    enabled = allowAdd.value
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onHide() }
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
                    fileTypeCsvDelimiter = fileTypeCsvDelimiter,
                    fileTypeCsvHasHeaders = fileTypeCsvHasHeaders
                )
                Divider(Modifier.padding(vertical = 4.dp))
            }
        }
    }

    AppManager.windows.find { it.title == titleText }?.let { frame ->
        frame.window.contentPane.dropTarget = object : DropTarget() {
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
    fileTypeCsvDelimiter: MutableState<TabulatedFile.TypeCsvDelimiter>,
    fileTypeCsvHasHeaders: MutableState<Boolean>
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
                delimiter = fileTypeCsvDelimiter,
                hasHeaders = fileTypeCsvHasHeaders
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
    delimiter: MutableState<TabulatedFile.TypeCsvDelimiter>,
    hasHeaders: MutableState<Boolean>
) {
    Spinner(
        items = TabulatedFile.TypeCsvDelimiter.values().asList(),
        itemTransform = { Text(it.uiName) },
        onItemSelected = { delimiter.value = it },
        modifier = Modifier.requiredWidth(224.dp)
    ) {
        Text("Delimiter: ${delimiter.value.uiName}")
    }
    Spacer(Modifier.width(8.dp))
    CheckButton(
        checked = hasHeaders.value,
        onCheckedChange = { hasHeaders.value = it }
    ) {
        Text("Has headers")
    }
}