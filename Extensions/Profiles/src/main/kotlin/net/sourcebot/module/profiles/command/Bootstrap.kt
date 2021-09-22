package net.sourcebot.module.profiles.command

import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand

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
) : SourceCommand() {
    override val permission by lazy { "${parent!!.permission!!}.$name" }
    override val guildOnly = true
}