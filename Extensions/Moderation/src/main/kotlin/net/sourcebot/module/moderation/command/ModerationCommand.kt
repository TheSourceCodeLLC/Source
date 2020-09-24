package net.sourcebot.module.moderation.command

import net.sourcebot.api.command.RootCommand
import net.sourcebot.module.moderation.Moderation

abstract class ModerationCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    protected val punishmentHandler = Moderation.punishmentHandler
    final override val permission = "moderation.$name"
    final override val guildOnly = true
}