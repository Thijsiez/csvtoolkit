package ch.icken.csvtoolkit.mutation

import androidx.compose.foundation.border
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ui.DialogContent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MutationEditDialog(
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
        initialAlignment = Alignment.Center,
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
            modifier = Modifier.border(1.dp, MaterialTheme.colors.primary),
            content = content
        )
    }
}