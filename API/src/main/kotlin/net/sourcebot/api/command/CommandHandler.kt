package net.sourcebot.api.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.sourcebot.api.command.PermissionCheck.Type.*
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.event.AbstractMessageHandler
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.error.ExceptionResponse
import net.sourcebot.api.response.error.GlobalAdminOnlyResponse
import net.sourcebot.api.response.error.GuildOnlyCommandResponse
import java.util.concurrent.TimeUnit

class CommandHandler(
    private val defaultPrefix: String,
    private val deleteSeconds: Long,
    private val configManager: ConfigurationManager,
    private val permissionHandler: PermissionHandler
) : AbstractMessageHandler() {
    private var commandMap = CommandMap<RootCommand>()

    override fun cascade(
        message: Message, label: String, arguments: Arguments
    ) {
        if (message.author.isBot) return
        val rootCommand = commandMap[label] ?: return
        if (!rootCommand.module.enabled) return
        val permissionCheck = checkPermissions(message, rootCommand, arguments)
        val command = permissionCheck.command
        when (permissionCheck.type) {
            GLOBAL_ONLY -> respond(permissionCheck.command, message, GlobalAdminOnlyResponse())
            GUILD_ONLY -> respond(permissionCheck.command, message, GuildOnlyCommandResponse())
            NO_PERMISSION -> respond(
                command, message, permissionHandler.getPermissionAlert(
                    command.guildOnly, message.jda,
                    permissionHandler.getData(message.guild).getUser(message.member!!),
                    command.permission!!
                )
            )
            VALID -> {
                try {
                    val response = command.execute(message, arguments)
                    if (response !is EmptyResponse) return respond(command, message, response)
                } catch (exception: Exception) {
                    val error = if (exception is InvalidSyntaxException) {
                        ErrorResponse(
                            "Invalid Syntax!",
                            """
                                ${exception.message!!}
                                **Syntax**: ${
                                when {
                                    message.isFromGuild -> getSyntax(message.guild, command)
                                    else -> getSyntax(command)
                                }
                            }
                            """.trimIndent()
                        )
                    } else {
                        exception.printStackTrace()
                        ExceptionResponse(exception)
                    }
                    return respond(command, message, error)
                }
            }
        }
    }

    override fun getViablePrefixes(
        event: MessageReceivedEvent
    ) = mutableListOf(
        "<@!${event.jda.selfUser.id}> ",
        "<@${event.jda.selfUser.id}> "
    ) + if (event.isFromGuild) getPrefix(event.guild) else defaultPrefix

    fun checkPermissions(
        message: Message,
        root: RootCommand,
        arguments: Arguments = Arguments(emptyArray())
    ): PermissionCheck {
        val hasGlobal = permissionHandler.hasGlobalAccess(message.author)
        val inGuild = message.channelType == ChannelType.TEXT
        var command: Command = root
        do {
            if (!hasGlobal) {
                if (command.requiresGlobal) return PermissionCheck(command, GLOBAL_ONLY)
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
                            if (!permissionHandler.hasPermission(
                                    sourceUser, permission, channel
                                )
                            ) return PermissionCheck(command, NO_PERMISSION)
                        }
                    } else if (command.guildOnly) return PermissionCheck(command, GUILD_ONLY)
                }
            }
            val nextId = arguments.next() ?: break
            val nextCommand = command[nextId]
            if (nextCommand == null) {
                arguments.backtrack()
                break
            }
            command = nextCommand
        } while (true)
        return PermissionCheck(command, VALID)
    }

    private fun respond(command: Command, message: Message, response: Response) {
        message.channel.sendMessage(response.asMessage(message.author)).queue {
            command.postResponse(response, message.author, it)
            if (!command.cleanupResponse) return@queue
            if (message.channelType == ChannelType.TEXT) {
                message.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
            }
            it.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS)
        }
    }

    fun getPrefix() = defaultPrefix
    override fun getPrefix(
        guild: Guild
    ) = configManager[guild].required("command-prefix") { defaultPrefix }

    fun getSyntax(guild: Guild, command: Command) = getSyntax(getPrefix(guild), command)
    fun getSyntax(prefix: String, command: Command) = "$prefix${command.getUsage()}".trim()
    fun getSyntax(command: Command) = getSyntax(defaultPrefix, command)

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
        if (!input.startsWith(defaultPrefix)) return null
        val identifier = input.substring(defaultPrefix.length, input.length).split(" ")[0]
        return getCommand(identifier) != null
    }

    fun unregister(module: SourceModule) = commandMap.removeIf { it.module == module }
}

class PermissionCheck(val command: Command, val type: Type) {
    enum class Type {
        GLOBAL_ONLY,
        GUILD_ONLY,
        NO_PERMISSION,
        VALID
    }

    fun isValid() = type == VALID
}