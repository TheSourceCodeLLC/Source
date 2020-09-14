package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response

class RestartCommand(
    private val script: String
) : RootCommand() {
    override val name = "restart"
    override val description = "Restart the bot."
    override val requiresGlobal = true

    override fun execute(message: Message, args: Arguments): Response {
        message.channel.sendMessage(
            InfoResponse(
                "Restart Scheduled",
                "The bot has been scheduled to restart."
            ).asMessage(message.author)
        ).complete()
        return try {
            Runtime.getRuntime().exec(script)
            EmptyResponse()
        } catch (ex: Throwable) {
            ErrorResponse(
                "Restart Failure",
                "There was a problem restarting the bot."
            ).addField("Exception:", ex.message, false) as Response
        }
    }
}