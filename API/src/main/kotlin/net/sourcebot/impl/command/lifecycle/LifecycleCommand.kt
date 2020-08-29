package net.sourcebot.impl.command.lifecycle

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.*
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments

abstract class LifecycleCommand(
    override val name: String,
    override val description: String,
    private val script: String
) : RootCommand() {
    final override val requiresGlobal = true

    final override fun execute(message: Message, args: Arguments): Response {
        message.channel.sendMessage(onQueued.asMessage(message.author)).queue()
        try {
            Runtime.getRuntime().exec(script)
        } catch (ex: Throwable) {
            return onFailure.addField("Exception:", ex.message, false) as Response
        }
        return EmptyResponse()
    }

    abstract val onQueued: EmbedResponse
    abstract val onFailure: EmbedResponse

}

class RestartCommand(
    script: String
) : LifecycleCommand(
    "restart", "Restarts the bot.", script
) {
    override val onQueued = InfoResponse(
        "Restart Scheduled",
        "The bot has been scheduled to restart."
    )
    override val onFailure = ErrorResponse(
        "Restart Failure",
        "There was a problem restarting the bot."
    )
}

class StopCommand(
    script: String
) : LifecycleCommand(
    "stop", "Stops the bot.", script
) {
    override val onQueued = InfoResponse(
        "Stop Scheduled",
        "The bot has been scheduled to stop."
    )
    override val onFailure = ErrorResponse(
        "Stop Failure",
        "There was a problem stopping the bot."
    )
}

class UpdateCommand(
    script: String
) : LifecycleCommand(
    "update", "Updates the bot.", script
) {
    override val onQueued = InfoResponse(
        "Update Scheduled",
        "The bot has been scheduled to update."
    )
    override val onFailure = ErrorResponse(
        "Update Failure",
        "There was a problem updating the bot."
    )
}