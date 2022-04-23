package net.sourcebot.api.command.argument.resolvable

import me.hwiggy.kommander.InvalidSyntaxException
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member

class MemberResolvable(private val guild: Guild, slug: String) {
    private val target: String

    init {
        target = slug.replace("<@!?(\\d+)>".toRegex(), "$1")
    }

    private fun byId(): Member? = target.runCatching(guild::getMemberById).getOrNull()
    private fun byTag() = target.runCatching(guild::getMemberByTag).getOrNull()
    private fun byEffectiveName() = guild.getMembersByEffectiveName(target, true).let {
        when {
            it.isEmpty() -> null
            it.size == 1 -> it[0]
            else -> throw InvalidSyntaxException("Argument '$target' matches multiple members!")
        }
    }

    fun resolve() = byId() ?: byTag() ?: byEffectiveName()
}