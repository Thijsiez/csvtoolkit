package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.isDown
import ch.icken.csvtoolkit.ui.DialogContent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditDialog(
    titleText: String,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit = {},
    state: DialogState = rememberDialogState(),
    content: @Composable BoxScope.() -> Unit
) {
    var isDialogShowing by remember { mutableStateOf(false) }
    if (!isDialogShowing) {
        isDialogShowing = true
        onOpen()
    }

    Dialog(
        onCloseRequest = {
            onHide()
            isDialogShowing = false
        },
        state = state,
        title = titleText,
        undecorated = true,
        resizable = false,
        onKeyEvent = {
            it.isDown(Key.Escape) {
                onHide()
                isDialogShowing = false
            }
        }
    ) {
        DialogContent(
            confirmButton = {
                TextButton(
                    onClick = {
                        onHide()
                        isDialogShowing = false
                    }
                ) {
                    Text("DONE")
                }
            },
            neutralButton = {
                TextButton(
                    onClick = {
                        onHide()
                        isDialogShowing = false
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