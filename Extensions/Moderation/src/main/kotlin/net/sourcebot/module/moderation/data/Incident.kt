package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.TextChannel

interface Incident {
    val type: Type

    fun execute(): Boolean
    fun computeId(): Long
    fun sendLog(channel: TextChannel): Long

    enum class Type {
        MUTE, KICK, TEMPBAN, BAN, UNMUTE, UNBAN
    }
}