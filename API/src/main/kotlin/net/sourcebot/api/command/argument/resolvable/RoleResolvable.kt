package net.sourcebot.api.command.argument.resolvable

import me.hwiggy.kommander.InvalidSyntaxException
import net.dv8tion.jda.api.entities.Guild

class RoleResolvable(private val guild: Guild, slug: String) {
    private val target: String

    init {
        target = slug.replace("<@&(\\d+)>".toRegex(), "$1")
    }

    private fun byId() = target.runCatching(guild::getRoleById).getOrNull()
    private fun byName() = guild.getRolesByName(target, true).let {
        when {
            it.isEmpty() -> null
            it.size == 1 -> it[0]
            else -> throw InvalidSyntaxException("Argument '$target' matches multiple roles!")
        }
    }

    fun resolve() = if (target == "everyone") guild.publicRole else byId() ?: byName()
}