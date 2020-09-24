package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response

class UnmuteCommand : ModerationCommand(
    "unmute", "Unmute a member for a specific reason."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to unmute."),
        Argument("reason", "The reason this member is being unmuted.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to unmute!")
        val reason = args.slurp(" ", "You did not specify an unmute reason!")
        return punishmentHandler.unmuteIncident(
            message.guild, message.member!!, target, reason
        )
    }
}