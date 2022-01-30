package ch.icken.csvtoolkit.file

import androidx.compose.ui.text.input.TextFieldValue
import ch.icken.csvtoolkit.file.ExcelFile.ExcelSerializer
import ch.icken.csvtoolkit.util.excelReader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

@Serializable(with = ExcelSerializer::class)
class ExcelFile(
    path: String,
    private val sheetName: String?,
    uuid: String = UUID.randomUUID().toString()
) : TabulatedFile(path, uuid) {
    private val reader = excelReader {
        sheetName = this@ExcelFile.sheetName
    }

    override val headers: List<String> =
        reader.open(file) {
            val possibleHeader = readNext()
            //TODO check for duplicate headers
            if (possibleHeader == null) {
                state = State.INVALID
                listOf()
            } else {
                possibleHeader
            }
        }
    override val preview =
        reader.open(file) {
            (0..14).mapNotNull { readNext() }
        }
    override val surrogate get() = ExcelSurrogate(path, uuid, alias.text, keepInMemory, sheetName)

    val sheetNames = reader.open(file) { sheetNames }

    constructor(surrogate: ExcelSurrogate) : this(surrogate.path, surrogate.sheetName, surrogate.uuid) {
        alias = TextFieldValue(surrogate.alias)
        keepInMemory = surrogate.keepInMemory
    }

    override fun loadData(): List<Map<String, String>> {
        return reader.readAllWithHeader(file)
    }

    @Serializable
    @SerialName("excel")
    class ExcelSurrogate(
        override val path: String,
        override val uuid: String,
        override val alias: String,
        override val keepInMemory: Boolean,
        val sheetName: String?
    ) : FileSurrogate
    object ExcelSerializer : KSerializer<ExcelFile> {
        override val descriptor = ExcelSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: ExcelFile) {
            encoder.encodeSerializableValue(ExcelSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): ExcelFile {
            return ExcelFile(decoder.decodeSerializableValue(ExcelSurrogate.serializer()))
        }
    }
}