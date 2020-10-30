package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response

class UnbanCommand : ModerationRootCommand(
    "unban", "Unban a user for a specific reason."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The id of the user to unmute."),
        Argument("reason", "The reason this user is being unmuted.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next("You did not specify a user ID to unban!")
        val reason = args.slurp(" ", "You did not specify an unban reason!")
        return punishmentHandler.unbanIncident(message.member!!, target, reason)
    }
}