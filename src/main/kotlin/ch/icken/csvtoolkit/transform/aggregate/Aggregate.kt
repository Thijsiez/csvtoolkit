package ch.icken.csvtoolkit.transform.aggregate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import ch.icken.csvtoolkit.transform.Transform.AggregateParentTransform
import kotlinx.serialization.Serializable

@Serializable
abstract class Aggregate {
    abstract val parentTransform: AggregateParentTransform
    abstract val description: AnnotatedString
    abstract val columnName: String
    abstract val surrogate: AggregateSurrogate

    var invalidMessage by mutableStateOf(""); protected set

    abstract fun aggregate(group: List<Map<String, String>>): String

    abstract fun isValid(context: Context): Boolean

    @Composable
    abstract fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    )

    abstract fun adopt(parentTransform: AggregateParentTransform): Aggregate

    data class Context(
        val headers: List<String>,
        val allowChanges: Boolean
    )

    @Suppress("unused")
    enum class Type(
        val uiName: String,
        val create: (parentTransform: AggregateParentTransform) -> Aggregate
    ) {
        AVERAGE("Average", { AverageAggregate(it) }),
        COUNT("Count", { CountAggregate(it) }),
        MINMAX("Min/Max", { MinMaxAggregate(it) }),
        SUM("Sum", { SumAggregate(it) })
    }

    interface AggregateSurrogate {
        val column: String?
        val asColumnName: String
    }
    protected object Error {
        const val NoReferencedData = "#N/A!"
        const val InvalidReference = "#REF!"
    }
}