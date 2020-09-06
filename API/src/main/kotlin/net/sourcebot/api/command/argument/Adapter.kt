package net.sourcebot.api.command.argument

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.SourceDuration

/**
 * Represents an adapter to convert command [Arguments] to a desired [T]
 */
class Adapter<T>(adapter: (Arguments) -> T?) : (Arguments) -> T? by adapter {
    companion object {
        @JvmStatic
        fun <T> ofSingleArg(adapter: (String) -> T): Adapter<T> = Adapter {
            val arg = it.next() ?: return@Adapter null
            val result = adapter(arg)
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
            val byEffective = target.runCatching { guild.getMembersByEffectiveName(this, true) }.getOrNull()
            if (byEffective?.isNotEmpty() == true) return@ofSingleArg byEffective[0]
            return@ofSingleArg null
        }

        @JvmStatic
        fun role(guild: Guild) = ofSingleArg {
            val target = it.replace("<@&(\\d+)>".toRegex(), "$1")
            val byId = target.runCatching(guild::getRoleById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = target.runCatching { guild.getRolesByName(this, true) }.getOrNull()
            if (byName?.isNotEmpty() == true) return@ofSingleArg byName[0]
            return@ofSingleArg null
        }

        @JvmStatic
        fun channel(guild: Guild) = ofSingleArg {
            val target = it.replace("<#(\\d+)>".toRegex(), "$1")
            val byId = target.runCatching(guild::getTextChannelById).getOrNull()
            if (byId != null) return@ofSingleArg byId
            val byName = target.runCatching { guild.getTextChannelsByName(this, true) }.getOrNull()
            if (byName?.isNotEmpty() == true) return@ofSingleArg byName[0]
            return@ofSingleArg null
        }

        @JvmStatic fun duration() = ofSingleArg {
            it.runCatching(SourceDuration::parse).getOrNull()
        }
    }
}