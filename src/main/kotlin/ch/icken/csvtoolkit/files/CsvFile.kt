package ch.icken.csvtoolkit.files

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader

class CsvFile(
    path: String,
    private val delimiter: Delimiter,
) : TabulatedFile(
    type = Type.CSV,
    path = path,
    alias = null
) {
    private val reader = csvReader {
        delimiter = this@CsvFile.delimiter.character
    }

    override val headers =
        reader.open(file) {
            val possibleHeader = readNext()
            if (possibleHeader == null) {
                state.value = State.INVALID
                listOf()
            } else {
                possibleHeader
            }
        }
    override val preview =
        reader.open(file) {
            (0..13).mapNotNull { readNext() }
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
}