package ch.icken.csvtoolkit.util

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

//I'm imitating doyaaaaaken's kotlin-csv API here, I quite like it
class ExcelReader(
    private val config: ExcelReaderConfig
) : ExcelReaderConfig by config {
    fun <R> open(file: File, block: ExcelFileReader.() -> R): R {
        return ExcelFileReader(file, config).use(block)
    }

    fun readAllWithHeader(file: File): List<Map<String, String>> {
        return open(file) { readAllWithHeader().toList() }
    }
}
interface ExcelReaderConfig {
    var sheetNumber: Int
    var sheetName: String?
    var cellTransform: (Cell?) -> String?
}
private class DefaultExcelReaderConfig : ExcelReaderConfig {
    override var sheetNumber: Int = 0
    override var sheetName: String? = null
    override var cellTransform: (Cell?) -> String? = { cell ->
        when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toString()
            CellType.STRING -> cell.stringCellValue
            CellType.FORMULA -> cell.numericCellValue.toString()
            CellType.BLANK -> null
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> null
        }
    }
}
fun excelReader(init: ExcelReaderConfig.() -> Unit = {}): ExcelReader {
    return ExcelReader(DefaultExcelReaderConfig().apply(init))
}

class ExcelFileReader(
    file: File,
    private val config: ExcelReaderConfig,
) : AutoCloseable {
    private val workbook: Workbook = WorkbookFactory.create(file)
    private val sheet: Sheet? by lazy {
        with(workbook) { when {
            numberOfSheets == 1 -> getSheetAt(0)
            config.sheetName != null && getSheet(config.sheetName) != null -> getSheet(config.sheetName)
            config.sheetNumber in 0 until numberOfSheets -> getSheetAt(config.sheetNumber)
            else -> getSheetAt(0)
        } }
    }
    private var currentRowNumber = 0

    val sheetNames: List<String> get() {
        return (0 until workbook.numberOfSheets).map { index ->
            workbook.getSheetName(index)
        }
    }

    fun readNext() = readRow()

    private fun readAll(numberOfColumns: Int? = null): Sequence<List<String>> {
        return generateSequence { readRow(numberOfColumns) }
    }

    fun readAllWithHeader(): Sequence<Map<String, String>> {
        val headers = readRow() ?: return emptySequence()
        //TODO check for duplicate headers
        return readAll(headers.size).map { fields ->
            headers.zip(fields).toMap()
        }
    }

    private fun readRow(numberOfCells: Int? = null): List<String>? {
        val row = sheet?.getRow(currentRowNumber++)
        return when {
            row == null -> null
            numberOfCells == null -> {
                //Read until we encounter an empty cell
                var currentColumnNumber = 0
                generateSequence {
                    config.cellTransform(row.getCell(currentColumnNumber++))
                }.toList().takeIf { it.isNotEmpty() }
            }
            else -> {
                //Read the specified number of cells
                (0 until numberOfCells).map {
                    config.cellTransform(row.getCell(it)) ?: ""
                }
            }
        }
    }

    override fun close() = workbook.close()
}