package net.sourcebot.api.command.argument

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
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
        val slurped = raw.copyOfRange(index, raw.size).joinToString(delimiter).trim()
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

    fun nextMember(guild: Guild): Member? {
        val target = next()?.replace("<@!?(\\d+)>".toRegex(), "$1") ?: return null
        try {
            val byId = guild.getMemberById(target)
            if (byId != null) return byId
        } catch (ignored: Throwable) {
        }
        try {
            val byTag = guild.getMemberByTag(target)
            if (byTag != null) return byTag
        } catch (ignored: Throwable) {
        }
        try {
            val byEffectiveName = guild.getMembersByEffectiveName(target, true)
            if (byEffectiveName.isNotEmpty()) return byEffectiveName[0]
        } catch (ex: Throwable) {
        }
        return null
    }

    fun nextMember(guild: Guild, error: String) =
        nextMember(guild) ?: throw InvalidSyntaxException(error)

    fun nextRole(guild: Guild): Role? {
        val target = next()?.replace("<@&(\\d+)>".toRegex(), "$1") ?: return null
        try {
            val byId = guild.getRoleById(target)
            if (byId != null) return byId
        } catch (ignored: Throwable) {
        }
        try {
            val byName = guild.getRolesByName(target, true)
            if (byName.isNotEmpty()) return byName[0]
        } catch (ignored: Throwable) {
        }
        return null
    }

    fun nextRole(guild: Guild, error: String) =
        nextRole(guild) ?: throw InvalidSyntaxException(error)
}