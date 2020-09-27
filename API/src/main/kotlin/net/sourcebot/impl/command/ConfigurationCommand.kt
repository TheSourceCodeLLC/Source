package net.sourcebot.impl.command

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse

class ConfigurationCommand(
    private val configurationManager: ConfigurationManager
) : RootCommand() {
    override val name = "configuration"
    override val description = "Utilize the Guild Configuration."
    override val permission = name
    override val guildOnly = true
    override val aliases = arrayOf("config", "configure", "cfg")
    override val cleanupResponse = true

    private inner class ConfigurationSetCommand : Bootstrap(
        "set", "Set a configuration value."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("path", "The path of the value to be set"),
            Argument("value", "The value to set")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val path = args.next("You did not specify a configuration path!")
            val value = args.next("You did not specify a value to set!")
            val config = configurationManager[message.guild]
            value.runCatching {
                JsonSerial.mapper.readTree(this)
            }.getOrDefault(value).let { config[path] = it }
            configurationManager.saveData(message.guild)
            return SuccessResponse(
                "Configuration Updated",
                "`$path` has been set to `$value`"
            )
        }
    }

    private inner class ConfigurationUnsetCommand : Bootstrap(
        "unset", "Unset a configuration value."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("path", "The path of the value to unset.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val path = args.next("You did not specify a configuration path!")
            val config = configurationManager[message.guild]
            config[path] = null
            configurationManager.saveData(message.guild)
            return SuccessResponse(
                "Configuration Updated",
                "The value at `$path` has been unset."
            )
        }
    }

    private inner class ConfigurationGetCommand : Bootstrap(
        "get", "Get a value from the configuration."
    ) {
        override val argumentInfo = ArgumentInfo(
            OptionalArgument("path", "The path of the value to get, absent to get the whole configuration.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val config = configurationManager[message.guild]
            return InfoResponse(
                "Configuration Query",
                args.next()?.let {
                    "```json\n${config.optional<JsonNode>(it)?.toPrettyString()}\n```"
                } ?: "```json\n${config.json.toPrettyString()}\n```"
            )
        }
    }

    init {
        addChildren(
            ConfigurationSetCommand(),
            ConfigurationUnsetCommand(),
            ConfigurationGetCommand()
        )
    }
}

private abstract class Bootstrap(
    final override val name: String,
    final override val description: String
) : Command() {
    final override val permission by lazy { "configuration.$name" }
    final override val guildOnly = true
    override val cleanupResponse = false
}