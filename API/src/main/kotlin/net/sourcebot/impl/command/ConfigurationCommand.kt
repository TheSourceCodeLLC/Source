package net.sourcebot.impl.command

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse

class ConfigurationCommand : RootCommand() {
    override val name = "configuration"
    override val description = "Utilize the Guild Configuration."
    override val permission = name
    override val guildOnly = true
    override val aliases = arrayOf("config", "configure", "cfg")
    override val cleanupResponse = true

    private val configurationManager = Source.CONFIG_MANAGER

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
            val stored = value.runCatching {
                JsonSerial.mapper.readTree(this)
            }.getOrDefault(
                JsonSerial.mapper.readTree("\"$value\"")
            )
            config[path] = stored
            configurationManager.saveData(message.guild, config)
            return StandardSuccessResponse(
                "Configuration Updated",
                """
                    `$path`:
                    ```json
                    ${stored.toPrettyString()}
                    ```
                """.trimIndent()
            )
        }
    }

    private inner class ConfigurationAddCommand : Bootstrap(
        "add", "Append a value to a list within the configuration."
    ) {
        override val aliases = arrayOf("append")
        override val argumentInfo = ArgumentInfo(
            Argument("path", "The path of the list to append to."),
            Argument("value", "The value to append into the list.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val path = args.next("You did not specify a configuration path!")
            val value = args.next("You did not specify a value to append!")
            val config = configurationManager[message.guild]
            val stored = config.required<JsonNode>(path, JsonSerial.Companion::newArray)
            val array = if (stored.isArray) stored as ArrayNode else JsonSerial.newArray().also { it.add(stored) }
            val toStore = value.runCatching {
                JsonSerial.mapper.readTree(this)
            }.getOrDefault(JsonSerial.mapper.readTree("\"$value\""))
            array.add(toStore)
            config[path] = array
            configurationManager.saveData(message.guild, config)
            return StandardSuccessResponse(
                "Configuration Updated",
                """
                    `$path`:
                    ```json
                    ${array.toPrettyString()}
                    ```
                """.trimIndent()
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
            configurationManager.saveData(message.guild, config)
            return StandardSuccessResponse(
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
            val queryPath = args.next()
            val value: JsonNode = queryPath?.let { config.optional(it) } ?: config.json
            val render = if (value.isObject) {
                value.fields().asSequence().joinToString(",\n", "{\n", "\n}") { (name, node) ->
                    "  \"$name\" : " + if (node.isObject) "{ ... }" else node.toPrettyString()
                }
            } else value.toPrettyString()
            return StandardInfoResponse(
                "Configuration Query",
                "```json\n$render\n```"
            )
        }
    }

    init {
        addChildren(
            ConfigurationSetCommand(),
            ConfigurationUnsetCommand(),
            ConfigurationGetCommand(),
            ConfigurationAddCommand()
        )
    }
}

private abstract class Bootstrap(
    final override val name: String,
    final override val description: String
) : Command() {
    final override val permission = "configuration.$name"
    final override val guildOnly = true
    override val cleanupResponse = false
}