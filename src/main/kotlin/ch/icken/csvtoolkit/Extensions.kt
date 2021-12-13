package ch.icken.csvtoolkit

import androidx.compose.foundation.lazy.LazyListState

fun <E> List<E>.firstDuplicateOrNull(): E? {
    val set = mutableSetOf<E>()
    forEach { if (set.contains(it)) return it else set.add(it) }
    return null
}

suspend fun <T, R> Iterable<T>.foldSuspendable(initial: R, operation: suspend (R, T) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

inline fun <T, C : MutableIterable<T>> C.onEach(action: (T, MutableIterator<T>) -> Unit): C {
    val iterator = this.iterator()
    return apply { while (iterator.hasNext()) action(iterator.next(), iterator) }
}

fun LazyListState.calculateOffset(itemWidths: List<Float>): Float {
    if (itemWidths.isEmpty()) return 0f
    return (itemWidths.subList(0, firstVisibleItemIndex).takeIf { it.isNotEmpty() }
        ?.reduce { total, width -> total + width } ?: 0f) + firstVisibleItemScrollOffset
}

inline fun <E> MutableList<E>.set(index: Int, newElement: (oldElement: E) -> E) {
    set(index, newElement(this[index]))
}

inline fun String.lowercaseIf(predicate: (String) -> Boolean): String {
    return if (predicate(this)) lowercase() else this
}