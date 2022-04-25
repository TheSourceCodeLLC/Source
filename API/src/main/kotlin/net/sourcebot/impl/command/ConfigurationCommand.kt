package net.sourcebot.impl.command

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.configuration.optional
import net.sourcebot.api.configuration.required
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse

class ConfigurationCommand : RootCommand() {
    override val name = "configuration"
    override val description = "Utilize the Guild Configuration."
    override val permission = name
    override val guildOnly = true
    override val aliases = listOf("config", "configure", "cfg")
    override val cleanupResponse = true

    private val configurationManager = Source.CONFIG_MANAGER

    private inner class ConfigurationSetCommand : Bootstrap(
        "set", "Set a configuration value."
    ) {
        override val synopsis = Synopsis {
            reqParam("path", "The path of the value to be set.", Adapter.single())
            reqParam("value", "The value to set.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val path = arguments.required<String>("path", "You did not specify a configuration path!")
            val value = arguments.required<String>("value", "You did not specify a value to set!")
            val config = configurationManager[sender.guild]
            val stored = value.runCatching {
                JsonSerial.mapper.readTree(this)
            }.getOrDefault(
                JsonSerial.mapper.readTree("\"$value\"")
            )
            config[path] = stored
            configurationManager.saveData(sender.guild, config)
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
        override val aliases = listOf("append")
        override val synopsis = Synopsis {
            reqParam("path", "The path of the list to append to.", Adapter.single())
            reqParam("value", "The value to append into the list.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val path = arguments.required<String>("path", "You did not specify a configuration path!")
            val value = arguments.required<String>("value", "You did not specify a value to append!")
            val config = configurationManager[sender.guild]
            val stored = config.required<JsonNode>(path, JsonSerial.Companion::newArray)
            val array = if (stored.isArray) stored as ArrayNode else JsonSerial.newArray().also { it.add(stored) }
            val toStore = value.runCatching {
                JsonSerial.mapper.readTree(this)
            }.getOrDefault(JsonSerial.mapper.readTree("\"$value\""))
            array.add(toStore)
            config[path] = array
            configurationManager.saveData(sender.guild, config)
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
        override val synopsis = Synopsis {
            reqParam("path", "The path of the value to unset.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val path = arguments.required<String>("path", "You did not specify a configuration path!")
            val config = configurationManager[sender.guild]
            config[path] = null
            configurationManager.saveData(sender.guild, config)
            return StandardSuccessResponse(
                "Configuration Updated",
                "The value at `$path` has been unset."
            )
        }
    }

    private inner class ConfigurationGetCommand : Bootstrap(
        "get", "Get a value from the configuration."
    ) {
        override val synopsis = Synopsis {
            optParam("path", "The path of the value to get, absent to get the whole configuration.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val config = configurationManager[sender.guild]
            val queryPath = arguments.optional<String>("path")
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
        register(
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
) : SourceCommand() {
    final override val permission = "configuration.$name"
    final override val guildOnly = true
    override val cleanupResponse = false
}