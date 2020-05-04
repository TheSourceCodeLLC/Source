package net.sourcebot.api.command

import com.google.gson.JsonObject
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.error.ExceptionAlert
import net.sourcebot.api.alert.error.InvalidSyntaxAlert
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.misc.AbstractMessageHandler
import java.util.concurrent.TimeUnit

class CommandHandler(
    private val commandMap: CommandMap,
    config: JsonObject
) : AbstractMessageHandler(config["prefix"].asString) {
    private val prefix = config["prefix"].asString
    private val deleteSeconds = config["delete-seconds"].asLong

    override fun cascade(
        message: Message, label: String, args: Array<String>
    ) {
        val arguments = Arguments(args)
        val command = commandMap[label]?.cascade(message, arguments) ?: return
        val response = try {
            command.execute(message, arguments)
        } catch (ex: Exception) {
            handleException(command, ex)
        }
        val embed = response.buildFor(message.author)
        message.channel.sendMessage(embed).queue {
            if (!command.cleanupResponse) return@queue
            message.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
            it.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
        }
    }

    private fun handleException(command: Command, exception: Exception) =
        if (exception is InvalidSyntaxException) {
            InvalidSyntaxAlert(
                "${exception.message!!}\n" +
                "**Syntax:** $prefix${command.usage}".trim()
            )
        } else ExceptionAlert(exception)

    fun register(command: RootCommand) = commandMap.register(command)
    fun unregister(command: RootCommand) = commandMap.unregister(command)
}