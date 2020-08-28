package net.sourcebot.impl.command.lifecycle

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.*
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments

abstract class LifecycleCommand(
    override val name: String,
    override val description: String,
    private val script: String
) : RootCommand() {
    final override val requiresGlobal = true

    final override fun execute(message: Message, args: Arguments): Alert {
        message.channel.sendMessage(onQueued.asMessage(message.author)).queue()
        try {
            Runtime.getRuntime().exec(script)
        } catch (ex: Throwable) {
            return onFailure.addField("Exception:", ex.message, false) as Alert
        }
        return EmptyAlert()
    }

    abstract val onQueued: EmbedAlert
    abstract val onFailure: EmbedAlert

}

class RestartCommand(
    script: String
) : LifecycleCommand(
    "restart", "Restarts the bot.", script
) {
    override val onQueued = InfoAlert(
        "Restart Scheduled",
        "The bot has been scheduled to restart."
    )
    override val onFailure = ErrorAlert(
        "Restart Failure",
        "There was a problem restarting the bot."
    )
}

class StopCommand(
    script: String
) : LifecycleCommand(
    "stop", "Stops the bot.", script
) {
    override val onQueued = InfoAlert(
        "Stop Scheduled",
        "The bot has been scheduled to stop."
    )
    override val onFailure = ErrorAlert(
        "Stop Failure",
        "There was a problem stopping the bot."
    )
}

class UpdateCommand(
    script: String
) : LifecycleCommand(
    "update", "Updates the bot.", script
) {
    override val onQueued = InfoAlert(
        "Update Scheduled",
        "The bot has been scheduled to update."
    )
    override val onFailure = ErrorAlert(
        "Update Failure",
        "There was a problem updating the bot."
    )
}