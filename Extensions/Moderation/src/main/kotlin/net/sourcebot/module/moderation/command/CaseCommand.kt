package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response

class CaseCommand : ModerationCommand(
    "case", "Show information about a specific incident."
) {
    override var cleanupResponse = false
    override val aliases = arrayOf("incident")
    override val argumentInfo = ArgumentInfo(
        Argument("id", "The ID of the case to view.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val id = args.next(Adapter.long(), "You did not specify a valid case ID to view!")
        return punishmentHandler.getCase(message.guild, id)?.render(message.guild) ?: return ErrorResponse(
            "Invalid Case ID!", "There is no case with ID #$id!"
        )
    }
}