package ch.icken.csvtoolkit.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope

@Composable
fun WindowScope.DialogContent(
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier.border(Dp.Hairline, MaterialTheme.colors.primary),
    dismissButton: @Composable (() -> Unit)? = null,
    neutralButton: @Composable (() -> Unit)? = null,
    titleText: String? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Column {
            titleText?.let {
                WindowDraggableArea {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(24.dp, 20.dp, 24.dp, 16.dp)
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.h6
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                content = content
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                neutralButton?.invoke()
                Spacer(Modifier.weight(1f))
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}