package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.StandardInfoResponse

class InfoCommand : RootCommand() {
    override val name = "info"
    override val description = "Show information about Source."

    override fun execute(
        message: Message,
        args: Arguments
    ) = StandardInfoResponse(
        "Information",
        "Running Source v${module.version} by ${module.author}."
    )
}