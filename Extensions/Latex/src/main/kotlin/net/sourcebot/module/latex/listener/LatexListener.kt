package net.sourcebot.module.latex.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.sourcebot.Source
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.error.ExceptionResponse
import net.sourcebot.module.latex.Latex
import net.sourcebot.module.latex.LatexException
import org.bson.Document

class LatexListener : EventSubscriber<Latex> {
    override fun subscribe(
        module: Latex,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMessageReact)
        jdaEvents.listen(module, this::onMessageSend)
    }

    private fun onMessageReact(event: GuildMessageReactionAddEvent) {
        val messageId = event.messageId
        val filter = Document("result", messageId)
        val database = Latex.Database(event.guild)
        val stored = database.find(filter).first() ?: return
        val authorId = stored["author"] as String
        val reactor = event.member
        val sender = event.guild.getMemberById(authorId)
        val canInteract = if (reactor != sender) {
            Source.PERMISSION_HANDLER
                .getData(event.guild)
                .getUser(reactor)
                .hasPermission("latex.interact-others") == true
        } else true
        if (!canInteract) return
        if (event.reactionEmote.isEmote) return
        if (event.reactionEmote.emoji == Latex.DELETE_REACT) {
            event.retrieveMessage().queue { it.delete().queue() }
            database.deleteOne(filter)
        } else return
    }

    private val validator = Regex("```latex\n(.+)\n?```", RegexOption.DOT_MATCHES_ALL)
    private fun onMessageSend(event: MessageReceivedEvent) {
        val response = try {
            val content = event.message.contentRaw
            if (!validator.matches(content)) return
            val expression = validator.matchEntire(content)!!.groupValues[1]
            val image = Latex.parse(expression)
            Latex.send(event.author, event.channel, image)
            EmptyResponse()
        } catch (ex: LatexException) {
            StandardErrorResponse("Invalid LaTeX!", ex.message)
        } catch (ex: Exception) {
            ExceptionResponse(ex)
        }
        if (response is EmptyResponse) return
        event.channel.sendMessage(
            response.asMessage(event.author)
        ).queue()
    }
}