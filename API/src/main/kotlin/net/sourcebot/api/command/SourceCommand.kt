package net.sourcebot.api.command

import me.hwiggy.kommander.Command
import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.ExtraParameters
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

    override fun getExtraParameters(sender: Message) = HashMap<String, Any>().also {
        if (sender.isFromGuild) it["guild"] = sender.guild
        it["jda"] = sender.jda
    }.let(ExtraParameters::fromMap)
}