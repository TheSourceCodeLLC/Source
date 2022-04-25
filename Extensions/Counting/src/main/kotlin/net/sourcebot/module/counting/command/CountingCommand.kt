package net.sourcebot.module.counting.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.counting.Counting
import net.sourcebot.module.counting.data.CountingListener

class CountingCommand(countingListener: CountingListener) : RootCommand() {
    override val name = "counting"
    override val description = "Various commands for Counting."
    override val permission = "counting"
    override val guildOnly = true

    init {
        register(
            CountingRulesCommand(),
            CountingSetCommand(countingListener)
        )
    }

    private class CountingRulesCommand : CommandBootstrap(
        "rules", "Show the rules for counting."
    ) {
        override fun execute(
            sender: Message,
            arguments: Arguments.Processed
        ) = StandardInfoResponse(
            "Counting Rules",
            "**1.** Players may not increment multiple times in a row.\n" +
                    "**2.** Players may only send numbers.\n" +
                    "**3.** Players may not intentionally disrupt the streak.\n"
        )
    }

    private class CountingSetCommand(
        private val countingListener: CountingListener
    ) : CommandBootstrap(
        "set", "Manually set the counting number."
    ) {
        override val synopsis = Synopsis {
            reqParam("number", "The counting value to be set.", Adapter.long(1))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val number = arguments.required<Long>("number", "You did not provide a value!")
            countingListener.updateCount(sender.guild, number, sender.author.id)
            val response = StandardInfoResponse(
                "Count Updated!", "The count value has been updated to `$number`!"
            )
            Counting.getCountingChannel(sender.guild)
                ?.sendMessageEmbeds(response.asEmbed(sender.author))
                ?.queue()
            return EmptyResponse()
        }
    }

    private abstract class CommandBootstrap(
        final override val name: String,
        final override val description: String
    ) : SourceCommand() {
        override val permission = "counting.$name"
        override val guildOnly = true
    }
}