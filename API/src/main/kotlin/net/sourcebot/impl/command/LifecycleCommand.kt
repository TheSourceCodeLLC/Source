package net.sourcebot.impl.command

import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.configuration.required
import net.sourcebot.api.response.*

abstract class LifecycleCommand(
    final override val name: String,
    final override val description: String,
    private val script: String
) : RootCommand() {
    final override val requiresGlobal = true

    protected abstract val onScheduled: StandardEmbedResponse
    protected abstract val onFailure: StandardEmbedResponse

    final override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        sender.channel.sendMessage(
            onScheduled.asMessage(sender.author)
        ).complete()
        return try {
            Runtime.getRuntime().exec(script)
            EmptyResponse()
        } catch (ex: Throwable) {
            val builder = EmbedBuilder(onFailure).addField("Exception:", ex.message, false)
            return WrappedEmbedResponse(builder.build())
        }
    }
}

class RestartCommand(script: String) : LifecycleCommand(
    "restart", "Restart the bot.", script
) {
    override val onScheduled = StandardInfoResponse(
        "Restart Scheduled", "The bot has been scheduled to restart."
    )
    override val onFailure = StandardErrorResponse(
        "Restart Failure", "There was a problem restarting the bot."
    )
}

class StopCommand(script: String) : LifecycleCommand(
    "stop", "Stop the bot.", script
) {
    override val onScheduled = StandardInfoResponse(
        "Stop Scheduled", "The bot has been scheduled to stop."
    )
    override val onFailure = StandardErrorResponse(
        "Stop Failure", "There was an error stopping the bot."
    )
}

class UpdateCommand(script: String) : LifecycleCommand(
    "update", "Update the bot.", script
) {
    override val onScheduled = StandardInfoResponse(
        "Update Scheduled", "The bot has been scheduled to update."
    )
    override val onFailure = StandardErrorResponse(
        "Update Failure", "There was an error updating the bot."
    )
}

internal fun lifecycleCommands(
    config: JsonConfiguration
) = arrayOf(
    RestartCommand(config.required("restart-script")),
    UpdateCommand(config.required("update-script")),
    StopCommand(config.required("stop-script"))
)