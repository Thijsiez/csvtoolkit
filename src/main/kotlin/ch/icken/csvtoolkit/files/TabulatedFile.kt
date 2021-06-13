package ch.icken.csvtoolkit.files

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

abstract class TabulatedFile(
    val type: Type,
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

    fun <R> letData(block: (data: List<Map<String, String>>) -> R): R? {
        return if (this::data.isInitialized && state.value == State.LOADED) data.let(block) else null
    }

    enum class Type(
        val uiName: String
    ) {
        CSV("CSV"),
        EXCEL("Excel")
    }

    enum class State {
        NOT_LOADED,
        LOADED,
        INVALID
    }
}