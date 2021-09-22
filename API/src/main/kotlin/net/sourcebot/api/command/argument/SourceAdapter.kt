package net.sourcebot.api.command.argument

import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.BoundAdapter
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.sourcebot.api.DurationUtils
import java.time.Duration

/**
 * Represents an adapter to convert command [Arguments] to a desired [T]
 */
@Suppress("UNCHECKED_CAST", "UNUSED")
class SourceAdapter<T>(adapter: (Arguments) -> T?) : Adapter<T>(adapter) {
    companion object {
        /**
         * Creates an [SourceAdapter] that returns a Member for a given [Guild]
         * If multiple Members are matched, [InvalidSyntaxException] is thrown
         *
         */
        @JvmStatic
        fun member(guild: Guild, arg: String): Member? {
            val target = arg.replace("<@!?(\\d+)>".toRegex(), "$1")
            return runCatching { guild.retrieveMemberById(target).complete() }.getOrElse {
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
         * Creates an [SourceAdapter] that returns a User for a given [JDA]
         * If multiple Users are matched, [InvalidSyntaxException] is thrown
         *
         */
        @JvmStatic
        fun user(jda: JDA, arg: String): User? {
            val target = arg.replace("<@!?(\\d+)>".toRegex(), "$1")
            return runCatching { jda.retrieveUserById(target).complete() }.getOrElse {
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
         * Creates an [SourceAdapter] that returns a Role for a given [Guild]
         * If multiple Roles are matched, [InvalidSyntaxException] is thrown
         */
        @JvmStatic
        fun role(guild: Guild, arg: String): Role? {
            val target = arg.replace("<@&(\\d+)>".toRegex(), "$1").toLowerCase()
            return if (target == "everyone") guild.publicRole
            else target.runCatching(guild::getRoleById).getOrElse {
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
        fun textChannel(guild: Guild, arg: String): TextChannel? {
            val target = arg.replace("<#(\\d+)>".toRegex(), "$1")
            return target.runCatching(guild::getTextChannelById).getOrElse {
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
        fun category(guild: Guild, arg: String): Category? {
            return arg.runCatching(guild::getCategoryById).getOrElse {
                guild.getCategoriesByName(arg, true).let {
                    when {
                        it.isEmpty() -> null
                        it.size == 1 -> it[0]
                        else -> throw InvalidSyntaxException("Argument '$arg' matches multiple categories!")
                    }
                }
            }
        }

        @JvmStatic
        fun guildMessageChannel(
            guild: Guild, arg: String
        ) = textChannel(guild, arg) ?: category(guild, arg)

        @JvmStatic fun duration() = single { it.runCatching(DurationUtils::parseDuration).getOrNull() }
        @JvmStatic fun duration(
            min: String? = null,
            max: String? = null,
            error: String? = null
        ): BoundAdapter<Duration> {
            val lower = min?.let(DurationUtils::parseDuration)
            val upper = max?.let(DurationUtils::parseDuration)
            return duration().bound(lower, upper, error, Duration::getSeconds)
        }
    }

    private fun <N> between(
        min: T, max: T, error: String? = null, mapper: (T) -> N
    ): SourceAdapter<T> where N : Comparable<N>, N : Number = SourceAdapter { args ->
        val read = args.runCatching(this).getOrNull() ?: return@SourceAdapter null
        val check = mapper(read)
        if (check < mapper(min) || check > mapper(max)) throw InvalidSyntaxException(
            error ?: "Expected `$min` <= value <= `$max`, actual: `$check`!"
        )
        return@SourceAdapter read
    }

    fun <N> min(
        min: T, error: String? = null, mapper: (T) -> N
    ): SourceAdapter<T> where N : Comparable<N>, N : Number = SourceAdapter {
        val read = it.runCatching(this).getOrNull() ?: return@SourceAdapter null
        val check = mapper(read)
        if (check < mapper(min)) throw InvalidSyntaxException(
            error ?: "Expected value >= `$min`, actual: `$check`!"
        )
        else return@SourceAdapter read
    }

    fun <N> max(
        max: T, error: String? = null, mapper: (T) -> N
    ): SourceAdapter<T> where N : Comparable<N>, N : Number = SourceAdapter {
        val read = it.runCatching(this).getOrNull() ?: return@SourceAdapter null
        val check = mapper(read)
        if (check > mapper(max)) throw InvalidSyntaxException(
            error ?: "Expected value <= `$max`, actual: `$read`!"
        )
        else return@SourceAdapter read
    }
}

infix fun <V, T : V, U : V> SourceAdapter<T>.or(other: SourceAdapter<U>) = SourceAdapter { this(it) ?: other(it) }