package ch.icken.csvtoolkit.files

interface TabulatedFile {
    val path: String

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