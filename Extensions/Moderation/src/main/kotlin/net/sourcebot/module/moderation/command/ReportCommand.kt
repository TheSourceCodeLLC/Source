package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.module.moderation.Moderation

class ReportCommand : ModerationRootCommand(
    "report", "Manage Guild reports."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The Member to report.", SourceAdapter.member())
        reqParam("reason", "Why you are reporting this member.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<Member>("target", "You did not specify a valid member to report!")
        if (target.user.isBot) return StandardErrorResponse(
            "Report Failure!", "You may not report bots!"
        )
        if (target.user == sender.author) return StandardErrorResponse(
            "Report Failure!", "You may not report yourself!"
        )
        val reason = arguments.required<String>("reason", "You did not specify a report reason!")
        return Moderation.getPunishmentHandler(sender.guild) { submitReport(sender, target, reason) }
    }

    private inner class ReportGetCommand : ModerationCommand(
        "get", "Show information about a specific report."
    ) {
        override val cleanupResponse = false
        override val synopsis = Synopsis {
            reqParam(
                "id", "The ID of the report to view.", Adapter.long(
                    1, error = "Report ID must be at least 1!"
                )
            )
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val id = arguments.required<Long>("id", "You did not specify a valid report ID to view!")
            return Moderation.getPunishmentHandler(sender.guild) {
                getReport(id)?.render(sender.guild) ?: StandardErrorResponse(
                    "Unknown Report!", "There is no report with the ID '$id'!"
                )
            }
        }
    }

    private inner class ReportDeleteCommand : ModerationCommand(
        "delete", "Delete a specific report."
    ) {
        override val synopsis = Synopsis {
            reqParam(
                "id", "The ID of the report to delete.", Adapter.long(
                    1, error = "Report ID must be at least 1!"
                )
            )
            reqParam("reason", "Why this report is being deleted.", Adapter.slurp(" "))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val id = arguments.required<Long>("id", "You did not specify a valid report ID to delete!")
            val reason = arguments.required<String>("reason", "You did not specify a reason for deleting this report!")
            return Moderation.getPunishmentHandler(sender.guild) { deleteReport(id, sender.member!!, reason) }
        }
    }

    init {
        register(
            ReportGetCommand(),
            ReportDeleteCommand()
        )
    }
}