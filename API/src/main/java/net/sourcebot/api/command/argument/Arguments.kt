package net.sourcebot.api.command.argument

import net.sourcebot.api.command.InvalidSyntaxException

class Arguments(private var raw: Array<String>) : Iterator<String?> {
    private var index = 0
    override fun hasNext() = index + 1 <= raw.size

    override fun next(): String? = if (hasNext()) raw[index++] else null
    fun next(error: String) = next() ?: throw InvalidSyntaxException(error)

    fun <T> next(adapter: (Arguments) -> T?) = adapter(this)
    fun <T> next(adapter: (Arguments) -> T?, error: String) = next(adapter) ?: throw InvalidSyntaxException(error)

    fun slurp(delimiter: String): String? {
        val slurped = raw.copyOfRange(index, raw.size).joinToString(delimiter)
        return if (slurped.isEmpty()) null else slurped
    }

    fun slurp(delimiter: String, error: String) = slurp(delimiter) ?: throw InvalidSyntaxException(error)

    fun backtrack(amount: Int = 1) {
        index -= amount
    }
}