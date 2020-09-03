package net.sourcebot.impl.command.lifecycle

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.*

abstract class LifecycleCommand(
    override val name: String,
    override val description: String,
    private val script: String
) : RootCommand() {
    final override val requiresGlobal = true

    final override fun execute(message: Message, args: Arguments): Response {
        message.channel.sendMessage(onQueued.asMessage(message.author)).complete()
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