package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class TempbanCommand : ModerationRootCommand(
    "tempban", "Temporarily ban a member for a specific reason."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to tempban."),
        OptionalArgument("delDays", "The number of days (0-7) of messages to delete.", 7),
        Argument("duration", "How long this member should be tempbanned."),
        Argument("reason", "The reason this member is being tempbanned.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to tempban!")
        val delDays = args.next(Adapter.int()) ?: 7
        val duration = args.next(Adapter.duration(), "You did not specify a valid duration to tempban for!")
        if (duration.isZero) throw InvalidSyntaxException("The duration may not be zero seconds!")
        val reason = args.slurp(" ", "You did not specify a tempban reason!")
        return Moderation.getPunishmentHandler(message.guild) {
            tempbanIncident(message.member!!, target, delDays, duration, reason)
        }
    }
}