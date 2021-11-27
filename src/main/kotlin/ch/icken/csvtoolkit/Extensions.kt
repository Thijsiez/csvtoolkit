package ch.icken.csvtoolkit

import androidx.compose.foundation.lazy.LazyListState

fun <E> List<E>.firstDuplicateOrNull(): E? {
    val set = mutableSetOf<E>()
    forEach { if (set.contains(it)) return it else set.add(it) }
    return null
}

//fold, but as a suspending function
suspend fun <T, R> fold(iterable: Iterable<T>, initial: R, operation: suspend (R, T) -> R): R {
    var accumulator = initial
    for (element in iterable) accumulator = operation(accumulator, element)
    return accumulator
}

//onEach, but allows for removal of element while iterating
inline fun <T, C : MutableIterable<T>> C.onEach(action: (T, MutableIterator<T>) -> Unit): C {
    val iterator = this.iterator()
    return apply { while (iterator.hasNext()) action(iterator.next(), iterator) }
}

fun LazyListState.calculateOffset(itemWidths: List<Float>): Float {
    if (itemWidths.isEmpty()) return 0f
    return (itemWidths.subList(0, firstVisibleItemIndex).takeIf { it.isNotEmpty() }
        ?.reduce { total, width -> total + width } ?: 0f) + firstVisibleItemScrollOffset
}

fun <E> MutableList<E>.set(index: Int, newElement: (oldElement: E) -> E) {
    set(index, newElement(this[index]))
}