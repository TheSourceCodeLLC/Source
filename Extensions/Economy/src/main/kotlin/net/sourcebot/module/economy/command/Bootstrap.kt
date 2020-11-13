package net.sourcebot.module.economy.command

import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand

abstract class EconomyRootCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val permission = "economy.$name"
    final override val guildOnly = true
}

abstract class EconomyCommand(
    final override val name: String,
    final override val description: String
) : Command() {
    final override val permission by lazy { "${parent!!.permission!!}.$name" }
    final override val guildOnly = true
}