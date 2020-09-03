package net.sourcebot.module.trivia.data

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.sourcebot.api.event.EventSystem
import net.sourcebot.module.trivia.Trivia

val validEmotes = arrayOf(
    "\uD83C\uDDE6",
    "\uD83C\uDDE7",
    "\uD83C\uDDE8",
    "\uD83C\uDDE9"
)

class TriviaListener(trivia: Trivia, events: EventSystem<GenericEvent>) {
    private val gameMap = HashMap<String, HashMap<String, Int>>()
    private val messageCache = HashMap<String, Message>()

    fun link(
        messageId: String,
        answerMap: HashMap<String, Int>
    ) {
        gameMap[messageId] = answerMap
    }

    fun unlink(messageId: String) {
        gameMap.remove(messageId)
        messageCache.remove(messageId)
    }

    private fun onReaction(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return
        val answerMap = gameMap[event.messageId] ?: return
        val message = messageCache.computeIfAbsent(event.messageId) {
            event.retrieveMessage().complete()
        }
        val reaction = event.reactionEmote
        if (reaction.isEmote) {
            message.removeReaction(reaction.emote, event.user).complete()
            return
        }
        val unicode = reaction.name
        if (unicode !in validEmotes) {
            message.removeReaction(unicode, event.user).complete()
            return
        }
        val answer = validEmotes.indexOf(unicode)
        answerMap[event.userId] = answer
        message.removeReaction(unicode, event.user).queue()
    }

    init {
        events.listen(trivia, this::onReaction)
    }
}