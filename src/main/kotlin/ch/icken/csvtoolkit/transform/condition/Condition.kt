package ch.icken.csvtoolkit.transform.condition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
abstract class Condition {
    abstract val parentTransform: ConditionParentTransform
    abstract val parentCondition: ConditionParent?
    abstract val description: AnnotatedString
    abstract val surrogate: ConditionSurrogate
    open val usesFiles = setOf<TabulatedFile>()

    var invalidMessage by mutableStateOf(""); protected set

    open suspend fun prepareChecks(): Boolean = true
    abstract fun check(row: Map<String, String>): Boolean

    abstract fun isValid(context: Context): Boolean

    @Composable
    abstract fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    )

    open fun postDeserialization(context: Context) {}
    abstract fun adopt(parentTransform: ConditionParentTransform, parentCondition: ConditionParent?): Condition

    @Serializable
    abstract class ConditionParent : Condition() {
        abstract override val surrogate: ConditionParentSurrogate

        @Transient
        protected val conditions = mutableStateListOf<Condition>()

        fun remove(condition: Condition) = conditions.remove(condition)

        interface ConditionParentSurrogate : ConditionSurrogate {
            val conditions: List<Condition>
        }
    }
    data class Context(
        val headers: List<String>,
        val files: List<TabulatedFile>,
        val allowChanges: Boolean
    )

    @Suppress("unused")
    enum class Type(
        val uiName: String,
        val create: (parentTransform: ConditionParentTransform, parentCondition: ConditionParent?) -> Condition
    ) {
        AND("And", { transform, condition -> AndCondition(transform, condition) }),
        FILE("File", { transform, condition -> FileCondition(transform, condition) }),
        LIST("List", { transform, condition -> ListCondition(transform, condition) }),
        NUMERICAL("Numerical", { transform, condition -> NumericalCondition(transform, condition) }),
        OR("Or", { transform, condition -> OrCondition(transform, condition) }),
        REGEX("RegEx", { transform, condition -> RegexCondition(transform, condition) }),
        TEXT("Text", { transform, condition -> TextCondition(transform, condition) })
    }

    interface ConditionSurrogate
}