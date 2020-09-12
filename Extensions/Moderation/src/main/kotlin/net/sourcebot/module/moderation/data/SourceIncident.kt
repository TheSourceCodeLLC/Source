package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.TextChannel

abstract class SourceIncident(
    val type: Type
) {
    private var last: Long = 0
    fun computeId() = ++last

    abstract fun execute(): Throwable?
    abstract fun sendLog(channel: TextChannel): Long

    enum class Type {
        MUTE, KICK, TEMPBAN, BAN, UNMUTE, UNBAN
    }
}