package ch.icken.csvtoolkit.files

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

abstract class TabulatedFile(
    val type: Type,
    val path: String
) {

    val file: File get() = File(path)
    val name: String get() = file.name
    val isDataLoaded: MutableState<Boolean> = mutableStateOf(false)
    val data: List<Map<String, String>> by lazy {
        loadData().also { isDataLoaded.value = true }
    }

    abstract val preview: List<List<String>>

    protected abstract fun loadData(): List<Map<String, String>>

    enum class Type(
        val uiName: String
    ) {
        CSV("CSV"),
        EXCEL("Excel")
    }

    enum class TypeCsvDelimiter(
        val uiName: String,
        val character: Char
    ) {
        COMMA("Comma", ','),
        SEMICOLON("Semicolon", ';')
    }
}

class CsvFile(
    path: String,
    private val delimiter: TypeCsvDelimiter,
) : TabulatedFile(
    type = Type.CSV,
    path = path
) {
    private val reader = csvReader {
        delimiter = this@CsvFile.delimiter.character
    }

    override val preview =
        reader.open(file) {
            (0..13).mapNotNull { readNext() }
        }

    override fun loadData(): List<Map<String, String>> {
        return reader.readAllWithHeader(file)
    }
}