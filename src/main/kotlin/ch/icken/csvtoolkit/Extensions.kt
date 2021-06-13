package ch.icken.csvtoolkit

fun <E> List<E>.firstDuplicateOrNull(): E? {
    val set = mutableSetOf<E>()
    forEach { if (set.contains(it)) return it else set.add(it) }
    return null
}