package ch.icken.csvtoolkit.transform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.lowercaseIf
import ch.icken.csvtoolkit.onEach
import ch.icken.csvtoolkit.transform.JoinTransform.JoinSerializer
import ch.icken.csvtoolkit.ui.Spinner
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = JoinSerializer::class)
class JoinTransform() : Transform() {
    override val description get() = buildAnnotatedString {
        append(joinType.uiName)
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" on ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(joinOnFile?.name ?: "?")
        }
        append('\'')
        if (joinOnFile?.name?.endsWith('s') == false) append('s')
        append(' ')
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(joinOnColumn ?: "?")
        }
    }
    override val surrogate get() = JoinSurrogate(column, joinType, joinOnFile?.uuid, joinOnColumn, caseInsensitive)
    override val usesFiles get() = setOfNotNull(joinOnFile)

    private var column: String? by mutableStateOf(null)
    private var joinType by mutableStateOf(Type.INNER)
    private var joinOnFile: TabulatedFile? by mutableStateOf(null)
    private var joinOnColumn: String? by mutableStateOf(null)
    private var caseInsensitive by mutableStateOf(false)

    //Used to find file when loading from project file
    private var joinOnFileUuid: String? = null

    constructor(surrogate: JoinSurrogate) : this() {
        column = surrogate.column
        joinType = surrogate.joinType
        joinOnFileUuid = surrogate.joinOnFileUuid
        joinOnColumn = surrogate.joinOnColumn
        caseInsensitive = surrogate.caseInsensitive
    }

    override fun doTheHeaderThing(intermediate: MutableList<String>): MutableList<String> {
        val joinOnFileValue = joinOnFile
        val joinOnColumnName = joinOnColumn

        if (joinOnFileValue == null ||
            joinOnColumnName == null) return intermediate

        return intermediate.apply {
            addAll(joinOnFileValue.headers.filterNot { it == joinOnColumnName })
        }
    }

    override suspend fun doTheActualThing(
        intermediate: MutableList<MutableMap<String, String>>
    ): MutableList<MutableMap<String, String>> = coroutineScope {
        val columnName = column
        val joinOnFileValue = joinOnFile
        val joinOnColumnName = joinOnColumn

        if (columnName == null ||
            joinOnFileValue == null ||
            joinOnColumnName == null) return@coroutineScope intermediate

        val joinDataLookup = joinOnFileValue.letData { data ->
            data.associate {
                val key = it[joinOnColumnName]?.lowercaseIf { caseInsensitive }
                key to it.filterNot { (columnName, _) -> columnName == joinOnColumnName }
            }
        }
        val joinLeftEmpty = joinOnFileValue.headers.filterNot { it == joinOnColumnName }.associateWith { "" }
        return@coroutineScope intermediate.chunked(chunkSize(intermediate.size)).map { chunk ->
            async {
                when (joinType) {
                    Type.INNER -> {
                        (chunk as MutableList).onEach { row, iterator ->
                            val joinData = joinDataLookup?.get(row[columnName]?.lowercaseIf { caseInsensitive })
                            if (joinData != null) row.putAll(joinData) else iterator.remove()
                        }
                    }
                    Type.LEFT -> {
                        chunk.onEach { row ->
                            val joinData = joinDataLookup?.get(row[columnName]?.lowercaseIf { caseInsensitive })
                            row.putAll(joinData ?: joinLeftEmpty)
                        }
                    }
                }
            }
        }.awaitAll().flatten() as MutableList<MutableMap<String, String>>
    }

    override fun isValid(instance: ToolkitInstance): Boolean {
        val columnName = column
        val joinOnFileValue = joinOnFile
        val joinOnColumnName = joinOnColumn

        if (columnName == null) {
            invalidMessage = "Missing left column"
            return false
        }
        if (joinOnFileValue == null) {
            invalidMessage = "Missing file to join on"
            return false
        }
        if (joinOnColumnName == null) {
            invalidMessage = "Missing right column"
            return false
        }
        if (columnName !in instance.headersUpTo(this)) {
            invalidMessage = "Left column not available"
            return false
        }
        if (joinOnFileValue !in instance.files) {
            invalidMessage = "File to join on not available"
            return false
        }
        if (joinOnColumnName !in joinOnFileValue.headers) {
            invalidMessage = "Right column not available"
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
            titleText = "Join",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(480.dp, Dp.Unspecified)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spinner(
                            items = instance.headersUpTo(this@JoinTransform),
                            selectedItem = { column },
                            onItemSelected = { column = it },
                            itemTransform = { it ?: "-" },
                            label = "Reference Column"
                        )
                        Spinner(
                            items = Type.values().asList(),
                            selectedItem = { joinType },
                            onItemSelected = { joinType = it },
                            itemTransform = { it.uiName },
                            label = "Join Type"
                        )
                    }
                    Text("ON")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spinner(
                            items = instance.files,
                            selectedItem = { joinOnFile },
                            onItemSelected = { joinOnFile = it },
                            itemTransform = { it?.name ?: "-" },
                            label = "Join File"
                        )
                        Spinner(
                            items = joinOnFile?.headers ?: emptyList(),
                            selectedItem = { joinOnColumn },
                            onItemSelected = { joinOnColumn = it },
                            itemTransform = { it ?: "-" },
                            label = "Join Column"
                        )
                    }
                }
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

    enum class Type(
        val uiName: String
    ) {
        INNER("Inner Join"),
        LEFT("Left Join")
    }

    @Serializable
    @SerialName("join")
    class JoinSurrogate(
        val column: String?,
        val joinType: Type,
        val joinOnFileUuid: String?,
        val joinOnColumn: String?,
        val caseInsensitive: Boolean
    ) : TransformSurrogate
    object JoinSerializer : KSerializer<JoinTransform> {
        override val descriptor = JoinSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: JoinTransform) {
            encoder.encodeSerializableValue(JoinSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): JoinTransform {
            return JoinTransform(decoder.decodeSerializableValue(JoinSurrogate.serializer()))
        }
    }
    override fun postDeserialization(instance: ToolkitInstance) {
        joinOnFile = instance.files.find { it.uuid == joinOnFileUuid }
    }
}