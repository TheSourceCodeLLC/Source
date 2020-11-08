package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class ClearCommand : ModerationRootCommand(
    "clear", "Clear a number of messages from a channel."
) {
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("channel", "The channel to clear messages from.", "Current"),
        Argument("amount", "The number of messages to clear. Must be at least 2."),
        Argument("reason", "Why the messages are being cleared.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val channel = args.next(Adapter.textChannel(message.guild)) ?: message.textChannel
        val amount = args.next(Adapter.int(), "You did not specify a number of messages to clear!")
        if (amount < 2) throw InvalidSyntaxException("Amount to clear may not be less than 2!")
        val reason = args.slurp(" ", "You did not specify a reason for clearing the messages!")
        return Moderation.getPunishmentHandler(message.guild) {
            clearIncident(message.member!!, channel, amount, reason)
        }
    }
}