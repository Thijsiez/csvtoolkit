package ch.icken.csvtoolkit.file

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.isDown
import ch.icken.csvtoolkit.ui.DialogContent
import ch.icken.csvtoolkit.ui.ListTable
import ch.icken.csvtoolkit.ui.MapTable
import ch.icken.csvtoolkit.ui.Tooltip
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import kotlin.coroutines.CoroutineContext

@Serializable
abstract class TabulatedFile(
    protected val path: String,
    val uuid: String
) : CoroutineScope, Closeable {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            println("$name ran into a problem...")
            throwable.printStackTrace()
        }

    private val watchService by lazy {
        runCatching {
            FileSystems.getDefault().newWatchService()
        }.getOrNull()
    }
    private val directoryWatchKey by lazy {
        watchService?.let { watcher ->
            runCatching {
                val parentPath = file.parentFile.toPath()
                parentPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
            }.getOrNull()
        }
    }
    private var directoryWatchJob: Job? = null

    @Transient
    protected val file = File(path)
    protected var alias by mutableStateOf(TextFieldValue(""))
    val name by derivedStateOf { alias.text.ifBlank { null } ?: file.nameWithoutExtension }
    var state by mutableStateOf(if (file.exists() && file.isFile) State.NOT_LOADED else State.INVALID); protected set
    val isValid by derivedStateOf { state != State.INVALID && state != State.LOADING }

    abstract val headers: List<String>
    protected abstract val preview: List<List<String>>
    val observablePreview = derivedStateOf { preview }
    abstract val surrogate: FileSurrogate

    private var data: List<Map<String, String>> by mutableStateOf(emptyList())
    protected var keepInMemory by mutableStateOf(true)

    private fun load() = launch {
        data = runCatching {
            loadData().also {
                state = State.LOADED
            }
        }.getOrElse {
            state = State.INVALID
            emptyList()
        }
    }
    protected abstract fun loadData(): List<Map<String, String>>

    suspend fun <R> letData(block: suspend (data: List<Map<String, String>>) -> R): R? {
        if (!isValid) return null
        if (state == State.NOT_LOADED) {
            state = State.LOADING
            load().join()
        }
        if (state == State.INVALID) return null
        return block(data)
    }

    fun watchForChanges() {
        if (directoryWatchJob?.isActive == true) return
        directoryWatchJob = launch {
            watchService?.let { watcher ->
                while (directoryWatchKey?.isValid == true) {
                    val watchKey = watcher.take()
                    for (event in watchKey.pollEvents()) {
                        if (!(event.context() as Path).endsWith(file.name)) continue
                        state = when (event.kind()) {
                            ENTRY_CREATE, ENTRY_MODIFY -> State.NOT_LOADED
                            ENTRY_DELETE -> State.INVALID
                            else -> state
                        }
                        data = emptyList()
                    }
                    if (!watchKey.reset()) {
                        watchKey.cancel()
                        watchService?.close()
                        break
                    }
                }
            }
        }
    }

    fun unloadIfNecessary() {
        if (state == State.LOADED && !keepInMemory) {
            state = State.NOT_LOADED
            data = emptyList()
        }
    }

    override fun close() {
        directoryWatchJob?.cancel()
        directoryWatchKey?.cancel()
        cancel()
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
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
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .height(500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            modifier = Modifier.padding(bottom = 8.dp),
                            label = { Text("Alias") },
                            placeholder = { Text(file.nameWithoutExtension) },
                            singleLine = true
                        )
                        TooltipArea(
                            tooltip = { Tooltip("This speeds up successive runs") }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = keepInMemory,
                                    onCheckedChange = { keep ->
                                        keepInMemory = keep
                                    }
                                )
                                Text("Keep in memory")
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (state == State.LOADING) CircularProgressIndicator()
                    }
                    Card(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state == State.LOADED) {
                            MapTable(derivedStateOf { data })
                        } else {
                            ListTable(observablePreview)
                            if (state == State.NOT_LOADED) {
                                state = State.LOADING
                                load()
                            }
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
        LOADING,
        LOADED,
        INVALID
    }

    interface FileSurrogate {
        val path: String
        val uuid: String
        val alias: String
        val keepInMemory: Boolean
    }
}