package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse

class CaseCommand : ModerationRootCommand(
    "case", "Manage Guild incidents."
) {
    override val aliases = arrayOf("incident")

    inner class CaseGetCommand : ModerationCommand(
        "get", "Show information about a specific incident."
    ) {
        override var cleanupResponse = false
        override val argumentInfo = ArgumentInfo(
            Argument("id", "The ID of the case to view.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val id = args.next(Adapter.long(), "You did not specify a valid case ID to view!")
            return punishmentHandler.getCase(message.guild, id)?.render(message.guild) ?: return StandardErrorResponse(
                "Invalid Case ID!", "There is no case with ID #$id!"
            )
        }
    }

    inner class CaseDeleteCommand : ModerationCommand(
        "delete", "Delete a specific incident."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("id", "The ID of the case to delete."),
            Argument("reason", "Why this case is being deleted.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val id = args.next(Adapter.long(), "You did not specify a valid case ID to delete!")
            val reason = args.slurp(" ", "You did not specify a reason for the case deletion!")
            return punishmentHandler.deleteCase(message.member!!, id, reason)
        }
    }

    init {
        addChildren(
            CaseGetCommand(),
            CaseDeleteCommand()
        )
    }
}