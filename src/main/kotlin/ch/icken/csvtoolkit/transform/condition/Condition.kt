package ch.icken.csvtoolkit.transform.condition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform

abstract class Condition(val parent: Transform) {
    abstract val description: AnnotatedString

    var invalidMessage by mutableStateOf(""); protected set

    abstract fun check(row: Map<String, String>): Boolean

    abstract fun isValid(context: ConditionalTransform.Context): Boolean

    @Composable
    abstract fun Dialog(
        context: ConditionalTransform.Context,
        onHide: () -> Unit
    )

    @Suppress("unused")
    enum class Type(
        val uiName: String,
        val create: (parent: Transform) -> Condition
    ) {
        NUMERICAL("Numerical", { NumericalCondition(it) }),
        TEXT("Text", { TextCondition(it) }),
        REGEX("RegEx", { RegexCondition(it) }),
        AND("And", { AndCondition(it) }),
        OR("Or", { OrCondition(it) })
    }
}