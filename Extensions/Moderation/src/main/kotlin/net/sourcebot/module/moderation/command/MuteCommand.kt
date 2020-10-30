package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response

class MuteCommand : ModerationRootCommand(
    "mute", "Temporarily mute a member for a specific reason."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to mute."),
        Argument("duration", "How long this member should be muted."),
        Argument("reason", "The reason this member is being muted.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to mute!")
        val duration = args.next(Adapter.duration(), "You did not specify a valid duration to mute for!")
        if (duration.isZero) throw InvalidSyntaxException("The duration may not be zero seconds!")
        val reason = args.slurp(" ", "You did not specify a mute reason!")
        return punishmentHandler.muteIncident(
            message.member!!, target, duration, reason
        )
    }
}