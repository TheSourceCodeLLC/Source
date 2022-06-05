package net.sourcebot.impl.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.EmbedResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.WrappedEmbedResponse
import net.sourcebot.api.wrapped

class SudoCommand : RootCommand() {
    override val name = "sudo"
    override val description = "Run a command as another Member."
    override val guildOnly = true
    override val requiresGlobal = true

    override val synopsis = Synopsis {
        reqParam("target", "The Member you would like to run the command as.", SourceAdapter.member())
        reqParam("command", "The command you would like the Member to run.", Adapter.single())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<Member>("target", "You did not specify a valid target member!")
        if (target.user.isBot) return StandardErrorResponse(
            "Sudo Failure!", "You may not run commands as bots!"
        )
        val label = arguments.required<String>("command", "You did not specify a command to run!")
        val (_, response) = Source.COMMAND_HANDLER.runCommand(
            ProxiedMessage(target, sender), label, arguments.parent.slice()
        )
        return when (response) {
            is WrappedEmbedResponse -> response
            is EmbedResponse -> response.wrapped(target)
            else -> response
        }
    }

    private class ProxiedMessage(
        private val newAuthor: Member,
        oldMessage: Message
    ) : Message by oldMessage {
        override fun getAuthor() = newAuthor.user
        override fun getMember() = newAuthor
    }
}