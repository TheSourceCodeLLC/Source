package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.SourceDuration

class TempbanIncident(
    private val sender: Member,
    private val target: Member,
    private val duration: SourceDuration,
    private val reason: String
) : SourceIncident(Type.TEMPBAN) {
    override fun execute(): Throwable? {
        TODO("Not yet implemented")
    }

    override fun sendLog(channel: TextChannel): Long {
        TODO("Not yet implemented")
    }
}