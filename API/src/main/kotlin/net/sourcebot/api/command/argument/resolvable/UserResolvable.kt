package net.sourcebot.api.command.argument.resolvable

import me.hwiggy.kommander.InvalidSyntaxException
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.RestAction

class UserResolvable(private val jda: JDA, slug: String) {
    private val target: String

    init {
        target = slug.replace("<@!?(\\d+)>".toRegex(), "$1")
    }

    private fun byId() = target.runCatching(jda::retrieveUserById).map(RestAction<User>::complete).getOrNull()
    private fun byTag() = target.runCatching(jda::getUserByTag).getOrNull()
    private fun byName() = jda.getUsersByName(target, true).let {
        when {
            it.isEmpty() -> null
            it.size == 1 -> it[0]
            else -> throw InvalidSyntaxException("Argument '$target' matches multiple users!")
        }
    }

    fun resolve() = byId() ?: byTag() ?: byName()
}