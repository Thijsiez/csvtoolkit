package ch.icken.csvtoolkit.file

import androidx.compose.ui.text.input.TextFieldValue
import ch.icken.csvtoolkit.file.CsvFile.CsvSerializer
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

@Serializable(with = CsvSerializer::class)
class CsvFile(
    path: String,
    private val delimiter: Delimiter,
    uuid: String = UUID.randomUUID().toString()
) : TabulatedFile(path, uuid) {
    private val reader = csvReader {
        delimiter = this@CsvFile.delimiter.character
    }

    override val headers =
        reader.open(file) {
            val possibleHeader = readNext()
            if (possibleHeader == null) {
                state = State.INVALID
                listOf()
            } else {
                possibleHeader
            }
        }
    override val preview =
        reader.open(file) {
            (0..10).mapNotNull { readNext() }
        }
    override val surrogate get() = CsvSurrogate(path, uuid, alias.text, keepInMemory, delimiter)

    constructor(surrogate: CsvSurrogate) : this(surrogate.path, surrogate.delimiter, surrogate.uuid) {
        alias = TextFieldValue(surrogate.alias)
        keepInMemory = surrogate.keepInMemory
    }

    override fun loadData(): List<Map<String, String>> {
        return reader.readAllWithHeader(file)
    }

    @Suppress("unused")
    enum class Delimiter(
        val uiName: String,
        val character: Char
    ) {
        COMMA("Comma", ','),
        SEMICOLON("Semicolon", ';')
    }

    @Serializable
    @SerialName("csv")
    class CsvSurrogate(
        override val path: String,
        override val uuid: String,
        override val alias: String,
        override val keepInMemory: Boolean,
        val delimiter: Delimiter
    ) : FileSurrogate
    object CsvSerializer : KSerializer<CsvFile> {
        override val descriptor = CsvSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: CsvFile) {
            encoder.encodeSerializableValue(CsvSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): CsvFile {
            return CsvFile(decoder.decodeSerializableValue(CsvSurrogate.serializer()))
        }
    }
}