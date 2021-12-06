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
    }

    abstract val description: AnnotatedString

    var invalidMessage by mutableStateOf(""); protected set

    abstract fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String>
    abstract suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>>

    open fun isValid(instance: ToolkitInstance): Boolean {
        if (instance.headersUpTo(this, true).firstDuplicateOrNull() != null) {
            //TODO attempt to fix header collision by renaming columns
            invalidMessage = "Header collision"
            return false
        }
        return true
    }

    @Composable
    abstract fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit
    )

    fun getContext(instance: ToolkitInstance): ConditionalTransform.Context {
        return ConditionalTransform.Context(
            headers = instance.headersUpTo(this)
        )
    }

    abstract class ConditionalTransform(val parent: Transform?) : Transform() {
        abstract fun doTheConditionalHeaderThing(intermediate: MutableList<String>): MutableList<String>
        abstract fun doTheConditionalThing(intermediateRow: MutableMap<String, String>): MutableMap<String, String>
        abstract fun isValidConditional(context: Context): Boolean
        @Composable abstract fun ConditionalDialog(context: Context, onHide: () -> Unit)

        data class Context(
            val headers: List<String>
        )
    }

    enum class Type(
        val uiName: String,
        val create: (parent: Transform?) -> Transform,
        val isConditional: Boolean
    ) {
        JOIN("Join", { JoinTransform() }, false),
        MERGE("Merge", { MergeTransform() }, false),
        CONDITIONAL("Conditional", { ConditionalTransformSet() }, false),
        FILTER("Filter", { FilterTransform() }, false),
        SET("Set", { SetTransform(it) }, true)
    }
}