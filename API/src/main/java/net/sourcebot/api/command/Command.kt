package net.sourcebot.api.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments

/**
 * Represents a command to be executed via tha [CommandHandler]
 */
abstract class Command {
    private val children = CommandMap<Command>()

    abstract val name: String
    abstract val description: String

    open val aliases = emptyArray<String>()
    open val argumentInfo: ArgumentInfo by lazy {
        val children = children.getCommandNames()
        if (children.isEmpty()) ArgumentInfo()
        else ArgumentInfo(
            Argument(children.joinToString("|"), "The subcommand you wish to perform")
        )
    }

    open var cleanupResponse = true
    open var parent: Command? = null

    open val permission: String? = null
    open val guildOnly = false

    val usage: String by lazy {
        var parent = this.parent
        var parentStr = this.name
        while (parent != null) {
            parentStr = "${parent.name} $parentStr"
            parent = parent.parent
        }
        "$parentStr ${argumentInfo.asList()}"
    }

    fun getChild(identifier: String) = children[identifier]

    open fun execute(message: Message, args: Arguments): Alert =
        throw InvalidSyntaxException("Invalid Subcommand!")

    protected fun addChild(command: Command): Boolean {
        children.register(command)
        command.parent = this
        return true
    }

    protected fun addChildren(vararg command: Command) = command.all(::addChild)
}