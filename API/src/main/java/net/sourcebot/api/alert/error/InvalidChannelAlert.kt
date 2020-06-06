package net.sourcebot.api.alert.error

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.alert.ErrorAlert

class InvalidChannelAlert(jda: JDA, contexts: Set<String>) : ErrorAlert("Invalid Channel!") {
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
        description = "You can use this command in the following channel(s):\n $channelList"
    }
}