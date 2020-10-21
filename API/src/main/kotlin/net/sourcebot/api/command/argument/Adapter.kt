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

        @JvmStatic
        fun boolean() = ofSingleArg(String::toBoolean)
        @JvmStatic
        fun short() = ofSingleArg(String::toShort)
        @JvmStatic
        fun int() = ofSingleArg(String::toInt)
        @JvmStatic
        fun long() = ofSingleArg(String::toLong)
        @JvmStatic
        fun float() = ofSingleArg(String::toFloat)
        @JvmStatic
        fun double() = ofSingleArg(String::toDouble)

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
            if (byName.size != 1) throw InvalidSyntaxException("Argument '${target}' matches multiple members!")
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
            if (byName.size != 1) throw InvalidSyntaxException("Argument '${target}' matches multiple roles!")
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
            if (byName.size != 1) throw InvalidSyntaxException("Argument '${target}' matches multiple categories!")
            return@ofSingleArg byName[0]
        }

        @JvmStatic
        fun duration() = ofSingleArg { it.runCatching(DurationUtils::parseDuration).getOrNull() }
    }
}