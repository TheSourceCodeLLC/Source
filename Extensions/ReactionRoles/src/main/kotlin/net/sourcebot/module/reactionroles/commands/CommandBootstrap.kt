package net.sourcebot.module.reactionroles.commands

import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.command.RootCommand

abstract class ReactionRolesRootCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val permission = "reactionroles.$name"
    final override val guildOnly = true
}

abstract class ReactionRolesChildCommand : SourceCommand() {
    final override val permission by lazy { "${parent!!.permission!!}.$name" }
    final override val guildOnly = true
}