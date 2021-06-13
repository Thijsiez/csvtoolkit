package ch.icken.csvtoolkit

import androidx.compose.runtime.mutableStateListOf
import ch.icken.csvtoolkit.files.TabulatedFile
import ch.icken.csvtoolkit.mutation.Mutation
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
    val mutations = mutableStateListOf<Mutation>()

    fun headersUpTo(thisMutation: Mutation, inclusive: Boolean = false): List<String> {
        return mutations
            .subList(0, mutations.indexOf(thisMutation) + if (inclusive) 1 else 0)
            .fold(files.first().headers.toMutableList()) { intermediateHeaders, mutation ->
                mutation.doTheHeaderThing(intermediateHeaders)
            }
    }

    fun theThing() = launch {
        println(files.first().letData { data ->
            mutations.fold(data.map { it.toMutableMap() }.toMutableList()) { intermediateData, mutation ->
                mutation.doTheActualThing(intermediateData)
            }
        })
    }
}