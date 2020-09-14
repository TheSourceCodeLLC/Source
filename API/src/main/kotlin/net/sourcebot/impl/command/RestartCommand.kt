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

    private val onQueued = InfoResponse(
        "Restart Scheduled",
        "The bot has been scheduled to restart."
    )
    private val onFailure = ErrorResponse(
        "Restart Failure",
        "There was a problem restarting the bot."
    )

    override fun execute(message: Message, args: Arguments): Response {
        message.channel.sendMessage(onQueued.asMessage(message.author)).complete()
        try {
            Runtime.getRuntime().exec(script)
        } catch (ex: Throwable) {
            return onFailure.addField("Exception:", ex.message, false) as Response
        }
        return EmptyResponse()
    }
}