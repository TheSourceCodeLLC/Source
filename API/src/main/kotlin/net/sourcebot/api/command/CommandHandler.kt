package net.sourcebot.api.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.event.AbstractMessageHandler
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.error.ExceptionResponse
import net.sourcebot.api.response.error.GlobalAdminOnlyResponse
import java.util.concurrent.TimeUnit

class CommandHandler(
    val prefix: String,
    private val deleteSeconds: Long,
    private val permissionHandler: PermissionHandler
) : AbstractMessageHandler(prefix) {
    private var commandMap = CommandMap<RootCommand>()

    override fun cascade(
        message: Message, label: String, args: Array<String>
    ) {
        val author = message.author
        if (author.isBot) return
        val rootCommand = commandMap[label] ?: return
        if (!rootCommand.module.enabled) return
        val inGuild = message.channelType == ChannelType.TEXT
        if (rootCommand.guildOnly && !inGuild) return respond(rootCommand, message, GuildOnlyCommandResponse())
        val arguments = Arguments(args)
        var command: Command = rootCommand
        val hasGlobal = permissionHandler.hasGlobalAccess(author)
        do {
            if (!hasGlobal && command.requiresGlobal) return respond(
                command, message, GlobalAdminOnlyResponse()
            )
            if (!hasGlobal && command.permission != null) {
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
                        if (!permissionHandler.hasPermission(sourceUser, permission, channel)) return respond(
                            command, message, permissionHandler.getPermissionAlert(
                                command.guildOnly,
                                message.jda,
                                sourceUser,
                                permission
                            )
                        )
                    }
                } else if (command.guildOnly) return respond(command, message, GuildOnlyCommandResponse())
            }
            val nextId = arguments.next() ?: break
            val nextCommand = command[nextId]
            if (nextCommand == null) {
                arguments.backtrack()
                break
            }
            command = nextCommand
        } while (true)
        val response = try {
            command.execute(message, arguments)
        } catch (exception: Exception) {
            if (exception is InvalidSyntaxException) {
                ErrorResponse(
                    "Invalid Syntax!",
                    "${exception.message!!}\n" +
                        "**Syntax:** ${getSyntax(command)}"
                )
            } else {
                exception.printStackTrace()
                ExceptionResponse(exception)
            }
        }
        if (response !is EmptyResponse) return respond(command, message, response)
    }

    private fun respond(command: Command, message: Message, response: Response) {
        message.channel.sendMessage(response.asMessage(message.author)).queue {
            command.postResponse(response, it)
            if (!command.cleanupResponse) return@queue
            if (message.channelType == ChannelType.TEXT) {
                message.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
            }
            it.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
        }
    }

    fun getSyntax(command: Command) = "$prefix${command.usage}".trim()

    fun getCommands(
        module: SourceModule
    ) = commandMap.getCommands().filter { it.module == module }

    fun getCommand(name: String) = commandMap[name.toLowerCase()]

    fun registerCommands(
        module: SourceModule,
        vararg command: RootCommand
    ) = command.forEach {
        it.module = module
        commandMap.register(it)
    }

    fun isValidCommand(input: String): Boolean? {
        if (!input.startsWith(prefix)) return null
        val identifier = input.substring(prefix.length, input.length).split(" ")[0]
        return getCommand(identifier) != null
    }

    fun unregister(module: SourceModule) = commandMap.removeIf { it.module == module }

    /**
     * Called when a user uses a command marked as guildOnly outside of a Guild (i.e Direct Message)
     */
    private class GuildOnlyCommandResponse : ErrorResponse(
        "Guild Only Command!", "This command may not be used outside of a guild!"
    )
}