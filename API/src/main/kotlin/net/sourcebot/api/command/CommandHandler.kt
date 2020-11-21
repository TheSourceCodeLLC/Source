package net.sourcebot.api.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.sourcebot.Source
import net.sourcebot.api.command.PermissionCheck.Type.*
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.event.AbstractMessageHandler
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.error.ExceptionResponse
import net.sourcebot.api.response.error.GlobalAdminOnlyResponse
import net.sourcebot.api.response.error.GuildOnlyCommandResponse
import java.util.concurrent.TimeUnit

class CommandHandler(
    private val defaultPrefix: String,
    private val deleteSeconds: Long
) : AbstractMessageHandler() {
    private val configManager = Source.CONFIG_MANAGER
    private val permissionHandler = Source.PERMISSION_HANDLER
    private var commandMap = CommandMap<RootCommand>()

    override fun cascade(
        message: Message, label: String, arguments: Arguments
    ) {
        val (command, response) = runCommand(message, label, arguments)
        if (command != null && response !is EmptyResponse)
            respond(command, message, response)
    }

    internal fun runCommand(
        message: Message, label: String, arguments: Arguments
    ): Pair<Command?, Response> {
        if (message.author.isBot) return null to EmptyResponse()
        val rootCommand = commandMap[label] ?: return null to EmptyResponse()
        if (!rootCommand.module.enabled) return rootCommand to EmptyResponse()
        val permissionCheck = checkPermissions(message, rootCommand, arguments)
        val command = permissionCheck.command
        return command to when (permissionCheck.type) {
            GLOBAL_ONLY -> GlobalAdminOnlyResponse()
            GUILD_ONLY -> GuildOnlyCommandResponse()
            NO_PERMISSION -> permissionHandler.getPermissionAlert(
                command.guildOnly, message.jda,
                permissionHandler.getData(message.guild).getUser(message.member!!),
                command.permission!!
            )
            VALID -> {
                try {
                    command.execute(message, arguments)
                } catch (exception: Exception) {
                    val error = if (exception is InvalidSyntaxException) {
                        StandardErrorResponse(
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
                    error
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
            val nextId = arguments.next() ?: break
            val nextCommand = command[nextId]
            if (nextCommand == null) {
                arguments.backtrack()
                break
            }
            command = nextCommand
        } while (true)
        if (command.requiresGlobal && !hasGlobal) return PermissionCheck(command, GLOBAL_ONLY)
        if (command.guildOnly && !inGuild) return PermissionCheck(command, GUILD_ONLY)
        if (command.permission != null) {
            val permission = command.permission!!
            if (inGuild && !hasGlobal) {
                val member = message.member!!
                if (member.roles.toMutableList().apply {
                        add(member.guild.publicRole)
                    }.none { it.hasPermission(Permission.ADMINISTRATOR) }) {
                    if (!permissionHandler.memberHasPermission(
                            member, permission, message.channel
                        )
                    ) return PermissionCheck(command, NO_PERMISSION)
                }
            }
        }
        return PermissionCheck(command, VALID)
    }

    fun respond(command: Command, message: Message, response: Response): Response {
        message.channel.sendMessage(response.asMessage(message.author)).queue {
            command.postResponse(response, message.author, it)
            if (!command.cleanupResponse) return@queue
            Source.SCHEDULED_EXECUTOR_SERVICE.schedule({
                //Prevent error logging for failed message deletions
                try {
                    if (message.isFromGuild) {
                        message.delete().complete()
                    }
                    it.delete().complete()
                } catch (err: Throwable) {
                }
            }, command.deleteSeconds ?: deleteSeconds, TimeUnit.SECONDS)
        }
        return response
    }

    fun getPrefix() = defaultPrefix
    override fun getPrefix(
        guild: Guild
    ) = configManager[guild].required("command-prefix") { defaultPrefix }

    private fun getSyntax(prefix: String, command: Command) = "$prefix${command.getUsage()}".trim()
    fun getSyntax(guild: Guild, command: Command) = getSyntax(getPrefix(guild), command)
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