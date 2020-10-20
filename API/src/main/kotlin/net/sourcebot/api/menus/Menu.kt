package net.sourcebot.api.menus

import com.google.common.collect.Lists

abstract class Menu<T>(
    options: List<T>,
    optsPerPage: Int,
    private val descriptor: (List<T>) -> String
) {
    private val pages: List<List<T>> = Lists.partition(options, optsPerPage)
    protected val iterator = object : ListIterator<List<T>> {
        private var index = -1

        override fun nextIndex() = index + 1
        override fun hasNext() = nextIndex() < pages.size
        override fun next() = pages[++index]

        override fun previousIndex() = index - 1
        override fun hasPrevious() = previousIndex() >= 0
        override fun previous() = pages[--index]

    }
    protected var page = iterator.next()

    fun hasNext() = iterator.hasNext()
    fun next(): Menu<T> {
        page = iterator.next()
        return this
    }

    fun hasPrev() = iterator.hasPrevious()
    fun previous(): Menu<T> {
        page = iterator.previous()
        return this
    }

    fun numOptions() = page.size

    fun render() = MenuResponse(this).apply {
        setDescription(
            """
            ${descriptor(page)}
            
            Page ${iterator.nextIndex()} of ${pages.size}
        """.trimIndent()
        )
    }

    open fun closable() = true
}