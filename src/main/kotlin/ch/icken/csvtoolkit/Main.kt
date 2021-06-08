package ch.icken.csvtoolkit

import androidx.compose.desktop.DesktopMaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ch.icken.csvtoolkit.files.FileAddDialog
import ch.icken.csvtoolkit.files.FilesView
import ch.icken.csvtoolkit.mutation.MutationView

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val instance = remember {
        ToolkitInstance()
    }
    val mainWindowState = rememberWindowState(
        size = WindowSize(1280.dp, 800.dp)
    )

    Window(
        state = mainWindowState,
        title = "csvtoolkit",
        resizable = false,
        initialAlignment = Alignment.Center
    ) {
        DesktopMaterialTheme {
            MainView(instance)
        }
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
            onAddFile = { instance.files.add(it) },
            onHide = { showAddFileDialog = false }
        )
    }
    if (showAddMutationDialog) {
        TODO("MutationAddDialog")
    }
}