package ch.icken.csvtoolkit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun Tooltip(text: String) {
    Surface(
        modifier = Modifier.shadow(4.dp),
        color = MaterialTheme.colors.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp)
        )
    }
}