package ch.icken.csvtoolkit.util

enum class InterpretAs(
    val uiName: String
) {
    TEXT("Text"),
    NUMBER("Number");

    operator fun invoke(value: String): Comparable<*> = when (this) {
        TEXT -> TextInterpreter.interpret(value)
        NUMBER -> NumberInterpreter.interpret(value)
    }
}
fun String.interpret(interpretAs: InterpretAs) = interpretAs(this)
fun String.interpretAsNumber() = NumberInterpreter.interpret(this)

interface Interpreter<T : Comparable<T>> {
    fun interpret(value: String): T
}
object TextInterpreter : Interpreter<String> {
    override fun interpret(value: String) = value
}
object NumberInterpreter : Interpreter<Double> {
    val IntInvalidCharacterFilter = Regex("[^-0-9]")
    val FloatInvalidCharacterFilter = Regex("[^-0-9.]")
    override fun interpret(value: String) = value.toDoubleOrNull() ?: Double.NaN
}