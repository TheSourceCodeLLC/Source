package net.sourcebot.api.command.argument

import net.sourcebot.api.command.InvalidSyntaxException

/**
 * Represents iterable command arguments
 * Arguments can be requested optionally or exceptionally
 * Arguments can also be converted to a specific type by specifying a proper adapter function
 */
class Arguments(private val raw: Array<String>) : Iterator<String?> {
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
    fun <T> next(adapter: (Arguments) -> T?) =
        try {
            adapter(this)
        } catch (ex: InvalidSyntaxException) {
            throw InvalidSyntaxException(ex.message!!)
        } catch (ex: Throwable) {
            throw InvalidSyntaxException(ex)
        }

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
        val slurped = raw.copyOfRange(index, raw.size).joinToString(delimiter).trim()
        return if (slurped.isEmpty()) {
            null
        } else slurped
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

    /**
     * Returns a copy of this [Arguments] raw [String] arguments
     */
    fun rawCopy(): Array<String> = raw.copyOf()

    /**
     * Returns a slice of the raw arguments from the current index
     */
    fun remaining(): Array<String> = raw.slice(index until raw.size).toTypedArray()

    fun current() = raw[index]

    operator fun plus(other: Arguments) = Arguments(
        remaining() + other.remaining()
    )

    companion object {
        /**
         * Reads arguments from a given input string
         * Arguments are delimited by whitespace unless they are wrapped in quotes.
         * Quotes and other characters may be escaped.
         *
         * @param[input] The input String to read arguments from
         * @return An [Array<String>] of the read arguments
         */
        @JvmStatic
        fun parse(input: String): Arguments {
            val raw = mutableListOf<String>()
            var current = String()
            var shouldEscape = false
            var insideQuotes = false
            for (it in input.toCharArray()) {
                if (shouldEscape) {
                    current += it; shouldEscape = false; continue
                }
                if (it == '\\') {
                    shouldEscape = true; continue
                }
                if (it == '\"') {
                    if (insideQuotes) {
                        insideQuotes = false
                        raw += current
                        current = String()
                        continue
                    } else {
                        insideQuotes = true; continue
                    }
                }
                if (it.isWhitespace() && !insideQuotes) {
                    if (current.isNotEmpty()) {
                        raw += current
                        current = String()
                    }
                    continue
                }
                current += it
            }
            if (current.isNotEmpty()) raw += current
            return Arguments(raw.toTypedArray())
        }
    }
}