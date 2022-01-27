package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.transform.TopTransform.TopSerializer
import ch.icken.csvtoolkit.util.NumberInterpreter.IntInvalidCharacterFilter
import ch.icken.csvtoolkit.util.interpretAsNumber
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TopSerializer::class)
class TopTransform() : Transform() {
    override val description get() = buildAnnotatedString {
        append("Keep only the first ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(numberOfRows.toString())
        }
        append(" rows")
    }
    override val surrogate get() = TopSurrogate(numberOfRows)

    private var limitTo by mutableStateOf(TextFieldValue())
    private val numberOfRows by derivedStateOf { limitTo.text.interpretAsNumber().toInt() }

    constructor(surrogate: TopSurrogate) : this() {
        limitTo = TextFieldValue(surrogate.limitTo.toString())
    }

    override fun doTheHeaderThing(intermediate: MutableList<String>) = intermediate

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        return@coroutineScope intermediate.take(numberOfRows) as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        if (limitTo.text.matches(IntInvalidCharacterFilter)) {
            invalidMessage = "Contains invalid characters"
            return false
        }
        if (numberOfRows <= 0) {
            invalidMessage = "No data will pass this transform"
            return false
        }

        return super.isValid(instance)
    }

    @Composable
    override fun Dialog(
        instance: ToolkitInstance,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        EditDialog(
            titleText = "Top",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(360.dp, Dp.Unspecified)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keep only the first ")
                OutlinedTextField(
                    value = limitTo,
                    onValueChange = {
                        limitTo = it.copy(
                            text = it.text.replace(IntInvalidCharacterFilter, "")
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    label = { Text("Amount of rows") },
                    placeholder = { Text("42") },
                    singleLine = true
                )
            }
        }
    }

    @Serializable
    @SerialName("top")
    class TopSurrogate(
        val limitTo: Int
    ) : TransformSurrogate
    object TopSerializer : KSerializer<TopTransform> {
        override val descriptor = TopSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: TopTransform) {
            encoder.encodeSerializableValue(TopSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): TopTransform {
            return TopTransform(decoder.decodeSerializableValue(TopSurrogate.serializer()))
        }
    }
}