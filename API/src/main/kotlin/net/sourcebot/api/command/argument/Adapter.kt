package net.sourcebot.api.command.argument

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.command.InvalidSyntaxException

/**
 * Represents an adapter to convert command [Arguments] to a desired [T]
 */
class Adapter<T>(adapter: (Arguments) -> T?) : (Arguments) -> T? by adapter {
    companion object {
        @JvmStatic
        fun <T> ofSingleArg(adapter: (String) -> T): Adapter<T> = Adapter {
            val arg = it.next() ?: return@Adapter null
            val result = arg.runCatching(adapter).getOrNull()
            return@Adapter if (result == null) {
                it.backtrack(); null
            } else result
        }

        @JvmStatic fun boolean() = ofSingleArg(String::toBoolean)

        @JvmStatic @JvmOverloads
        fun short(
            min: Short? = null,
            max: Short? = null,
            error: String? = null
        ): Adapter<Short> = when {
            min == null && max != null -> short().max(max, error)
            min != null && max == null -> short().min(min, error)
            min != null && max != null -> short().between(min, max, error)
            else -> ofSingleArg(String::toShort)
        }

        @JvmStatic @JvmOverloads
        fun int(
            min: Int? = null,
            max: Int? = null,
            error: String? = null
        ): Adapter<Int> = when {
            min == null && max != null -> int().max(max, error)
            min != null && max == null -> int().min(min, error)
            min != null && max != null -> int().between(min, max, error)
            else -> ofSingleArg(String::toInt)
        }

        @JvmStatic @JvmOverloads
        fun long(
            min: Long? = null,
            max: Long? = null,
            error: String? = null
        ): Adapter<Long> = when {
            min == null && max != null -> long().max(max, error)
            min != null && max == null -> long().min(min, error)
            min != null && max != null -> long().between(min, max, error)
            else -> ofSingleArg(String::toLong)
        }

        @JvmStatic @JvmOverloads
        fun float(
            min: Float? = null,
            max: Float? = null,
            error: String? = null
        ): Adapter<Float> = when {
            min == null && max != null -> float().max(max, error)
            min != null && max == null -> float().min(min, error)
            min != null && max != null -> float().between(min, max, error)
            else -> ofSingleArg(String::toFloat)
        }

        @JvmStatic @JvmOverloads
        fun double(
            min: Double? = null,
            max: Double? = null,
            error: String? = null
        ): Adapter<Double> = when {
            min == null && max != null -> double().max(max, error)
            min != null && max == null -> double().min(min, error)
            min != null && max != null -> double().between(min, max, error)
            else -> ofSingleArg(String::toDouble)
        }

        @JvmStatic
        fun member(guild: Guild) = ofSingleArg {
            val target = it.replace("<@!?(\\d+)>".toRegex(), "$1")
            val byId = target.runCatching(guild::getMemberById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byTag = target.runCatching(guild::getMemberByTag).getOrNull()
            if (byTag != null) return@ofSingleArg byTag
            val byName = target.runCatching {
                guild.getMembersByEffectiveName(this, true)
            }.getOrNull() ?: return@ofSingleArg null
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '$target' matches multiple members!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic
        fun role(guild: Guild) = ofSingleArg {
            val target = it.replace("<@&(\\d+)>".toRegex(), "$1")
            val byId = target.runCatching(guild::getRoleById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = target.runCatching {
                guild.getRolesByName(this, true)
            }.getOrNull() ?: return@ofSingleArg null
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '$target' matches multiple roles!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic
        fun textChannel(guild: Guild) = ofSingleArg {
            val target = it.replace("<#(\\d+)>".toRegex(), "$1")
            val byId = target.runCatching(guild::getTextChannelById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = target.runCatching {
                guild.getTextChannelsByName(this, true)
            }.getOrNull() ?: return@ofSingleArg null
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '${target}' matches multiple channels!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic
        fun category(guild: Guild) = ofSingleArg { target ->
            val byId = target.runCatching(guild::getCategoryById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = target.runCatching {
                guild.getCategoriesByName(this, true)
            }.getOrNull() ?: return@ofSingleArg null
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '$target' matches multiple categories!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic
        fun duration() = ofSingleArg { it.runCatching(DurationUtils::parseDuration).getOrNull() }
    }
}

private fun <T> Adapter<T>.between(
    min: T, max: T, error: String? = null
) where T : Comparable<T>, T : Number = Adapter {
    val read: T = this(it) ?: return@Adapter null
    if (read < min || read > max) throw InvalidSyntaxException(
        error ?: "Expected `$min` <= value <= `$max`, actual: `$read`!"
    )
    return@Adapter read
}

fun <T> Adapter<T>.min(min: T, error: String? = null) where T : Comparable<T>, T : Number = Adapter {
    val read: T = this(it) ?: return@Adapter null
    if (read < min) throw InvalidSyntaxException(
        error ?: "Expected value >= `$min`, actual: `$read`!"
    )
    else return@Adapter read
}

fun <T> Adapter<T>.max(max: T, error: String? = null) where T : Comparable<T>, T : Number = Adapter {
    val read: T = this(it) ?: return@Adapter null
    if (read > max) throw InvalidSyntaxException(
        error ?: "Expected value <= `$max`, actual: `$read`!"
    )
    else return@Adapter read
}