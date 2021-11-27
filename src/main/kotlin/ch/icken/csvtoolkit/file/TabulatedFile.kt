package ch.icken.csvtoolkit.file

import androidx.compose.foundation.border
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ui.DialogContent
import ch.icken.csvtoolkit.ui.ListTable
import ch.icken.csvtoolkit.ui.MapTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

abstract class TabulatedFile(
    val path: String,
    private val alias: String?
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private lateinit var data: List<Map<String, String>>

    val file: File get() = File(path)
    val name: String get() = alias ?: file.nameWithoutExtension
    val state: MutableState<State> = mutableStateOf(State.NOT_LOADED)
    val isValid: Boolean get() = file.run { exists() && isFile } && state.value == State.LOADED

    abstract val headers: List<String>
    abstract val preview: List<List<String>>

    protected abstract fun loadData(): List<Map<String, String>>

    fun load() {
        scope.launch {
            data = loadData()
            state.value = State.LOADED
        }
    }

    suspend fun <R> letData(block: suspend (data: List<Map<String, String>>) -> R): R? {
        return if (this::data.isInitialized && state.value == State.LOADED) block(data) else null
    }

    @Composable
    fun Dialog(onHide: () -> Unit) {
        Dialog(
            state = rememberDialogState(
                size = DpSize(960.dp, 640.dp)
            ),
            title = name,
            undecorated = true,
            resizable = false,
            onCloseRequest = onHide
        ) {
            DialogContent(
                titleText = name,
                confirmButton = {
                    TextButton(
                        onClick = onHide
                    ) {
                        Text("CLOSE")
                    }
                },
                modifier = Modifier.border(Dp.Hairline, MaterialTheme.colors.primary)
            ) {
                Card {
                    if (state.value == State.LOADED) {
                        MapTable(data)
                    } else {
                        ListTable(preview)
                    }
                }
            }
        }
    }

    enum class Type(
        val uiName: String
    ) {
        CSV("CSV"),
        //TODO EXCEL("Excel")
    }

    enum class State {
        NOT_LOADED,
        LOADED,
        INVALID
    }
}