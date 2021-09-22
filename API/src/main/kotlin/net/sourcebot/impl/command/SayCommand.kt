package net.sourcebot.impl.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response

class SayCommand : RootCommand() {
    override val name = "say"
    override val description = "Send a message to a specified channel as the bot."
    override val guildOnly = true
    override val permission = "say"

    override val synopsis = Synopsis {
        optParam("channel", "The channel to send the message into.", Adapter.single())
        reqParam("message", "The message to send.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val channel = arguments.optional<String, TextChannel>("channel", sender.textChannel) {
            SourceAdapter.textChannel(sender.guild, it)
        }
        val toSend = arguments.required<String>("message", "You did not specify a message to send!")
        channel.sendMessage(toSend).queue()
        return EmptyResponse(true)
    }
}