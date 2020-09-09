package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.configuration.GuildConfigurationManager
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse

class ConfigurationCommand(
    private val configurationManager: GuildConfigurationManager
) : RootCommand() {
    override val name = "configuration"
    override val description = "Modify the Guild Configuration."
    override val permission = "configuration"
    override val guildOnly = true
    override val aliases = arrayOf("config", "configure", "cfg")

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
            config[path] = value
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
        "get", "Get a configuration value."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("path", "The path of the value to get.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val path = args.next("You did not specify a configuration path!")
            val config = configurationManager[message.guild]
            val found = config.optional<Any>(path)
            return InfoResponse(
                "Configuration Query",
                "`$path` : `$found`"
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
    override val name: String,
    override val description: String
) : Command() {
    override val permission by lazy { "configuration.$name" }
    override val guildOnly = true
}