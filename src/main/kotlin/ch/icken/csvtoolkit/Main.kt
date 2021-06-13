package ch.icken.csvtoolkit

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import ch.icken.csvtoolkit.mutation.Mutation
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
    var showEditMutationDialogFor: Mutation? by remember { mutableStateOf(null) }
    val allowDoingTheThing = remember { derivedStateOf {
        instance.files.size >= 2 && instance.files.all { it.isValid } &&
                instance.mutations.size >= 1 && instance.mutations.all { it.isValid(instance) }
    } }

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
            onAddMutation = { instance.mutations.add(it) },
            onEditMutation = { showEditMutationDialogFor = it }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { instance.theThing() },
            modifier = Modifier.align(Alignment.End),
            enabled = allowDoingTheThing.value
        ) {
            Text("DO THE THING")
        }
    }

    if (showAddFileDialog) {
        FileAddDialog(
            onAddFile = { instance.files.add(it.apply { load() }) },
            onHide = { showAddFileDialog = false }
        )
    }
    showEditMutationDialogFor?.Dialog(
        instance = instance,
        onHide = { showEditMutationDialogFor = null }
    )
}