package net.sourcebot.api.menus

import com.google.common.collect.Lists

abstract class Menu<T>(
    options: List<T>,
    optsPerPage: Int,
    private val descriptor: (List<T>) -> String
) {
    private val pages: List<List<T>> = Lists.partition(options, optsPerPage)
    protected val iterator = pages.listIterator()
    protected var page = iterator.next()

    fun hasNext() = iterator.nextIndex() < pages.size
    fun next(): Menu<T> {
        page = iterator.next()
        return this
    }

    fun hasPrev() = iterator.previousIndex() > 0
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