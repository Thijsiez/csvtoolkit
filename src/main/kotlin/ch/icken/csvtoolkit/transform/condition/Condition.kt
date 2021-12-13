package ch.icken.csvtoolkit.transform.condition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform

abstract class Condition(
    val parentTransform: ConditionParentTransform,
    val parentCondition: ConditionParent?
) {
    abstract val description: AnnotatedString

    var invalidMessage by mutableStateOf(""); protected set

    abstract fun check(row: Map<String, String>): Boolean

    abstract fun isValid(context: Context): Boolean

    @Composable
    abstract fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    )

    abstract class ConditionParent(
        parentTransform: ConditionParentTransform,
        parentCondition: ConditionParent?
    ) : Condition(parentTransform, parentCondition) {
        protected val conditions = mutableStateListOf<Condition>()

        fun remove(condition: Condition) = conditions.remove(condition)
    }
    data class Context(
        val headers: List<String>
    )

    @Suppress("unused")
    enum class Type(
        val uiName: String,
        val create: (parentTransform: ConditionParentTransform, parentCondition: ConditionParent?) -> Condition
    ) {
        NUMERICAL("Numerical", { transform, condition -> NumericalCondition(transform, condition) }),
        TEXT("Text", { transform, condition -> TextCondition(transform, condition) }),
        REGEX("RegEx", { transform, condition -> RegexCondition(transform, condition) }),
        AND("And", { transform, condition -> AndCondition(transform, condition) }),
        OR("Or", { transform, condition -> OrCondition(transform, condition) })
    }
}