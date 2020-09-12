package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.TextChannel

class UnmuteIncident : SourceIncident(Type.UNMUTE) {
    override fun execute(): Throwable? {
        TODO("Not yet implemented")
    }

    override fun sendLog(channel: TextChannel): Long {
        TODO("Not yet implemented")
    }
}