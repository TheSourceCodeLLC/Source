package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse

class ReportCommand : ModerationRootCommand(
    "report", "Manage Guild reports."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to report."),
        Argument("reason", "Why you are reporting this member.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to report!")
        if (target.user.isBot) return StandardErrorResponse(
            "Report Failure!", "You may not report bots!"
        )
        if (target.user == message.author) return StandardErrorResponse(
            "Report Failure!", "You may not report yourself!"
        )
        val reason = args.slurp(" ", "You did not specify a report reason!")
        return punishmentHandler.submitReport(message, target, reason)
    }

    private inner class ReportGetCommand : ModerationCommand(
        "get", "Show information about a specific report."
    ) {
        override val cleanupResponse = false
        override val argumentInfo = ArgumentInfo(
            Argument("id", "The ID of the report to view.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val id = args.next(Adapter.long(1), "You did not specify a valid report ID to view!")
            return punishmentHandler.getReport(message.guild, id)?.render(message.guild)
                ?: return StandardErrorResponse(
                    "Unknown Report!", "There is no report with the ID '$id'!"
                )
        }
    }

    private inner class ReportDeleteCommand : ModerationCommand(
        "delete", "Delete a specific report."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("id", "The ID of the report to delete."),
            Argument("reason", "Why this report is being deleted.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val id = args.next(Adapter.long(1), "You did not specify a valid report ID to delete!")
            val reason = args.slurp(" ", "You did not specify a reason for deleting this report!")
            return punishmentHandler.deleteReport(message.guild, id, message.member!!, reason)
        }
    }

    init {
        addChildren(
            ReportGetCommand(),
            ReportDeleteCommand()
        )
    }
}