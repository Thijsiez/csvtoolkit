package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedButton
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
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.lowercaseIf
import ch.icken.csvtoolkit.transform.EditDialog
import ch.icken.csvtoolkit.transform.Transform.ConditionFosterParent
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.transform.condition.FileCondition.FileSerializer
import ch.icken.csvtoolkit.ui.Spinner
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = FileSerializer::class)
class FileCondition(
    override val parentTransform: ConditionParentTransform,
    override val parentCondition: ConditionParent?
) : Condition() {
    override val description get() = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(column ?: "?")
        }
        append(" is")
        if (!inFile) append(" not")
        append(" in ")
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(compareFile?.name ?: "?")
        }
        append('\'')
        if (compareFile?.name?.endsWith('s') == false) append('s')
        append(' ')
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(compareColumn ?: "?")
        }
    }
    override val surrogate get() = FileSurrogate(column, compareFile?.uuid, compareColumn, inFile, caseInsensitive)
    override val usesFiles get() = setOfNotNull(compareFile)

    private var column: String? by mutableStateOf(null)
    private var compareFile: TabulatedFile? by mutableStateOf(null)
    private var compareColumn: String? by mutableStateOf(null)
    private var inFile by mutableStateOf(true)
    private var caseInsensitive by mutableStateOf(false)

    //Used to find file when loading from project file
    private var compareFileUuid: String? = null
    //Used when running checks
    private var compareDataLookup = setOf<String>()

    constructor(surrogate: FileSurrogate) : this(ConditionFosterParent, null) {
        column = surrogate.column
        compareFileUuid = surrogate.compareFileUuid
        compareColumn = surrogate.compareColumn
        inFile = surrogate.inFile
        caseInsensitive = surrogate.caseInsensitive
    }

    override suspend fun prepareChecks(): Boolean {
        val compareFileValue = compareFile ?: return false
        val compareColumnName = compareColumn ?: return false
        compareDataLookup = compareFileValue.letData { data ->
            data.mapNotNull { it[compareColumnName]?.lowercaseIf { caseInsensitive } }.toSet()
        } ?: return false
        return true
    }
    override fun check(row: Map<String, String>): Boolean {
        val columnName = column ?: return false
        val referenceText = row[columnName]?.lowercaseIf { caseInsensitive } ?: return false
        return compareDataLookup.contains(referenceText) == inFile
    }

    override fun isValid(context: Context): Boolean {
        val columnName = column
        val compareFileValue = compareFile
        val compareColumnName = compareColumn

        if (columnName == null) {
            invalidMessage = "Missing reference column"
            return false
        }
        if (compareFileValue == null) {
            invalidMessage = "Missing file to compare"
            return false
        }
        if (compareColumnName == null) {
            invalidMessage = "Missing compare column"
            return false
        }
        if (columnName !in context.headers) {
            invalidMessage = "Reference column not available"
            return false
        }
        if (compareFileValue !in context.files) {
            invalidMessage = "File to compare not available"
            return false
        }
        if (compareColumnName !in compareFileValue.headers) {
            invalidMessage = "Compare column not available"
            return false
        }

        return true
    }

    @Composable
    override fun Dialog(
        context: Context,
        onHide: () -> Unit,
        onDelete: () -> Unit
    ) {
        EditDialog(
            titleText = "File Condition",
            onHide = onHide,
            onDelete = onDelete,
            state = rememberDialogState(
                size = DpSize(720.dp, Dp.Unspecified)
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
                    Spinner(
                        items = context.headers,
                        selectedItem = { column },
                        onItemSelected = { column = it },
                        itemTransform = { it ?: "-" },
                        label = "Reference Column"
                    )
                    OutlinedButton(
                        onClick = { inFile = !inFile },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (inFile) "IS IN" else "IS NOT IN")
                    }
                    Spinner(
                        items = context.files,
                        selectedItem = { compareFile },
                        onItemSelected = { compareFile = it },
                        itemTransform = { it?.name ?: "-" },
                        label = "Compare File"
                    )
                    Spinner(
                        items = compareFile?.headers ?: emptyList(),
                        selectedItem = { compareColumn },
                        onItemSelected = { compareColumn = it },
                        itemTransform = { it ?: "-" },
                        label = "Compare Column"
                    )
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

    @Serializable
    @SerialName("file")
    class FileSurrogate(
        val column: String?,
        val compareFileUuid: String?,
        val compareColumn: String?,
        val inFile: Boolean,
        val caseInsensitive: Boolean
    ) : ConditionSurrogate
    object FileSerializer : KSerializer<FileCondition> {
        override val descriptor = FileSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: FileCondition) {
            encoder.encodeSerializableValue(FileSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): FileCondition {
            return FileCondition(decoder.decodeSerializableValue(FileSurrogate.serializer()))
        }
    }
    override fun postDeserialization(context: Context) {
        compareFile = context.files.find { it.uuid == compareFileUuid }
    }
    override fun adopt(parentTransform: ConditionParentTransform, parentCondition: ConditionParent?): Condition {
        return FileCondition(parentTransform, parentCondition).also { copy ->
            copy.column = column
            copy.compareFile = compareFile
            copy.compareColumn = compareColumn
            copy.inFile = inFile
            copy.caseInsensitive = caseInsensitive
            copy.compareFileUuid = compareFileUuid
        }
    }
}