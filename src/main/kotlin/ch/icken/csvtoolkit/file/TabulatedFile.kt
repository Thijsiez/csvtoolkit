package ch.icken.csvtoolkit.file

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.isDown
import ch.icken.csvtoolkit.ui.DialogContent
import ch.icken.csvtoolkit.ui.ListTable
import ch.icken.csvtoolkit.ui.MapTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@Serializable
abstract class TabulatedFile(
    protected val path: String,
    val uuid: String
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private lateinit var data: List<Map<String, String>>

    @Transient
    protected val file = File(path)
    val name by derivedStateOf { file.nameWithoutExtension }
    val isValid: Boolean get() = file.run { exists() && isFile }

    var state by mutableStateOf(State.NOT_LOADED); protected set

    abstract val headers: List<String>
    abstract val preview: List<List<String>>
    abstract val surrogate: FileSurrogate

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

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Dialog(onHide: () -> Unit) {
        Dialog(
            onCloseRequest = onHide,
            state = rememberDialogState(
                size = DpSize(960.dp, Dp.Unspecified)
            ),
            title = name,
            undecorated = true,
            resizable = false,
            onKeyEvent = {
                it.isDown(Key.Escape, onHide)
            }
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

    interface FileSurrogate {
        val path: String
        val uuid: String
    }
}