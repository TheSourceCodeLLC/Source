package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.response.Response

class BanCommand : ModerationRootCommand(
    "ban", "Ban a member for a specific reason."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to ban."),
        OptionalArgument("delDays", "The number of days (0-7) of messages to delete.", 7),
        Argument("reason", "The reason this member is being banned.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to ban!")
        val delDays = args.next(Adapter.int()) ?: 7
        val reason = args.slurp(" ", "You did not specify a ban reason!")
        return punishmentHandler.banIncident(
            message.member!!, target, delDays, reason
        )
    }
}