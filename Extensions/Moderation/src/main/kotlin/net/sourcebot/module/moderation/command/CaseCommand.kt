package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.module.moderation.Moderation

class CaseCommand : ModerationRootCommand(
    "case", "Manage Guild incidents."
) {
    override val aliases = listOf("incident")

    inner class CaseGetCommand : ModerationCommand(
        "get", "Show information about a specific incident."
    ) {
        override var cleanupResponse = false
        override val synopsis = Synopsis {
            reqParam(
                "id", "The ID of the case to view.", Adapter.long(
                    1, error = "The case ID must be at least 1!"
                )
            )
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val id = arguments.required<Long>("id", "You did not specify a valid case ID to view!")
            return Moderation.getPunishmentHandler(sender.guild) {
                getCase(id)?.render(sender.guild) ?: StandardErrorResponse(
                    "Invalid Case ID!", "There is no case with ID #$id!"
                )
            }
        }
    }

    inner class CaseDeleteCommand : ModerationCommand(
        "delete", "Delete a specific incident."
    ) {
        override val synopsis = Synopsis {
            reqParam(
                "id", "The ID of the case to delete.", Adapter.long(
                    1, error = "The ID must be at least 1!"
                )
            )
            reqParam("reason", "Why this case is being deleted.", Adapter.slurp(" "))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val id = arguments.required<Long>("id", "You did not specify a valid case ID to delete!")
            val reason = arguments.required<String>("reason", "You did not specify a reason for the case deletion!")
            return Moderation.getPunishmentHandler(sender.guild) { deleteCase(sender.member!!, id, reason) }
        }
    }

    init {
        addChildren(
            CaseGetCommand(),
            CaseDeleteCommand()
        )
    }
}