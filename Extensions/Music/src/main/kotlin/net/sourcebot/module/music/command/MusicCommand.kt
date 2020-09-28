package net.sourcebot.module.music.command

import net.sourcebot.api.command.RootCommand

abstract class MusicCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val permission = "music.$name"
    final override val guildOnly = true
}