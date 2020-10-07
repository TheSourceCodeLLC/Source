package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response

class ReportCommand : ModerationCommand(
    "report", "Report another member."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to report."),
        Argument("reason", "Why you are reporting this member.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to report!")
        val reason = args.slurp(" ", "You did not specify a report reason!")
        return punishmentHandler.submitReport(message.member!!, target, reason)
    }
}