package net.sourcebot.module.economy.command

import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand

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
) : SourceCommand() {
    final override val permission by lazy { "${parent!!.permission!!}.$name" }
    final override val guildOnly = true
}