package net.sourcebot.api.command

import me.hwiggy.kommander.Command
import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.response.Response

/**
 * Represents a command to be executed via tha [CommandHandler]
 */
abstract class SourceCommand : Command<Message, Response, SourceCommand>() {
    open val cleanupResponse = true
    open val deleteSeconds: Long? = null

    open val requiresGlobal = false
    open val permission: String? = null
    open val guildOnly = false

    fun getChildren() = children.getIdentifiers()

    override fun execute(
        sender: Message,
        arguments: Arguments.Processed
    ): Response = throw InvalidSyntaxException("Invalid Subcommand!")

    open fun postResponse(response: Response, forWhom: User, message: Message) = Unit

    protected fun addChildren(vararg command: SourceCommand) = command.forEach(::addChild)
}