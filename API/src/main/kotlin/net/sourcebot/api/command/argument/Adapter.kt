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
        @JvmStatic
        fun <T> ofSingleArg(adapter: (String) -> T?): Adapter<T> = Adapter { it.next()?.let(adapter) }

        /**
         * Creates an [Adapter] that turns a [String] into a [Boolean]
         */
        @JvmStatic
        fun boolean() = ofSingleArg(String::toBoolean)

        /**
         * Creates an [Adapter] that turns a [String] into a [Byte]
         * @param min The minimum value (inclusive) for this [Byte]
         * @param max The maximum value (inclusive) for this [Byte]
         * @param error The error to send if this [Byte] is not between [min] and [max]
         */
        @JvmStatic
        @JvmOverloads
        fun byte(
            min: Byte = Byte.MIN_VALUE,
            max: Byte = Byte.MAX_VALUE,
            error: String? = null
        ): Adapter<Byte> = ofSingleArg(String::toByte).between(min, max, error) { it }

        /**
         * Creates an [Adapter] that turns a [String] into a [Short]
         * @param min The minimum value (inclusive) for this [Short]
         * @param max The maximum value (inclusive) for this [Short]
         * @param error The error to send if this [Short] is not between [min] and [max]
         */
        @JvmStatic
        @JvmOverloads
        fun short(
            min: Short = Short.MIN_VALUE,
            max: Short = Short.MAX_VALUE,
            error: String? = null
        ): Adapter<Short> = ofSingleArg(String::toShort).between(min, max, error) { it }

        /**
         * Creates an [Adapter] that turns a [String] into a [Int]
         * @param min The minimum value (inclusive) for this [Int]
         * @param max The maximum value (inclusive) for this [Int]
         * @param error The error to send if this [Int] is not between [min] and [max]
         */
        @JvmStatic
        @JvmOverloads
        fun int(
            min: Int = Int.MIN_VALUE,
            max: Int = Int.MAX_VALUE,
            error: String? = null
        ): Adapter<Int> = ofSingleArg(String::toInt).between(min, max, error) { it }

        /**
         * Creates an [Adapter] that turns a [String] into a [Long]
         * @param min The minimum value (inclusive) for this [Long]
         * @param max The maximum value (inclusive) for this [Long]
         * @param error The error to send if this [Long] is not between [min] and [max]
         */
        @JvmStatic
        @JvmOverloads
        fun long(
            min: Long = Long.MIN_VALUE,
            max: Long = Long.MAX_VALUE,
            error: String? = null
        ): Adapter<Long> = ofSingleArg(String::toLong).between(min, max, error) { it }

        /**
         * Creates an [Adapter] that turns a [String] into a [Float]
         * @param min The minimum value (inclusive) for this [Float]
         * @param max The maximum value (inclusive) for this [Float]
         * @param error The error to send if this [Float] is not between [min] and [max]
         */
        @JvmStatic
        @JvmOverloads
        fun float(
            min: Float = Float.MIN_VALUE,
            max: Float = Float.MAX_VALUE,
            error: String? = null
        ): Adapter<Float> = ofSingleArg(String::toFloat).between(min, max, error) { it }

        /**
         * Creates an [Adapter] that turns a [String] into a [Double]
         * @param min The minimum value (inclusive) for this [Double]
         * @param max The maximum value (inclusive) for this [Double]
         * @param error The error to send if this [Double] is not between [min] and [max]
         */
        @JvmStatic
        @JvmOverloads
        fun double(
            min: Double = Double.MIN_VALUE,
            max: Double = Double.MAX_VALUE,
            error: String? = null
        ): Adapter<Double> = ofSingleArg(String::toDouble).between(min, max, error) { it }

        /**
         * Creates an [Adapter] that returns a Member for a given [Guild]
         * If multiple Members are matched, [InvalidSyntaxException] is thrown
         *
         * @param guild The [Guild] to search the Member in
         */
        @JvmStatic
        fun member(guild: Guild) = ofSingleArg {
            val target = it.replace("<@!?(\\d+)>".toRegex(), "$1")
            runCatching { guild.retrieveMemberById(target).complete() }.getOrElse {
                target.runCatching(guild::getMemberByTag).getOrElse {
                    guild.getMembersByEffectiveName(target, true).let {
                        when {
                            it.isEmpty() -> null
                            it.size == 1 -> it[0]
                            else -> throw InvalidSyntaxException("Argument '$target' matches multiple members!")
                        }
                    }
                }
            }
        }

        /**
         * Creates an [Adapter] that returns a User for a given [JDA]
         * If multiple Users are matched, [InvalidSyntaxException] is thrown
         *
         * @param jda The [JDA] instance to search the User in
         */
        @JvmStatic
        fun user(jda: JDA) = ofSingleArg {
            val target = it.replace("<@!?(\\d+)>".toRegex(), "$1")
            runCatching { jda.retrieveUserById(target).complete() }.getOrElse {
                target.runCatching(jda::getUserByTag).getOrElse {
                    jda.getUsersByName(target, true).let {
                        when {
                            it.isEmpty() -> null
                            it.size == 1 -> it[0]
                            else -> throw InvalidSyntaxException("Argument '$target' matches multiple users!")
                        }
                    }
                }
            }
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
            target.runCatching(guild::getRoleById).getOrElse {
                guild.getRolesByName(target, true).let {
                    when {
                        it.isEmpty() -> null
                        it.size == 1 -> it[0]
                        else -> throw InvalidSyntaxException("Argument '$target' matches multiple roles!")
                    }
                }
            }
        }

        @JvmStatic
        fun textChannel(guild: Guild) = ofSingleArg { arg ->
            val target = arg.replace("<#(\\d+)>".toRegex(), "$1")
            target.runCatching(guild::getTextChannelById).getOrElse {
                guild.getTextChannelsByName(target, true).let {
                    when {
                        it.isEmpty() -> null
                        it.size == 1 -> it[0]
                        else -> throw InvalidSyntaxException(
                            "Argument '$target' matches multiple channels!"
                        )
                    }
                }
            }
        }

        @JvmStatic
        fun category(guild: Guild) = ofSingleArg { target ->
            target.runCatching(guild::getCategoryById).getOrElse {
                guild.getCategoriesByName(target, true).let {
                    when {
                        it.isEmpty() -> null
                        it.size == 1 -> it[0]
                        else -> throw InvalidSyntaxException("Argument '$target' matches multiple categories!")
                    }
                }
            }
        }

        @JvmStatic
        @JvmOverloads
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
        min: T, max: T, error: String? = null, mapper: (T) -> N
    ): Adapter<T> where N : Comparable<N>, N : Number = Adapter { args ->
        val read = args.runCatching(this).getOrNull() ?: return@Adapter null
        val check = mapper(read)
        if (check < mapper(min) || check > mapper(max)) throw InvalidSyntaxException(
            error ?: "Expected `$min` <= value <= `$max`, actual: `$check`!"
        )
        return@Adapter read
    }

    fun <N> min(
        min: T, error: String? = null, mapper: (T) -> N
    ): Adapter<T> where N : Comparable<N>, N : Number = Adapter {
        val read = it.runCatching(this).getOrNull() ?: return@Adapter null
        val check = mapper(read)
        if (check < mapper(min)) throw InvalidSyntaxException(
            error ?: "Expected value >= `$min`, actual: `$check`!"
        )
        else return@Adapter read
    }

    fun <N> max(
        max: T, error: String? = null, mapper: (T) -> N
    ): Adapter<T> where N : Comparable<N>, N : Number = Adapter {
        val read = it.runCatching(this).getOrNull() ?: return@Adapter null
        val check = mapper(read)
        if (check > mapper(max)) throw InvalidSyntaxException(
            error ?: "Expected value <= `$max`, actual: `$read`!"
        )
        else return@Adapter read
    }
}

infix fun <V, T : V, U : V> Adapter<T>.or(other: Adapter<U>) = Adapter { this(it) ?: other(it) }