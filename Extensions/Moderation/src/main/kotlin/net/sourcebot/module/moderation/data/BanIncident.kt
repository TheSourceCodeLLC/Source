package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatted
import net.sourcebot.api.response.ErrorResponse

class BanIncident(
    private val sender: Member,
    private val target: Member,
    private val delDays: Int,
    private val reason: String
) : SourceIncident(Type.BAN) {
    override fun execute(): Throwable? =
        try {
            target.ban(delDays, reason).complete(); null
        } catch (ex: Throwable) {
            ex
        }

    override fun sendLog(channel: TextChannel): Long {
        val case = computeId()
        val embed = ErrorResponse(
            "Ban - Case #$case",
            """
                Banned By: ${sender.formatted()} (${sender.id})
                Banned User: ${target.formatted()} (${target.id})
                Reason: $reason
            """.trimIndent()
        ).asEmbed(target.user)
        channel.sendMessage(embed).queue()
        return case
    }
}