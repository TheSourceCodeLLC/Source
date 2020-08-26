package net.sourcebot.module.counting.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.data.GuildDataManager
import net.sourcebot.module.counting.data.CountingData

class CountingCommand(
    private val dataManager: GuildDataManager
) : RootCommand() {
    override val name = "counting"
    override val description = "Various commands for Counting."
    override val permission = "counting"
    override val guildOnly = true

    init {
        addChildren(
            CountingRulesCommand(),
            CountingChannelCommand(),
            CountingRecordCommand()
        )
    }

    private class CountingRulesCommand : CommandBootstrap(
        "rules", "Show the rules for counting."
    ) {
        override fun execute(
            message: Message,
            args: Arguments
        ) = InfoAlert(
            "Counting Rules",
            "**1.** Players may not increment multiple times in a row.\n" +
                    "**2.** Players may only send numbers.\n" +
                    "**3.** Players may not intentionally disrupt the streak.\n"
        )
    }

    private inner class CountingRecordCommand : CommandBootstrap(
        "record", "Show the current counting record."
    ) {
        override fun execute(message: Message, args: Arguments): Alert {
            val guildData = dataManager[message.guild]
            val data: CountingData = guildData.optional("counting") ?: return ErrorAlert(
                "Counting Record Error", "Counting has not been configured for this Guild!"
            )
            return InfoAlert("Counting Record", "The current record is: $${data.record}")
        }
    }

    private inner class CountingChannelCommand : CommandBootstrap(
        "channel", "Sets the counting channel to the current channel."
    ) {
        override fun execute(message: Message, args: Arguments): Alert {
            val guildData = dataManager[message.guild]
            val data: CountingData = guildData.optional("counting") ?: CountingData(message.channel.id, 0)
            data.channel = message.channel.id
            dataManager.saveData(message.guild, guildData)
            return SuccessAlert("Counting Channel Updated", "The current channel is now the counting channel!")
        }
    }

    private abstract class CommandBootstrap(
        override val name: String,
        override val description: String
    ) : Command() {
        override val permission by lazy { "counting.$name" }
        override val guildOnly = true
    }
}