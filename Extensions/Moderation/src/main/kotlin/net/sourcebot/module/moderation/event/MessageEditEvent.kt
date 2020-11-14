package net.sourcebot.module.moderation.event

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.event.SourceEvent

data class MessageEditEvent(
    val guild: Guild,
    val author: Member,
    val channel: TextChannel,
    val newContent: String,
    val oldContent: String?,
    val message: Message
) : SourceEvent