package net.sourcebot.module.experience.command

import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand

abstract class ExperienceCommand(
    final override val name: String,
    final override val description: String
) : Command() {
    override val permission by lazy { "${parent!!.permission!!}.$name" }
    override val guildOnly = true
}

abstract class ExperienceRootCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val permission = "experience.$name"
    override val guildOnly = true
}