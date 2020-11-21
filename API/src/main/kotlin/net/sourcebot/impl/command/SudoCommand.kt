package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
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

    override val argumentInfo = ArgumentInfo(
        Argument("target", "The Member you would like to run the command as."),
        Argument("command", "The command you would like the member to run.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid target member!")
        if (target.user.isBot) return StandardErrorResponse(
            "Sudo Failure!", "You may not run commands as bots!"
        )
        val label = args.next("You did not specify a command to run!")
        val (_, response) = Source.COMMAND_HANDLER.runCommand(
            ProxiedMessage(target, message), label, args
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