package net.sourcebot.api.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.error.ExceptionAlert
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.event.AbstractMessageHandler
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.permission.PermissionHandler
import java.util.concurrent.TimeUnit

class CommandHandler(
        private val prefix: String,
        private val deleteSeconds: Long,
        private val permissionHandler: PermissionHandler
) : AbstractMessageHandler(prefix) {
    private var commandMap = CommandMap<RootCommand>()

    override fun cascade(
            message: Message, label: String, args: Array<String>
    ) {
        val author = message.author
        if (author.isFake || author.isBot) return
        val rootCommand = commandMap[label] ?: return
        if (!rootCommand.module.enabled) return
        val inGuild = message.channelType == ChannelType.TEXT
        if (rootCommand.guildOnly && !inGuild) {
            return respond(
                    rootCommand,
                    message,
                    GuildOnlyCommandAlert(),
                    true
            )
        }
        val arguments = Arguments(args)
        var command: Command = rootCommand
        do {
            if (command.permission != null) {
                val permission = command.permission!!
                if (inGuild) {
                    val permissionData = permissionHandler.getData(message.guild)
                    val guild = message.guild
                    val member = message.member!!
                    val roles = member.roles.toMutableList().apply {
                        add(guild.publicRole)
                    }
                    if (roles.none { it.hasPermission(Permission.ADMINISTRATOR) }) {
                        val sourceUser = permissionData.getUser(member)
                        val sourceRoles = roles.map(permissionData::getRole).toSet()
                        sourceUser.roles = sourceRoles
                        val channel = message.channel as TextChannel
                        if (!permissionHandler.hasPermission(sourceUser, permission, channel)) {
                            return respond(
                                    command,
                                    message,
                                    permissionHandler.getPermissionAlert(
                                            command.guildOnly,
                                            message.jda,
                                            sourceUser,
                                            permission
                                    ),
                                    true
                            )
                        }
                    }
                } else if (command.guildOnly) {
                    return respond(
                            command,
                            message,
                            GuildOnlyCommandAlert(),
                            true
                    )
                }
            }
            val nextId = arguments.next() ?: break
            val nextCommand = command.getChild(nextId)
            if (nextCommand == null) {
                arguments.backtrack()
                break
            }
            command = nextCommand
        } while (true)
        val response = try {
            command.execute(message, arguments)
        } catch (ex: Exception) {
            handleException(command, ex)
        }
        return respond(command, message, response, command.cleanupResponse)
    }

    private fun respond(command: Command, message: Message, alert: Alert, cleanup: Boolean) {
        message.channel.sendMessage(alert.asMessage(message.author)).queue {
            command.postResponse(it)
            if (!cleanup) return@queue
            if (message.channelType == ChannelType.TEXT) {
                message.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
            }
            it.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
        }
    }

    private fun handleException(command: Command, exception: Exception) =
            if (exception is InvalidSyntaxException) {
                ErrorAlert(
                        "Invalid Syntax!",
                        "${exception.message!!}\n" +
                                "**Syntax:** ${getSyntax(command)}"
                )
            } else ExceptionAlert(exception)

    fun getSyntax(command: Command) = "$prefix${command.usage}".trim()

    fun getCommands(module: SourceModule) =
            commandMap.getCommands().filter { it.module == module }

    fun getCommand(name: String) = commandMap[name.toLowerCase()]

    fun registerCommand(module: SourceModule, command: RootCommand) {
        command.module = module
        commandMap.register(command)
    }

    fun unregister(module: SourceModule) = commandMap.removeIf { it.module == module }

    /**
     * Called when a user uses a command marked as guildOnly outside of a Guild (i.e Direct Message)
     */
    private class GuildOnlyCommandAlert : ErrorAlert(
            "Guild Only Command!", "This command may not be used outside of a guild!"
    )
}