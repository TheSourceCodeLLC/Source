package net.sourcebot.api.command.argument

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.command.InvalidSyntaxException
import java.time.Duration

/**
 * Represents an adapter to convert command [Arguments] to a desired [T]
 */
@Suppress("UNCHECKED_CAST", "UNUSED")
class Adapter<T>(adapter: (Arguments) -> T?) : (Arguments) -> T? by adapter {
    companion object {
        /**
         * Creates an [Adapter] using a single [String] argument from a set of [Arguments]
         * If [Arguments.next] returns null, this method returns null.
         * If the [adapter] returns null, the active [Arguments] instance is backtracked and null is returned.
         *
         * @param adapter The function to turn a [String] into an instance of [T] or null
         * @return The transformed argument as [T] or null if the argument could not be transformed.
         */
        @JvmStatic fun <T> ofSingleArg(adapter: (String) -> T?): Adapter<T> = Adapter {
            val arg = it.next() ?: return@Adapter null
            val result = arg.runCatching(adapter).getOrNull()
            return@Adapter if (result == null) {
                it.backtrack(); null
            } else result
        }

        /**
         * Creates an [Adapter] that turns a [String] into a [Boolean]
         */
        @JvmStatic fun boolean() = ofSingleArg(String::toBoolean)

        /**
         * Creates an [Adapter] that turns a [String] into a [Short]
         * @param min The minimum value (inclusive) for this [Short] or null if not needed
         * @param max The maximum value (inclusive) for this [Short] or null if not needed
         * @param error The error to send if this [Short] is not between [min] and [max]
         */
        @JvmStatic @JvmOverloads
        fun short(
            min: Short? = null,
            max: Short? = null,
            error: String? = null
        ): Adapter<Short> = when {
            min == null && max != null -> short().max<Short>(max, error)
            min != null && max == null -> short().min<Short>(min, error)
            min != null && max != null -> short().between<Short>(min, max, error)
            else -> ofSingleArg(String::toShort)
        }

        /**
         * Creates an [Adapter] that turns a [String] into a [Int]
         * @param min The minimum value (inclusive) for this [Int] or null if not needed
         * @param max The maximum value (inclusive) for this [Int] or null if not needed
         * @param error The error to send if this [Int] is not between [min] and [max]
         */
        @JvmStatic @JvmOverloads
        fun int(
            min: Int? = null,
            max: Int? = null,
            error: String? = null
        ): Adapter<Int> = when {
            min == null && max != null -> int().max<Int>(max, error)
            min != null && max == null -> int().min<Int>(min, error)
            min != null && max != null -> int().between<Int>(min, max, error)
            else -> ofSingleArg(String::toInt)
        }

        /**
         * Creates an [Adapter] that turns a [String] into a [Long]
         * @param min The minimum value (inclusive) for this [Long] or null if not needed
         * @param max The maximum value (inclusive) for this [Long] or null if not needed
         * @param error The error to send if this [Long] is not between [min] and [max]
         */
        @JvmStatic @JvmOverloads
        fun long(
            min: Long? = null,
            max: Long? = null,
            error: String? = null
        ): Adapter<Long> = when {
            min == null && max != null -> long().max<Long>(max, error)
            min != null && max == null -> long().min<Long>(min, error)
            min != null && max != null -> long().between<Long>(min, max, error)
            else -> ofSingleArg(String::toLong)
        }

        /**
         * Creates an [Adapter] that turns a [String] into a [Float]
         * @param min The minimum value (inclusive) for this [Float] or null if not needed
         * @param max The maximum value (inclusive) for this [Float] or null if not needed
         * @param error The error to send if this [Float] is not between [min] and [max]
         */
        @JvmStatic @JvmOverloads
        fun float(
            min: Float? = null,
            max: Float? = null,
            error: String? = null
        ): Adapter<Float> = when {
            min == null && max != null -> float().max<Float>(max, error)
            min != null && max == null -> float().min<Float>(min, error)
            min != null && max != null -> float().between<Float>(min, max, error)
            else -> ofSingleArg(String::toFloat)
        }

        /**
         * Creates an [Adapter] that turns a [String] into a [Double]
         * @param min The minimum value (inclusive) for this [Double] or null if not needed
         * @param max The maximum value (inclusive) for this [Double] or null if not needed
         * @param error The error to send if this [Double] is not between [min] and [max]
         */
        @JvmStatic @JvmOverloads
        fun double(
            min: Double? = null,
            max: Double? = null,
            error: String? = null
        ): Adapter<Double> = when {
            min == null && max != null -> double().max<Double>(max, error)
            min != null && max == null -> double().min<Double>(min, error)
            min != null && max != null -> double().between<Double>(min, max, error)
            else -> ofSingleArg(String::toDouble)
        }

        /**
         * Creates an [Adapter] that returns a Member for a given [Guild]
         * If multiple Members are matched, [InvalidSyntaxException] is thrown
         *
         * @param guild The [Guild] to search the Member in
         */
        @JvmStatic
        fun member(guild: Guild) = ofSingleArg {
            val target = it.replace("<@!?(\\d+)>".toRegex(), "$1")
            val byId = runCatching { guild.retrieveMemberById(target).complete() }.getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byTag = target.runCatching(guild::getMemberByTag).getOrNull()
            if (byTag != null) return@ofSingleArg byTag
            val byName = guild.getMembersByEffectiveName(target, true)
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '$target' matches multiple members!")
            return@ofSingleArg byName[0]
        }

        /**
         * Creates an [Adapter] that returns a User for a given [JDA]
         * If multiple Users are matched, [InvalidSyntaxException] is thrown
         *
         * @param jda The [JDA] instance to search the User in
         */
        @JvmStatic fun user(jda: JDA) = ofSingleArg {
            val target = it.replace("<@!?(\\d+)>".toRegex(), "$1")
            val byId = runCatching { jda.retrieveUserById(target).complete() }.getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byTag = target.runCatching(jda::getUserByTag).getOrNull()
            if (byTag != null) return@ofSingleArg byTag
            val byName = jda.getUsersByName(target, true)
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '$target' matches multiple users!")
            return@ofSingleArg byName[0]
        }

        /**
         * Creates an [Adapter] that returns a Role for a given [Guild]
         * If multiple Roles are matched, [InvalidSyntaxException] is thrown
         *
         * @param guild The [Guild] instance to search the Role in
         */
        @JvmStatic
        fun role(guild: Guild) = ofSingleArg {
            val target = it.replace("<@&(\\d+)>".toRegex(), "$1").toLowerCase()
            if (target == "everyone") return@ofSingleArg guild.publicRole
            val byId = target.runCatching(guild::getRoleById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = guild.getRolesByName(target, true)
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '$target' matches multiple roles!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic
        fun textChannel(guild: Guild) = ofSingleArg {
            val target = it.replace("<#(\\d+)>".toRegex(), "$1")
            val byId = target.runCatching(guild::getTextChannelById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = guild.getTextChannelsByName(target, true)
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '${target}' matches multiple channels!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic
        fun category(guild: Guild) = ofSingleArg { target ->
            val byId = target.runCatching(guild::getCategoryById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = guild.getCategoriesByName(target, true)
            if (byName.isEmpty()) return@ofSingleArg null
            if (byName.size != 1) throw InvalidSyntaxException("Argument '$target' matches multiple categories!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic @JvmOverloads
        fun duration(
            min: String? = null,
            max: String? = null,
            error: String? = null
        ): Adapter<Duration> {
            val lower = min?.let(DurationUtils::parseDuration)
            val upper = max?.let(DurationUtils::parseDuration)
            return when {
                lower == null && upper != null -> duration().max(upper, error, Duration::getSeconds)
                lower != null && upper == null -> duration().min(lower, error, Duration::getSeconds)
                lower != null && upper != null -> duration().between(lower, upper, error, Duration::getSeconds)
                else -> ofSingleArg { it.runCatching(DurationUtils::parseDuration).getOrNull() }
            }
        }
    }

    private fun <N> between(
        min: T, max: T, error: String? = null, mapper: (T) -> N = { it as N }
    ): Adapter<T> where N : Comparable<N>, N : Number = Adapter {
        val read = this(it) ?: return@Adapter null
        val check = mapper(read)
        if (check < mapper(min) || check > mapper(max)) throw InvalidSyntaxException(
            error ?: "Expected `$min` <= value <= `$max`, actual: `$check`!"
        )
        return@Adapter read
    }

    fun <N> min(
        min: T, error: String? = null, mapper: (T) -> N = { it as N }
    ): Adapter<T> where N : Comparable<N>, N : Number = Adapter {
        val read = this(it) ?: return@Adapter null
        val check = mapper(read)
        if (check < mapper(min)) throw InvalidSyntaxException(
            error ?: "Expected value >= `$min`, actual: `$check`!"
        )
        else return@Adapter read
    }

    fun <N> max(
        max: T, error: String? = null, mapper: (T) -> N = { it as N }
    ): Adapter<T> where N : Comparable<N>, N : Number = Adapter {
        val read = this(it) ?: return@Adapter null
        val check = mapper(read)
        if (check > mapper(max)) throw InvalidSyntaxException(
            error ?: "Expected value <= `$max`, actual: `$read`!"
        )
        else return@Adapter read
    }
}

infix fun <V, T : V, U : V> Adapter<T>.or(other: Adapter<U>) = Adapter {
    this(it) ?: other(it)
}