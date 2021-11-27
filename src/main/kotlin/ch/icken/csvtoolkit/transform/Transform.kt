package ch.icken.csvtoolkit.transform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.firstDuplicateOrNull

abstract class Transform {
    companion object {
        private val NumberOfLogicalCores = Runtime.getRuntime().availableProcessors()
        fun chunkSize(listSize: Int) = (listSize + NumberOfLogicalCores - 1) / NumberOfLogicalCores
        fun create(type: Type) = when (type) {
            Type.JOIN -> JoinTransform()
            Type.MERGE -> MergeTransform()
        }
    }

    abstract val description: AnnotatedString

    var invalidMessage: String by mutableStateOf(""); protected set

    abstract fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String>
    abstract suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>>

    @Composable
    abstract fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit
    )

    open fun isValid(instance: ToolkitInstance): Boolean {
        if (instance.headersUpTo(this, true).firstDuplicateOrNull() != null) {
            invalidMessage = "Header collision"
            return false
        }
        return true
    }

    enum class Type(
        val uiName: String
    ) {
        JOIN("Join"),
        MERGE("Merge")
    }
}