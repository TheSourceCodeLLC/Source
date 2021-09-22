package net.sourcebot.module.moderation.command

import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand

abstract class ModerationRootCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val permission = "moderation.$name"
    final override val guildOnly = true
}

abstract class ModerationCommand(
    final override val name: String,
    final override val description: String
) : SourceCommand() {
    final override val permission by lazy { "${parent!!.permission!!}.$name" }
    final override val guildOnly = true
}