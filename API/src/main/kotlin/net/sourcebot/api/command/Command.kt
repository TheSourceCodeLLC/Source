package net.sourcebot.api.command

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response

/**
 * Represents a command to be executed via tha [CommandHandler]
 */
abstract class Command {
    val children = CommandMap<Command>()

    abstract val name: String
    abstract val description: String

    open val aliases = emptyArray<String>()
    open val argumentInfo = ArgumentInfo()

    open val cleanupResponse = true
    open val deleteSeconds: Long? = null
    open var parent: Command? = null

    open val requiresGlobal = false
    open val permission: String? = null
    open val guildOnly = false

    fun getUsage(): String = getUsage(this.name)
    fun getUsage(label: String): String {
        var parent = this.parent
        var parentStr = label
        while (parent != null) {
            parentStr = "${parent.name} $parentStr"
            parent = parent.parent
        }
        val arguments = argumentInfo.asList()
        return ("$parentStr " + if (arguments.isNullOrEmpty()) {
            val subcommands = children.getCommandNames()
            if (subcommands.isEmpty()) "" else subcommands.joinToString("|", "<", ">")
        } else arguments).trimEnd()
    }

    fun getChildren() = children.getCommandNames()

    open fun execute(
        message: Message,
        args: Arguments
    ): Response = throw InvalidSyntaxException("Invalid Subcommand!")

    open fun postResponse(response: Response, forWhom: User, message: Message) = Unit

    protected fun addChildren(vararg command: Command) = command.forEach(::addChild)
    protected fun addChild(
        command: Command
    ) = command.let {
        it.parent = this
        children.register(it)
    }
}