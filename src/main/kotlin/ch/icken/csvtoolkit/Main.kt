package ch.icken.csvtoolkit

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.files.FileAddDialog
import ch.icken.csvtoolkit.files.FilesView
import ch.icken.csvtoolkit.mutation.MutationView

fun main() = Window(
    title = "csvtoolkit",
    size = IntSize(1280, 800),
    resizable = false
) {
    val instance = remember {
        ToolkitInstance()
    }

    DesktopMaterialTheme {
        MainView(instance)
    }
}

@Composable
private fun MainView(instance: ToolkitInstance) = Row(
    modifier = Modifier.fillMaxSize()
) {
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showAddMutationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(320.dp)
            .fillMaxHeight()
    ) {
        FilesView(
            instance = instance,
            onAddFile = { showAddFileDialog = true }
        )
        MutationView(
            instance = instance,
            onAddMutation = { showAddMutationDialog = true }
        )
    }

    if (showAddFileDialog) {
        FileAddDialog(
            onHide = { showAddFileDialog = false }
        )
    }
    if (showAddMutationDialog) {
        TODO("MutationAddDialog")
    }
}