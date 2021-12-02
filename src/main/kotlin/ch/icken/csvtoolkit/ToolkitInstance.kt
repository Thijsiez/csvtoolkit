package ch.icken.csvtoolkit

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.transform.Transform
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ToolkitInstance : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            println("Whoops, we ran into a problem: ${throwable.message}")
        }

    val files = mutableStateListOf<TabulatedFile>()
    val transforms = mutableStateListOf<Transform>()
    var data: List<Map<String, String>>? by mutableStateOf(null); private set

    var baseFileOverride: TabulatedFile? by mutableStateOf(null)
    val baseFile = derivedStateOf { baseFileOverride ?: files.firstOrNull() ?: throw NoSuchElementException() }

    val allowDoingTheThing = derivedStateOf {
        files.size >= 1 && files.all { it.isValid } && transforms.size >= 1 && transforms.all { it.isValid(this) }
    }
    var isDoingTheThing: Boolean by mutableStateOf(false); private set
    var currentlyProcessingTransform: Transform? by mutableStateOf(null); private set

    fun headersUpTo(thisTransform: Transform, inclusive: Boolean = false): List<String> {
        return transforms
            .subList(0, transforms.indexOf(thisTransform) + if (inclusive) 1 else 0)
            .fold(baseFile.value.headers.toMutableList()) { intermediateHeaders, transform ->
                transform.doTheHeaderThing(intermediateHeaders)
            }
    }

    fun theThing() = launch {
        isDoingTheThing = true
        val finalData = baseFile.value.letData { data ->
            transforms.foldSuspendable(data.map { it.toMutableMap() }.toMutableList()) { intermediateData, transform ->
                currentlyProcessingTransform = transform
                transform.doTheActualThing(intermediateData)
            }
        }
        currentlyProcessingTransform = null
        isDoingTheThing = false
        launch(Dispatchers.Main) { data = finalData }
    }
}