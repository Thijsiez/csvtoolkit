package ch.icken.csvtoolkit

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize

fun main() = Window(
    title = "csvtoolkit",
    size = IntSize(1280, 800)
) {
    val instance = remember {
        ToolkitInstance()
    }

    DesktopMaterialTheme {
        MainView(instance)
    }
}

@Composable
fun MainView(instance: ToolkitInstance) {

}