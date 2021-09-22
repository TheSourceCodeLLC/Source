package net.sourcebot.impl.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardSuccessResponse

class ReactCommand : RootCommand() {
    override val name = "react"
    override val description = "React to a message's reactions as the bot."
    override val permission = name
    override val guildOnly = true

    override val synopsis = Synopsis {
        optParam("channel", "The channel the message is in.", Adapter.single())
        reqParam("message", "The message ID of the message.", Adapter.single())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val channel = arguments.optional<String, TextChannel>("channel", sender.textChannel) {
            SourceAdapter.textChannel(sender.guild, it)
        }
        val messageId = arguments.required<String>("message", "You did not specify a message ID to react to!")
        val retrieved = channel.retrieveMessageById(messageId).complete()
        retrieved.reactions.forEach {
            val reaction = it.reactionEmote
            when {
                reaction.isEmoji -> retrieved.addReaction(reaction.emoji)
                reaction.isEmote -> retrieved.addReaction(reaction.emote)
                else -> (throw IllegalStateException("Invalid emote type discovered!"))
            }.queue()
        }
        return StandardSuccessResponse(
            description = "Successfully reacted to all reactions for the given message!"
        )
    }
}