package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response

class WarnCommand : ModerationCommand(
    "warn", "Warn a member for a specific reason."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to warn."),
        Argument("reason", "The reason this member is being warned.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to warn!")
        val reason = args.slurp(" ", "You did not specify a warning reason!")
        return punishmentHandler.warnIncident(
            message.guild, message.member!!, target, reason
        )
    }
}