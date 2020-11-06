package net.sourcebot.module.profiles.command

import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand

abstract class RootCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    override val permission = "profiles.$name"
    override val guildOnly = true
}

abstract class Command(
    final override val name: String,
    final override val description: String
) : Command() {
    override val permission by lazy { "${parent!!.permission!!}.$name" }
    override val guildOnly = true
}