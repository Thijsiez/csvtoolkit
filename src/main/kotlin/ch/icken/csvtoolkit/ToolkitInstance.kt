package ch.icken.csvtoolkit

import androidx.compose.runtime.mutableStateListOf
import ch.icken.csvtoolkit.files.TabulatedFile
import ch.icken.csvtoolkit.mutation.Mutation

class ToolkitInstance {
    val files = mutableStateListOf<TabulatedFile>()
    val mutations = mutableStateListOf<Mutation>()
}