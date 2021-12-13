package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ui.DialogContent

@Composable
fun EditDialog(
    titleText: String,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    state: DialogState = rememberDialogState(),
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        state = state,
        title = titleText,
        undecorated = true,
        resizable = false,
        onCloseRequest = onHide
    ) {
        DialogContent(
            confirmButton = {
                TextButton(
                    onClick = onHide
                ) {
                    Text("DONE")
                }
            },
            neutralButton = {
                TextButton(
                    onClick = {
                        onHide()
                        onDelete()
                    }
                ) {
                    Text("DELETE")
                }
            },
            titleText = titleText,
            content = content
        )
    }
}