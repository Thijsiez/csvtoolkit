package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.lowercaseIf
import ch.icken.csvtoolkit.transform.SortTransform.SortSerializer
import ch.icken.csvtoolkit.ui.Spinner
import ch.icken.csvtoolkit.ui.Tooltip
import ch.icken.csvtoolkit.util.InterpretAs
import ch.icken.csvtoolkit.util.interpret
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SortSerializer::class)
class SortTransform() : Transform() {
    override val description get() = buildAnnotatedString {
        append("Sort by ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(if (ascending) ", ascending" else ", descending")
    }
    override val surrogate get() = SortSurrogate(column, ascending, interpretAs, caseInsensitive)

    private var column: String? by mutableStateOf(null)
    private var ascending by mutableStateOf(true)
    private var interpretAs by mutableStateOf(InterpretAs.TEXT)
    private var caseInsensitive by mutableStateOf(false)

    constructor(surrogate: SortSurrogate) : this() {
        column = surrogate.column
        ascending = surrogate.ascending
        interpretAs = surrogate.interpretAs
        caseInsensitive = surrogate.caseInsensitive
    }

    override fun doTheHeaderThing(intermediate: MutableList<String>) = intermediate

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        val columnName = column
        if (columnName == null ||
            intermediate.firstOrNull()?.containsKey(columnName) == false) return@coroutineScope intermediate
        return@coroutineScope intermediate.apply {
            sort(ascending) {
                val value = it[columnName]
                if (interpretAs == InterpretAs.TEXT) {
                    value?.lowercaseIf { caseInsensitive }
                } else {
                    value?.interpret(interpretAs)
                }
            }
        }
    }

    private fun <T> MutableList<T>.sort(ascending: Boolean, selector: (T) -> Comparable<*>?) {
        if (size > 1) sortWith(
            if (ascending) compareBy(selector) else compareByDescending(selector)
        )
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val columnName = column

        if (columnName == null) {
            invalidMessage = "Missing reference column"
            return false
        }
        if (columnName !in instance.headersUpTo(this)) {
            invalidMessage = "Reference column not available"
            return false
        }

        return super.isValid(instance)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        EditDialog(
            titleText = "Sort",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(420.dp, Dp.Unspecified)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sort by")
                    Spinner(
                        items = instance.headersUpTo(this@SortTransform),
                        selectedItem = { column },
                        onItemSelected = { column = it },
                        itemTransform = { it ?: "-" },
                        label = "Reference Column",
                        modifier = Modifier.requiredWidth(240.dp)
                    )
                    TooltipArea(
                        tooltip = { Tooltip(if (ascending) "Ascending" else "Descending") }
                    ) {
                        IconToggleButton(
                            checked = ascending,
                            onCheckedChange = { ascending = it }
                        ) {
                            Icon(
                                imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = if (ascending) "Ascending" else "Descending"
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spinner(
                        items = InterpretAs.values().asList(),
                        selectedItem = { interpretAs },
                        onItemSelected = { interpretAs = it },
                        itemTransform = { it.uiName },
                        label = "Interpret as"
                    )
                    if (interpretAs == InterpretAs.TEXT) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = caseInsensitive,
                                onCheckedChange = { isChecked ->
                                    caseInsensitive = isChecked
                                }
                            )
                            Text("Case Insensitive")
                        }
                    }
                }
            }
        }
    }

    @Serializable
    @SerialName("sort")
    class SortSurrogate(
        val column: String?,
        val ascending: Boolean,
        val interpretAs: InterpretAs,
        val caseInsensitive: Boolean
    ) : TransformSurrogate
    object SortSerializer : KSerializer<SortTransform> {
        override val descriptor = SortSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SortTransform) {
            encoder.encodeSerializableValue(SortSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): SortTransform {
            return SortTransform(decoder.decodeSerializableValue(SortSurrogate.serializer()))
        }
    }
}