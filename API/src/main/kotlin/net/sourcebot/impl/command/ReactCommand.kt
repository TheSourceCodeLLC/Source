package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardSuccessResponse

class ReactCommand : RootCommand() {
    override val name = "react"
    override val description = "React to a message's reactions as the bot."
    override val permission = name
    override val guildOnly = true

    override val argumentInfo = ArgumentInfo(
        OptionalArgument("channel", "The channel the message is in.", "current"),
        Argument("message", "The message ID of the message.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val channel = args.next(Adapter.textChannel(message.guild)) ?: message.textChannel
        val messageId = args.next("You did not specify a message ID to react to!")
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