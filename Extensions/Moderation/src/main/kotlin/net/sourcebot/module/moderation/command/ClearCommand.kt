package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class ClearCommand : ModerationRootCommand(
    "clear", "Clear a number of messages from a channel."
) {
    override val synopsis = Synopsis {
        optParam("channel", "The channel to clear messages from.", SourceAdapter.textChannel())
        reqParam(
            "amount", "The number of messages to clear; 2 or more.", Adapter.int(
                2, error = "Amount to clear may not be less than 2!"
            )
        )
        reqParam("reason", "Why the messages are being cleared.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val channel = arguments.optional("channel", sender.textChannel)
        val amount = arguments.required<Int>("amount", "You did not specify a number of messages to clear!")
        val reason = arguments.required<String>("reason", "You did not specify a reason for clearing the messages!")
        return Moderation.getPunishmentHandler(sender.guild) {
            clearIncident(sender.member!!, channel, amount, reason)
        }
    }
}