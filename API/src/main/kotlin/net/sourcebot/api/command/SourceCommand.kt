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
abstract class SourceCommand : Command<Message, Message, Response, SourceCommand>() {
    open val cleanupResponse = true
    open val deleteSeconds: Long? = null

    open val requiresGlobal = false
    open val permission: String? = null
    open val guildOnly = false

    fun getChildren() = children()

    override fun execute(
        sender: Message,
        arguments: Arguments.Processed
    ): Response = throw InvalidSyntaxException("Invalid Subcommand!")

    open fun postResponse(response: Response, forWhom: User, message: Message) = Unit

    override fun getExtra(sender: Message) = ExtraParameters.of(
        "jda" to sender.jda,
        "guild" to if (sender.isFromGuild) sender.guild else null
    )

    override fun postRegister(child: SourceCommand) {
        child.parent = this
    }
}