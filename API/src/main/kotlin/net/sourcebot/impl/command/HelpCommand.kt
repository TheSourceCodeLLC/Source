package net.sourcebot.impl.command

import me.hwiggy.kommander.arguments.*
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.PermissionCheck
import net.sourcebot.api.command.PermissionCheck.Type.GUILD_ONLY
import net.sourcebot.api.command.PermissionCheck.Type.VALID
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.error.GlobalAdminOnlyResponse
import net.sourcebot.api.response.error.GuildOnlyCommandResponse

class HelpCommand : RootCommand() {
    override val name = "help"
    override val description = "Shows command / module information."
    override val synopsis = Synopsis {
        optParam(
            "topic", "The command or module to show help for. Empty to show the module listing.",
            Adapter.single()
        )
        optParam(
            "children...", "The sub-command(s) to get help for, in the case that `topic` is a command.",
            Adapter.slurp(" ")
        )
    }
    override val deleteSeconds = 60L
    private val permissionHandler = Source.PERMISSION_HANDLER
    private val commandHandler = Source.COMMAND_HANDLER
    private val moduleHandler = Source.MODULE_HANDLER

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val topic = arguments.optional<String>("topic")
        if (topic == null) {
            val modules = moduleHandler.loader.getExtensions()
            val enabled = modules.filter { it.enabled }
            if (enabled.isEmpty()) return StandardInfoResponse(
                "Module Index",
                "There are currently no modules enabled."
            )
            val sorted = enabled.sortedBy { it.name }.joinToString("\n") {
                "**${it.name}**: ${it.description}"
            }
            return StandardInfoResponse(
                "Module Index",
                """
                    Below are valid module names and descriptions.
                    Module names may be passed into this command for more detail.
                    Command names may be passed into this command for usage information.
                """.trimIndent()
            ).also {
                if (sender.isFromGuild) {
                    val prefix = commandHandler.getPrefix(sender.guild)
                    it.appendDescription("\nThis Guild's prefix is: `$prefix`")
                }
                it.addField("Modules", sorted, false) as Response
            }
        }
        val asCommand = commandHandler.getCommand(topic)
        if (asCommand != null) {
            val permCheck = commandHandler.checkPermissions(sender, asCommand, arguments.parent.slice())
            val command = permCheck.command
            return when (permCheck.type) {
                PermissionCheck.Type.GLOBAL_ONLY -> GlobalAdminOnlyResponse()
                GUILD_ONLY -> GuildOnlyCommandResponse()
                PermissionCheck.Type.NO_PERMISSION -> {
                    val data = permissionHandler.getData(sender.guild)
                    val permissible = data.getUser(sender.member!!)
                    permissionHandler.getPermissionAlert(
                        command.guildOnly, sender.jda, permissible, command.permission!!
                    )
                }
                VALID -> {
                    StandardInfoResponse(
                        "Command Information:",
                        "Arguments surrounded by <> are required, those surrounded by () are optional."
                    ).apply {
                        addField("Description:", command.description, false)
                        addField(
                            "Usage:", when {
                                sender.isFromGuild -> commandHandler.getSyntax(sender.guild, command)
                                else -> commandHandler.getSyntax(command)
                            }, false
                        )
                        addField("Detail:", command.synopsis.buildParameterDetail(
                            {
                                when (it) {
                                    is Group<*> -> renderGroup(it)
                                    else -> renderParameter(it)
                                }
                            },
                            { it.joinToString("\n") }
                        ), false)
                        if (command.aliases.isNotEmpty())
                            addField("Aliases:", command.aliases.joinToString(), false)
                        if (command.permission != null)
                            addField("Permission:", "`${command.permission}`", false)
                        if (command.getChildren().isNotEmpty())
                            addField("Subcommands:", command.getChildren().joinToString(), false)
                    }
                }
            }
        }
        val asModule: SourceModule? = moduleHandler.loader.findExtension(topic)
        if (asModule != null) {
            val response = StandardInfoResponse("${asModule.name} Module Assistance")
            val config = when {
                asModule.configurationInfo != null -> {
                    asModule.configurationInfo!!.resolved.entries.joinToString("\n") { (k, v) ->
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
                commandHandler.checkPermissions(sender, it).type
            }
            val valid = grouped[VALID] ?: emptyList()
            val guildOnly = grouped[GUILD_ONLY] ?: emptyList()
            if (valid.isEmpty() && guildOnly.isEmpty()) return response.apply {
                addField("Commands", "You do not have access to any of this module's commands.", false)
            }
            val prefix =
                if (sender.isFromGuild) commandHandler.getPrefix(sender.guild)
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
        return StandardErrorResponse(
            "Unknown Topic!",
            "There is no command or module named `$topic`!"
        )
    }

    private fun renderParameter(parameter: Parameter<*>) = StringBuilder().apply {
        append("**${parameter.name}**: ${parameter.description}")
        if (parameter.adapter is BoundAdapter<*>) {
            append(" ")
            val adapter = parameter.adapter as BoundAdapter<*>
            if (adapter.min != null && adapter.max != null) {
                append("Min: ${adapter.min}, Max: ${adapter.max}")
            } else {
                if (adapter.min != null) {
                    append("Min: ${adapter.min}")
                }
                if (adapter.max != null) {
                    append("Max: ${adapter.max}")
                }
            }
        }
        if (parameter.default != null) {
            append(" (Default: _${parameter.default}_)")
        }
    }.toString()

    private fun renderGroup(group: Group<*>) = StringBuilder().apply {
        append("**${group.name}**: ${group.description}")
        group.choices.forEach { (option, description) ->
            append("\n\t**${option.synopsisName}**: $description")
        }
        if (group.default != null) {
            append("\n(Default: _${group.default!!.synopsisName}_)")
        }
    }.toString()
}