package ch.icken.csvtoolkit.file

import androidx.compose.ui.text.input.TextFieldValue
import ch.icken.csvtoolkit.file.TsvFile.TsvSerializer
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable(with = TsvSerializer::class)
class TsvFile(
    path: String,
    uuid: String = UUID.randomUUID().toString()
) : TabulatedFile(path, uuid) {
    private val reader = csvReader {
        delimiter = '\t'
        escapeChar = '\\'
    }

    override val headers =
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
    override val surrogate get() = TsvSurrogate(path, uuid, alias.text, keepInMemory)

    constructor(surrogate: TsvSurrogate) : this(surrogate.path, surrogate.uuid) {
        alias = TextFieldValue(surrogate.alias)
        keepInMemory = surrogate.keepInMemory
    }

    override fun loadData(): List<Map<String, String>> {
        return reader.readAllWithHeader(file)
    }

    @Serializable
    @SerialName("tsv")
    class TsvSurrogate(
        override val path: String,
        override val uuid: String,
        override val alias: String,
        override val keepInMemory: Boolean
    ) : FileSurrogate
    object TsvSerializer : KSerializer<TsvFile> {
        override val descriptor = TsvSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: TsvFile) {
            encoder.encodeSerializableValue(TsvSurrogate.serializer(), value.surrogate)
        }

        override fun deserialize(decoder: Decoder): TsvFile {
            return TsvFile(decoder.decodeSerializableValue(TsvSurrogate.serializer()))
        }
    }
}