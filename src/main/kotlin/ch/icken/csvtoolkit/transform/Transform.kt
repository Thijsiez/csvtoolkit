package ch.icken.csvtoolkit.transform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.firstDuplicateOrNull
import ch.icken.csvtoolkit.transform.aggregate.Aggregate
import ch.icken.csvtoolkit.transform.condition.Condition
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
abstract class Transform {
    companion object {
        private val NumberOfLogicalCores = Runtime.getRuntime().availableProcessors()
        fun chunkSize(listSize: Int) = (listSize + NumberOfLogicalCores - 1) / NumberOfLogicalCores
    }

    abstract val description: AnnotatedString
    abstract val surrogate: TransformSurrogate
    open val usesFile: TabulatedFile? = null

    var invalidMessage by mutableStateOf(""); protected set
    var lastRunStats: Statistics? by mutableStateOf(null); protected set

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

    open fun postDeserialization(instance: ToolkitInstance) {}

    fun getAggregateContext(instance: ToolkitInstance): Aggregate.Context {
        return Aggregate.Context(
            headers = instance.headersUpTo(this),
            allowChanges = !instance.isDoingTheThing
        )
    }
    fun getConditionContext(instance: ToolkitInstance): Condition.Context {
        return Condition.Context(
            headers = instance.headersUpTo(this),
            allowChanges = !instance.isDoingTheThing
        )
    }

    suspend fun <T> track(calculation: suspend Transform.() -> MutableList<T>): MutableList<T> {
        val start = System.currentTimeMillis()
        return this.calculation().also { output ->
            lastRunStats = Statistics(
                milliseconds = System.currentTimeMillis() - start,
                rowCount = output.size
            )
        }
    }

    @Serializable
    abstract class AggregateParentTransform : Transform() {
        abstract override val surrogate: AggregateParentTransformSurrogate

        @Transient
        protected val aggregates = mutableStateListOf<Aggregate>()

        fun remove(aggregate: Aggregate) = aggregates.remove(aggregate)

        interface AggregateParentTransformSurrogate : TransformSurrogate {
            val aggregates: List<Aggregate>
        }
    }
    @Serializable
    abstract class ConditionParentTransform : Transform() {
        abstract override val surrogate: ConditionParentTransformSurrogate

        @Transient
        protected val conditions = mutableStateListOf<Condition>()

        open fun remove(condition: Condition) = conditions.remove(condition)

        interface ConditionParentTransformSurrogate : TransformSurrogate {
            val conditions: List<Condition>
        }
    }
    @Serializable
    abstract class ConditionalTransform(@Transient val parent: Transform? = null) : Transform() {
        abstract fun doTheConditionalHeaderThing(intermediate: MutableList<String>): MutableList<String>
        abstract fun doTheConditionalThing(intermediateRow: MutableMap<String, String>): MutableMap<String, String>
        abstract fun isValidConditional(context: Condition.Context): Boolean
        @Composable abstract fun ConditionalDialog(
            context: Condition.Context,
            onHide: () -> Unit,
            onDelete: () -> Unit
        )
        abstract fun adopt(parent: Transform): ConditionalTransform
    }

    data class Statistics(
        val milliseconds: Long,
        val rowCount: Int
    ) {
        val text by lazy {
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append("Last run stats")
                }
                append("\nTime taken: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(milliseconds.toString())
                }
                append("ms\nRow count: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(rowCount.toString())
                }
            }
        }
    }

    enum class Type(
        val uiName: String,
        val create: (parent: Transform?) -> Transform,
        val isConditional: Boolean
    ) {
        CONDITIONAL("Conditional", { ConditionalTransformSet() }, false),
        FILTER("Filter", { FilterTransform() }, false),
        GROUPBY("Group By", { GroupByTransform() }, false),
        JOIN("Join", { JoinTransform() }, false),
        MERGE("Merge", { MergeTransform() }, false),
        SELECT("Select", { SelectTransform() }, false),
        SET("Set", { SetTransform(it) }, true),
        SORT("Sort", { SortTransform() }, false),
        TOP("Top", { TopTransform() }, false)
    }

    interface TransformSurrogate
    object AggregateFosterParent : AggregateParentTransform() {
        override val description = buildAnnotatedString {
            withStyle(style = SpanStyle(Color.Red)) {
                append("You really shouldn't be seeing this :/")
            }
        }
        override val surrogate: AggregateParentTransformSurrogate
            get() = throw IllegalStateException()

        override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> =
            throw IllegalStateException()

        override suspend fun doTheActualThing(
            intermediate: MutableList<MutableMap<String, String>>
        ): MutableList<MutableMap<String, String>> = throw IllegalStateException()

        override fun isValid(instance: ToolkitInstance) = false

        @Composable
        override fun Dialog(instance: ToolkitInstance, onHide: () -> Unit, onDelete: () -> Unit) {
            throw IllegalStateException()
        }
    }
    object ConditionFosterParent : ConditionParentTransform() {
        override val description = buildAnnotatedString {
            withStyle(style = SpanStyle(Color.Red)) {
                append("You really shouldn't be seeing this :/")
            }
        }
        override val surrogate: ConditionParentTransformSurrogate
            get() = throw IllegalStateException()

        override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> =
            throw IllegalStateException()

        override suspend fun doTheActualThing(
            intermediate: MutableList<MutableMap<String, String>>
        ): MutableList<MutableMap<String, String>> = throw IllegalStateException()

        override fun isValid(instance: ToolkitInstance) = false

        @Composable
        override fun Dialog(instance: ToolkitInstance, onHide: () -> Unit, onDelete: () -> Unit) {
            throw IllegalStateException()
        }

        override fun remove(condition: Condition) = false
    }
}