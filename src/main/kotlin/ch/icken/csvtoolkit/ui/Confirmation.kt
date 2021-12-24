package ch.icken.csvtoolkit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.isDown
import ch.icken.csvtoolkit.transform.condition.Condition

data class Confirmation(
    private val title: String,
    private val onHide: () -> Unit,
    private val positive: Pair<String, () -> Unit>,
    private val negative: Pair<String, () -> Unit> = "CANCEL" to {},
    private val content: @Composable BoxScope.() -> Unit
) {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Dialog(
        state: DialogState = rememberDialogState(
            size = DpSize(360.dp, Dp.Unspecified)
        )
    ) {
        Dialog(
            onCloseRequest = onHide,
            state = state,
            title = title,
            undecorated = true,
            resizable = false,
            onKeyEvent = {
                it.isDown(Key.Escape, onHide)
            }
        ) {
            DialogContent(
                confirmButton = {
                    TextButton(
                        onClick = {
                            onHide()
                            positive.second()
                        }
                    ) {
                        Text(positive.first)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onHide()
                            negative.second()
                        }
                    ) {
                        Text(negative.first)
                    }
                },
                titleText = title,
                content = content
            )
        }
    }
}

fun DeleteConditionConfirmation(
    condition: Condition,
    onHide: () -> Unit,
    title: String = "Delete condition?",
    onDelete: () -> Unit = {
        if (condition.parentCondition != null) {
            condition.parentCondition.remove(condition)
        } else {
            condition.parentTransform.remove(condition)
        }
    },
    content: @Composable BoxScope.() -> Unit = {
        DeleteConfirmationContent(condition.description)
    }
) = Confirmation(
    title = title,
    onHide = onHide,
    positive = "DELETE" to onDelete,
    content = content
)

@Composable
fun DeleteConfirmationContent(
    description: AnnotatedString
) = Column(
    modifier = Modifier.fillMaxWidth()
) {
    Text("You're about to delete the following item")
    Box(
        modifier = Modifier.height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.body1
        )
    }
}