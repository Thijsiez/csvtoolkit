package ch.icken.csvtoolkit.mutation

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.firstDuplicateOrNull

abstract class Mutation(
    val type: Type
) {
    companion object {
        fun create(type: Type) = when (type) {
            Type.JOIN -> JoinMutation()
        }
    }

    abstract val description: AnnotatedString

    abstract fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String>
    abstract fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>>

    @Composable
    abstract fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit
    )

    open fun isValid(instance: ToolkitInstance): Boolean {
        return instance.headersUpTo(this, true).firstDuplicateOrNull() == null
    }

    enum class Type(
        val uiName: String
    ) {
        JOIN("Join")
    }
}