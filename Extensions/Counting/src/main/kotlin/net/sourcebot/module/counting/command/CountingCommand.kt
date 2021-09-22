package net.sourcebot.module.counting.command

import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.response.StandardInfoResponse

class CountingCommand : RootCommand() {
    override val name = "counting"
    override val description = "Various commands for Counting."
    override val permission = "counting"
    override val guildOnly = true

    init {
        addChildren(
            CountingRulesCommand()
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

    private abstract class CommandBootstrap(
        final override val name: String,
        final override val description: String
    ) : SourceCommand() {
        override val permission = "counting.$name"
        override val guildOnly = true
    }
}