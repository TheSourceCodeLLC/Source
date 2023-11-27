package net.sourcebot.api.command.argument

import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.BoundAdapter
import me.hwiggy.kommander.arguments.or
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.command.argument.resolvable.RoleResolvable
import net.sourcebot.api.command.argument.resolvable.UserResolvable
import java.time.Duration

/**
 * Holder object for additional adapters
 */
@Suppress("UNCHECKED_CAST", "UNUSED")
object SourceAdapter {
    /**
     * Creates an [Adapter] that returns a Member for a [Guild] obtained through extra parameters
     * If multiple Members are matched, [InvalidSyntaxException] is thrown
     */
    @JvmStatic fun member(guild: Guild, arg: String): Member? {
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

    @JvmStatic fun member() = Adapter.single { arg, extra ->
        member(extra["guild"], arg)
    }

    /**
     * Creates an [Adapter] that returns a User for a [JDA] obtained through extra parameters
     * If multiple Users are matched, [InvalidSyntaxException] is thrown
     *
     */
    @JvmStatic fun user() = Adapter.single { arg, extra ->
        UserResolvable(extra["jda"], arg).resolve()
    }

    @JvmStatic fun role(guild: Guild, arg: String): Role? {
        val target = arg.replace("<@&(\\d+)>".toRegex(), "$1").lowercase()
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

    /**
     * Creates an [Adapter] that returns a Role for a [Guild] obtained through extra parameters
     * If multiple Roles are matched, [InvalidSyntaxException] is thrown
     */
    @JvmStatic fun role() = Adapter.single { arg, extra ->
        RoleResolvable(extra["guild"], arg).resolve()
    }

    @JvmStatic fun textChannel() = Adapter.single { arg, extra ->
        val guild: Guild by extra
        textChannel(guild, arg)
    }

    @JvmStatic fun textChannel(guild: Guild, arg: String): TextChannel? {
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

    @JvmStatic fun category() = Adapter.single { arg, extra ->
        val guild: Guild by extra
        category(guild, arg)
    }

    @JvmStatic fun category(guild: Guild, arg: String) = arg.runCatching(guild::getCategoryById).getOrElse {
        guild.getCategoriesByName(arg, true).let {
            when {
                it.isEmpty() -> null
                it.size == 1 -> it[0]
                else -> throw InvalidSyntaxException("Argument '$arg' matches multiple categories!")
            }
        }
    }

    @JvmStatic fun <K, V> dictionary(
        columnSeparator: String,
        rowSeparator: String,
        keyTransform: (String) -> K,
        valTransform: (String) -> V
    ) = Adapter.single<Map<K, V>> { arg ->
        val map = hashMapOf<K, V>()
        arg.split(rowSeparator).forEach {
            val (k, v) = it.split(columnSeparator)
            map[keyTransform(k)] = valTransform(v)
        }
        return@single map
    }

    @JvmStatic fun dictionary(
        columnSeparator: String,
        rowSeparator: String
    ) = dictionary(columnSeparator, rowSeparator, { it }, { it })

    @JvmStatic fun guildMessageChannel() = textChannel() or category()

    @JvmStatic fun duration() = Adapter.single { it ->
        it.runCatching(DurationUtils::parseDuration).getOrNull()
    }

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