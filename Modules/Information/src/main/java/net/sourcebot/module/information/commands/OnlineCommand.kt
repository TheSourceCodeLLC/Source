package net.sourcebot.module.information.commands

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments

class OnlineCommand : RootCommand() {
    override val name = "online"
    override val description = "Show member demographics"
    override val guildOnly = true

    override fun execute(message: Message, args: Arguments): Alert {
        val guild = message.guild
        val members = guild.members
        val online = members.filterNot {
            it.user.isBot || it.user.isFake
        }.count { it.onlineStatus != OnlineStatus.OFFLINE }
        val total = members.count()
        return InfoAlert(
            "Online Members",
            "There are currently **${online}** members online out of **${total}** total members."
        )
    }
}