package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.StandardErrorResponse
import java.time.Duration

class TempbanIncident(
    override val id: Long,
    private val sender: Member,
    val member: Member,
    private val delDays: Int,
    val duration: Duration,
    override val reason: String
) : ExpiringPunishment(duration, Level.FOUR) {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.TEMPBAN
    private val tempban = StandardErrorResponse(
        "Tempban - Case #$id",
        """
            **Tempbanned By:** ${sender.formatLong()} ($source)
            **Tempbanned User:** ${member.formatLong()} ($target)
            **Duration:** ${DurationUtils.formatDuration(duration)}
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(tempban.asMessage(member)).complete()
        }
        member.ban(delDays, reason).complete()
    }

    override fun sendLog(logChannel: TextChannel): Message = logChannel.sendMessage(
        tempban.asMessage(sender)
    ).complete()
}