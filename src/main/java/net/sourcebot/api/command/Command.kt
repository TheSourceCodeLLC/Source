package net.sourcebot.api.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments

abstract class Command {
    private val children = CommandMap()

    abstract val name: String
    open val aliases = emptyArray<String>()
    abstract val description: String
    open val argumentInfo = ArgumentInfo()
    open var cleanupResponse = true
    open var parent: Command? = null
    val usage: String by lazy {
        var parent = this.parent
        var parentStr = this.name
        while (parent != null) {
            parentStr = "${parent.name} $parentStr"
            parent = parent.parent
        }
        "$parentStr ${argumentInfo.asList()}"
    }

    private fun getChild(identifier: String) = children[identifier]

    fun cascade(message: Message, args: Arguments): Command {
        if (args.hasNext()) {
            val identifier = args.next()?.toLowerCase()!!
            val child = getChild(identifier)
            if (child != null) return child.cascade(
                message,
                args.splice()
            )
            args.reset()
        }
        return this
    }

    open fun execute(message: Message, args: Arguments): Alert {
        throw InvalidSyntaxException("Invalid Subcommand!")
    }

    protected fun addChild(command: Command) {
        children.register(command)
        command.parent = this
    }
}