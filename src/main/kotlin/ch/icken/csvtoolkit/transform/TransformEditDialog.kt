package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.border
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ui.DialogContent

@Composable
fun TransformEditDialog(
    titleText: String,
    onHide: () -> Unit,
    state: DialogState = rememberDialogState(),
    content: @Composable () -> Unit
) {
    Dialog(
        state = state,
        title = titleText,
        undecorated = true,
        resizable = false,
        onCloseRequest = onHide
    ) {
        DialogContent(
            titleText = titleText,
            confirmButton = {
                TextButton(
                    onClick = onHide
                ) {
                    Text("DONE")
                }
            },
            modifier = Modifier.border(Dp.Hairline, MaterialTheme.colors.primary),
            content = content
        )
    }
}