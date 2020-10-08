package net.sourcebot.module.counting.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse

class CountingCommand(
    private val configurationManager: ConfigurationManager
) : RootCommand() {
    override val name = "counting"
    override val description = "Various commands for Counting."
    override val permission = "counting"
    override val guildOnly = true

    init {
        addChildren(
            CountingRulesCommand(),
            CountingRecordCommand()
        )
    }

    private class CountingRulesCommand : CommandBootstrap(
        "rules", "Show the rules for counting."
    ) {
        override fun execute(
            message: Message,
            args: Arguments
        ) = StandardInfoResponse(
            "Counting Rules",
            "**1.** Players may not increment multiple times in a row.\n" +
                    "**2.** Players may only send numbers.\n" +
                    "**3.** Players may not intentionally disrupt the streak.\n"
        )
    }

    private inner class CountingRecordCommand : CommandBootstrap(
        "record", "Show the current counting record."
    ) {
        override fun execute(message: Message, args: Arguments): Response {
            val guildData = configurationManager[message.guild]
            val data: JsonConfiguration = guildData.optional("counting") ?: return StandardErrorResponse(
                "Counting Record Error", "Counting has not been configured for this Guild!"
            )
            val record: Long = data.optional("record") ?: 0
            return StandardInfoResponse("Counting Record", "The current record is: $record")
        }
    }

    private abstract class CommandBootstrap(
        final override val name: String,
        final override val description: String
    ) : Command() {
        override val permission = "counting.$name"
        override val guildOnly = true
    }
}