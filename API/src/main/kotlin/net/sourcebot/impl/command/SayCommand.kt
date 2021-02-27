package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response

class SayCommand : RootCommand() {
    override val name = "say"
    override val description = "Send a message to a specified channel as the bot."
    override val guildOnly = true
    override val permission = "say"
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("channel", "The channel to send the message into", "current"),
        Argument("message", "The message to send")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val channel = args.next(Adapter.textChannel(message.guild)) ?: message.textChannel
        val toSend = args.slurp(" ", "You did not specify a message to send!")
        channel.sendMessage(toSend).queue()
        return EmptyResponse(true)
    }
}