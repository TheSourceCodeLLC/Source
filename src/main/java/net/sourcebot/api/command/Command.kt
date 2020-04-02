package net.sourcebot.api.command

abstract class Command {
    abstract val name: String
    open val aliases = emptyArray<String>()
    abstract val description: String
    open val parameterDetail = "This command has no parameters."
}