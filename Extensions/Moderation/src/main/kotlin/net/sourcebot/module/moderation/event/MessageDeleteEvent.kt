package net.sourcebot.module.moderation.event

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.event.SourceEvent
import java.time.Instant

data class MessageDeleteEvent(
    val guild: Guild,
    val authorId: String,
    val content: String,
    val channelId: String,
    val sent: Instant
) : SourceEvent {
    val author = guild.getMemberById(authorId)
    val channel = guild.getTextChannelById(channelId)
}