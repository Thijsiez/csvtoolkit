package ch.icken.csvtoolkit.transform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.firstDuplicateOrNull
import ch.icken.csvtoolkit.transform.condition.Condition

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
        onHide: () -> Unit,
        onDelete: () -> Unit
    )

    fun getContext(instance: ToolkitInstance): Condition.Context {
        return Condition.Context(
            headers = instance.headersUpTo(this)
        )
    }

    abstract class ConditionParentTransform : Transform() {
        protected val conditions = mutableStateListOf<Condition>()

        fun remove(condition: Condition) = conditions.remove(condition)
    }
    abstract class ConditionalTransform(val parent: Transform?) : Transform() {
        abstract fun doTheConditionalHeaderThing(intermediate: MutableList<String>): MutableList<String>
        abstract fun doTheConditionalThing(intermediateRow: MutableMap<String, String>): MutableMap<String, String>
        abstract fun isValidConditional(context: Condition.Context): Boolean
        @Composable abstract fun ConditionalDialog(
            context: Condition.Context,
            onHide: () -> Unit,
            onDelete: () -> Unit
        )
    }

    enum class Type(
        val uiName: String,
        val create: (parent: Transform?) -> Transform,
        val isConditional: Boolean
    ) {
        CONDITIONAL("Conditional", { ConditionalTransformSet() }, false),
        FILTER("Filter", { FilterTransform() }, false),
        //TODO grouping transform
        JOIN("Join", { JoinTransform() }, false),
        MERGE("Merge", { MergeTransform() }, false),
        SELECT("Select", { SelectTransform() }, false),
        SET("Set", { SetTransform(it) }, true),
        SORT("Sort", { SortTransform() }, false)
    }
}