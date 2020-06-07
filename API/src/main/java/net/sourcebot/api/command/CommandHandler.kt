package net.sourcebot.api.command

import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.error.ExceptionAlert
import net.sourcebot.api.alert.error.InvalidChannelAlert
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.event.AbstractMessageHandler
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.permission.SourceUser
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
                message,
                GuildOnlyCommandAlert().buildFor(author),
                true
            )
        }
        val arguments = Arguments(args)
        var command: Command = rootCommand
        do {
            if (command.permission != null) {
                if (inGuild) {
                    val member = message.member!!
                    val user = permissionHandler.getUser(member)
                    val channel = message.channel as TextChannel
                    val parent = channel.parent
                    val contexts = mutableListOf<String>()
                    contexts.add(channel.id)
                    if (parent != null) contexts.add(parent.id)
                    val effectiveNodes = getEffectiveNodes(command.permission!!)
                    if (effectiveNodes.none { hasPermission(user, it, contexts) }) {
                        val availableIn = effectiveNodes.flatMap { user.getContexts(it) }.toSet()
                        return respond(
                            message,
                            (if (availableIn.isEmpty()) {
                                if (command.guildOnly) ErrorAlert(
                                    "No Permission!",
                                    "You do not have permission to use that command!"
                                )
                                else ErrorAlert(
                                    "No Permission!",
                                    "You don't have permission to use that command here, but you do in DMs!"
                                )
                            } else InvalidChannelAlert(message.jda, availableIn)).buildFor(author),
                            true
                        )
                    }
                } else if (command.guildOnly) {
                    return respond(
                        message,
                        GuildOnlyCommandAlert().buildFor(author),
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
        val embed = response.buildFor(message.author)
        return respond(message, embed, command.cleanupResponse)
    }

    private fun respond(message: Message, embed: MessageEmbed, cleanup: Boolean) {
        message.channel.sendMessage(embed).queue {
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

    private fun hasPermission(
        sourceUser: SourceUser,
        node: String,
        context: List<String>
    ) =
        if (context.any { sourceUser.hasPermission(node, it) }) true
        else sourceUser.hasPermission(node)

    private fun getEffectiveNodes(permission: String): Set<String> =
        mutableSetOf(permission).apply {
            addAll(permission.mapIndexed { idx, c ->
                if (c == '.') permission.substring(0..idx) + "*" else null
            }.filterNotNull().toMutableSet()).apply { add("*") }
        }

    /**
     * Called when a user uses a command marked as guildOnly outside of a Guild (i.e Direct Message)
     */
    private class GuildOnlyCommandAlert : ErrorAlert(
        "Guild Only Command!", "This command may not be used outside of a guild!"
    )
}