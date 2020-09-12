package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.TextChannel

class TempbanIncident : SourceIncident(Type.TEMPBAN) {
    override fun execute(): Throwable? {
        TODO("Not yet implemented")
    }

    override fun sendLog(channel: TextChannel): Long {
        TODO("Not yet implemented")
    }
}