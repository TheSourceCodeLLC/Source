package net.sourcebot.api.command.argument

import net.sourcebot.api.command.InvalidSyntaxException

/**
 * Represents iterable command arguments
 * Arguments can be requested optionally or exceptionally
 * Arguments can also be converted to a specific type by specifying a proper adapter function
 */
class Arguments(private var raw: Array<String>) : Iterator<String?> {
    private var index = 0
    override fun hasNext() = index + 1 <= raw.size

    /**
     * Gets the next argument as a string optionally
     */
    override fun next(): String? = if (hasNext()) raw[index++] else null

    /**
     * Gets the next argument as a string, throwing if it is absent
     */
    fun next(error: String) = next() ?: throw InvalidSyntaxException(error)

    /**
     * Gets the next [T] optionally
     */
    fun <T> next(adapter: (Arguments) -> T?) = adapter(this)

    /**
     * Gets the next [T] exceptionally, throwing if it is absent or malformed
     */
    fun <T> next(adapter: (Arguments) -> T?, error: String) = next(adapter) ?: throw InvalidSyntaxException(error)

    /**
     * Joins the remaining arguments into a single argument, delimited by [delimiter]
     * @param[delimiter] The delimiter used to join each argument
     * @return The joined arguments or null if there were no arguments to join.
     */
    fun slurp(delimiter: String): String? {
        val slurped = raw.copyOfRange(index, raw.size).joinToString(delimiter)
        return if (slurped.isEmpty()) null else slurped
    }

    /**
     * Joins the remaining arguments into a single argument, throwing if there are none.
     */
    fun slurp(delimiter: String, error: String) = slurp(delimiter) ?: throw InvalidSyntaxException(error)

    /**
     * Reduces [amount] from the current [index] to 'undo' an argument reading.
     */
    fun backtrack(amount: Int = 1) {
        index -= amount
    }
}