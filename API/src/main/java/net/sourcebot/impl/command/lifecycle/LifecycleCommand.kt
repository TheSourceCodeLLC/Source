package net.sourcebot.impl.command.lifecycle

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.EmbedAlert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments

abstract class LifecycleCommand(
    override val name: String,
    override val description: String,
    private val script: String
) : RootCommand() {
    final override val requiresGlobal = true

    final override fun execute(message: Message, args: Arguments): Alert {
        return try {
            Runtime.getRuntime().exec(script)
            onSuccess
        } catch (ex: Throwable) {
            onFailure.addField("Exception:", ex.message, false)
        } as Alert
    }

    abstract val onSuccess: EmbedAlert
    abstract val onFailure: EmbedAlert
}

class RestartCommand(
    script: String
) : LifecycleCommand(
    "restart", "Restarts the bot.", script
) {
    override val onSuccess = SuccessAlert(
        "Restart Success",
        "The bot has been scheduled to restart."
    )
    override val onFailure = ErrorAlert(
        "Restart Failure",
        "There was a problem scheduling the bot to restart."
    )
}

class StopCommand(
    script: String
) : LifecycleCommand(
    "stop", "Stops the bot.", script
) {
    override val onSuccess = SuccessAlert(
        "Stop Success",
        "The bot has been scheduled to stop."
    )
    override val onFailure = ErrorAlert(
        "Stop Failure",
        "There was a problem scheduling the bot to stop."
    )
}

class UpdateCommand(
    script: String
) : LifecycleCommand(
    "update", "Updates the bot.", script
) {
    override val onSuccess = SuccessAlert(
        "Update Success",
        "The bot has been scheduled to update."
    )
    override val onFailure = ErrorAlert(
        "Update Failure",
        "There was a problem scheduling the bot to update."
    )
}