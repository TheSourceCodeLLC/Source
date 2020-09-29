package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.command.PermissionCheck
import net.sourcebot.api.command.PermissionCheck.Type.GUILD_ONLY
import net.sourcebot.api.command.PermissionCheck.Type.VALID
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.module.ModuleHandler
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.error.GlobalAdminOnlyResponse
import net.sourcebot.api.response.error.GuildOnlyCommandResponse

class HelpCommand(
    private val moduleHandler: ModuleHandler,
    private val permissionHandler: PermissionHandler,
    private val commandHandler: CommandHandler
) : RootCommand() {
    override val name = "help"
    override val description = "Shows command / module information."
    override val argumentInfo = ArgumentInfo(
        OptionalArgument(
            "topic",
            "The command or module to show help for. Empty to show the module listing."
        ),
        OptionalArgument(
            "children...",
            "The sub-command(s) to get help for, in the case that `topic` is a command."
        )
    )

    override fun execute(message: Message, args: Arguments): Response {
        val topic = args.next()
        if (topic == null) {
            val modules = moduleHandler.getModules()
            val enabled = modules.filter { it.enabled }
            if (enabled.isEmpty()) return InfoResponse(
                "Module Index",
                "There are currently no modules enabled."
            )
            val sorted = enabled.sortedBy { it.name }.joinToString("\n") {
                "**${it.name}**: ${it.description}"
            }
            return InfoResponse(
                "Module Index",
                """
                    Below are valid module names and descriptions.
                    Module names may be passed into this command for more detail.
                    Command names may be passed into this command for usage information.
                """.trimIndent()
            ).addField("Modules", sorted, false) as Response
        }
        val asCommand = commandHandler.getCommand(topic)
        if (asCommand != null) {
            val permCheck = commandHandler.checkPermissions(message, asCommand, args)
            val command = permCheck.command
            return when (permCheck.type) {
                PermissionCheck.Type.GLOBAL_ONLY -> GlobalAdminOnlyResponse()
                GUILD_ONLY -> GuildOnlyCommandResponse()
                PermissionCheck.Type.NO_PERMISSION -> {
                    val data = permissionHandler.getData(message.guild)
                    val permissible = data.getUser(message.member!!)
                    permissionHandler.getPermissionAlert(
                        command.guildOnly, message.jda, permissible, command.permission!!
                    )
                }
                VALID -> {
                    InfoResponse(
                        "Command Information:",
                        "Arguments surrounded by <> are required, those surrounded by () are optional."
                    ).apply {
                        addField("Description:", command.description, false)
                        addField(
                            "Usage:", when {
                                message.isFromGuild -> commandHandler.getSyntax(message.guild, command)
                                else -> commandHandler.getSyntax(command)
                            }, false
                        )
                        addField("Detail:", command.argumentInfo.getParameterDetail(), false)
                        if (command.aliases.isNotEmpty())
                            addField("Aliases:", command.aliases.joinToString(), false)
                        if (command.permission != null)
                            addField("Permission:", "`${command.permission}`", false)
                    }
                }
            }
        }
        val asModule = moduleHandler.findModule(topic)
        if (asModule != null) {
            val response = InfoResponse("${asModule.name} Module Assistance")
            val config = when {
                asModule.configurationInfo != null -> {
                    asModule.configurationInfo!!.resolved.joinToString("\n") { (k, v) ->
                        "`$k`: $v"
                    }
                }
                else -> null
            } ?: "This module does not have any configuration info."
            response.addField("Configuration", config, false)
            val commands = commandHandler.getCommands(asModule)
            if (commands.isEmpty()) return response.apply {
                addField("Commands", "This module does not have any commands.", false)
            }
            val grouped = commands.groupBy {
                commandHandler.checkPermissions(message, it).type
            }
            val valid = grouped[VALID] ?: emptyList()
            val guildOnly = grouped[GUILD_ONLY] ?: emptyList()
            if (valid.isEmpty() && guildOnly.isEmpty()) return response.apply {
                addField("Commands", "You do not have access to any of this module's commands.", false)
            }
            val prefix =
                if (message.isFromGuild) commandHandler.getPrefix(message.guild)
                else commandHandler.getPrefix()
            if (valid.isNotEmpty()) {
                val listing = valid.sortedBy { it.name }.joinToString("\n") {
                    "**$prefix${it.name}**: ${it.description}"
                }
                response.addField("Usable Commands:", listing, false)
            }
            if (guildOnly.isNotEmpty()) {
                val listing = guildOnly.sortedBy { it.name }.joinToString("\n") {
                    "**$prefix${it.name}**: ${it.description}"
                }
                response.addField("Guild Commands:", listing, false)
            }
            return response
        }
        return ErrorResponse(
            "Unknown Topic!",
            "There is no command or module named `$topic`!"
        )
    }
}