package net.sourcebot.api.response.error

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.response.StandardErrorResponse

/**
 * Called when a user has permission to use a command, but not in the current channel.
 */
class InvalidChannelResponse(jda: JDA, contexts: Set<String>) : StandardErrorResponse("Invalid Channel!") {
    init {
        val validChannels = mutableSetOf<TextChannel>()
        contexts.forEach {
            val category = jda.getCategoryById(it)
            if (category != null) validChannels.addAll(category.textChannels)
            val channel = jda.getTextChannelById(it)
            if (channel != null) validChannels.add(channel)
        }
        val channelList = validChannels.joinToString(", ") {
            it.asMention
        }
        setDescription("You can use this command in the following channel(s):\n $channelList")
    }
}