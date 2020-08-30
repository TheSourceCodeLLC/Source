package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.module.ModuleHandler
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response

class HelpCommand(
    private val moduleHandler: ModuleHandler,
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
        return if (topic != null) {
            val asCommand = commandHandler.getCommand(topic)
            if (asCommand != null) {
                var command: Command = asCommand
                do {
                    val nextArg = args.next() ?: break
                    val nextCommand = asCommand.get(nextArg)
                    if (nextCommand == null) {
                        args.backtrack()
                        break
                    }
                    command = nextCommand
                } while (true)
                object : InfoResponse(
                    "Command Information:",
                    "Arguments surrounded by <> are required, those surrounded by () are optional."
                ) {
                    init {
                        addField("Description:", command.description, false)
                        addField("Usage:", commandHandler.getSyntax(command), false)
                        addField("Detail:", command.argumentInfo.getParameterDetail(), false)
                        val aliases = command.aliases.joinToString(", ") { it }
                        val aliasList = if (aliases.isEmpty()) {
                            "N/A"
                        } else {
                            aliases
                        }
                        addField("Aliases:", aliasList, false)
                    }
                }
            } else {
                val asModule = moduleHandler.findModule(topic)
                if (asModule != null) {
                    object : InfoResponse(
                        "${asModule.name} Module Assistance",
                        "Below are a list of commands provided by this module."
                    ) {
                        init {
                            val commands = commandHandler.getCommands(asModule)
                                .sortedBy(RootCommand::name)
                            val listing = if (commands.isEmpty()) {
                                "This module does not have any commands."
                            } else {
                                commands.joinToString("\n") {
                                    "**${it.name}**: ${it.description}"
                                }
                            }
                            addField("Commands:", listing, false)
                        }
                    }
                } else {
                    ErrorResponse(
                        "Invalid Topic!",
                        "There is no such module or command named `$topic` !"
                    )
                }
            }
        } else {
            object : InfoResponse(
                "Module Listing",
                "Below are valid module names and descriptions.\n" +
                "Module names may be passed into this command for more detail.\n" +
                "Command names may be passed into this command for usage information."
            ) {
                init {
                    val index = moduleHandler.getModules()
                        .sortedBy { it.name }
                        .filter { it.enabled }
                        .joinToString("\n") {
                            "**${it.name}**: ${it.description}"
                        }
                    val listing = if (index.isEmpty()) {
                        "There are currently no modules enabled."
                    } else {
                        index
                    }
                    addField("Modules", listing, false)
                }
            }
        }
    }
}