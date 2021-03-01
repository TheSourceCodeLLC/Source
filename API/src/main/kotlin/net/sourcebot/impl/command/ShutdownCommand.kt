package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import kotlin.system.exitProcess

class ShutdownCommand : RootCommand() {
    override val name = "shutdown"
    override val description = "Exit the bot process normally, invoking shutdown hooks."
    override val requiresGlobal = true

    override fun execute(message: Message, args: Arguments): Response {
        exitProcess(0)
    }
}