package net.sourcebot.module.trivia.data

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

    fun link(
        messageId: String,
        answerMap: HashMap<String, Int>
    ) {
        gameMap[messageId] = answerMap
    }

    fun unlink(messageId: String) {
        gameMap.remove(messageId)
    }

    private fun onReaction(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return
        val message = event.retrieveMessage().complete()
        val answerMap = gameMap[message.id] ?: return
        val reaction = event.reactionEmote
        if (reaction.isEmote) return message.removeReaction(reaction.emote, event.user).queue()
        val unicode = reaction.name
        if (unicode !in validEmotes) return message.removeReaction(unicode, event.user).queue()
        val answer = validEmotes.indexOf(unicode)
        answerMap[event.userId] = answer
        message.removeReaction(unicode, event.user).queue()
    }

    init {
        events.listen(trivia, this::onReaction)
    }
}