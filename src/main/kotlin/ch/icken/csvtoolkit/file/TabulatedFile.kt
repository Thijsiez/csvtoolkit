package ch.icken.csvtoolkit.file

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    var state by mutableStateOf(State.NOT_LOADED); protected set
    val isValid: Boolean get() = file.run { exists() && isFile } && state == State.LOADED

    abstract val headers: List<String>
    abstract val preview: List<List<String>>

    protected abstract fun loadData(): List<Map<String, String>>

    fun load() {
        scope.launch {
            data = loadData()
            state = State.LOADED
        }
    }

    suspend fun <R> letData(block: suspend (data: List<Map<String, String>>) -> R): R? {
        return if (this::data.isInitialized && state == State.LOADED) block(data) else null
    }

    @Composable
    fun Dialog(onHide: () -> Unit) {
        Dialog(
            state = rememberDialogState(
                size = DpSize(960.dp, Dp.Unspecified)
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
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(420.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state == State.LOADED) {
                            MapTable(data)
                        } else {
                            ListTable(preview)
                        }
                    }
                }
            }
        }
    }

    enum class Type(
        val uiName: String,
        vararg val extensions: String
    ) {
        CSV("CSV", "csv"),
        //TODO EXCEL("Excel")
    }

    enum class State {
        NOT_LOADED,
        LOADED,
        INVALID
    }
}