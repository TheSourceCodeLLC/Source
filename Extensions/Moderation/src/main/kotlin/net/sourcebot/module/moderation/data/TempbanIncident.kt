package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.ErrorResponse
import java.time.Duration
import java.time.Instant

class TempbanIncident(
    override val id: Long,
    private val sender: Member,
    val tempbanned: Member,
    private val delDays: Int,
    duration: Duration,
    override val reason: String,
) : SimpleIncident(
    Instant.now().plus(duration)
) {
    override val source = sender.id
    override val target = tempbanned.id
    override val type = Incident.Type.TEMPBAN
    private val tempban = ErrorResponse(
        "Tempban - Case #$id",
        """
            **Tempbanned By:** ${sender.formatted()} ($source)
            **Tempbanned User:** ${tempbanned.formatted()} ($target)
            **Duration:** ${DurationUtils.formatDuration(duration)}
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = tempbanned.user.openPrivateChannel().complete()
            dm.sendMessage(tempban.asMessage(tempbanned)).complete()
        }
        tempbanned.ban(delDays, reason).complete()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        tempban.asMessage(sender)
    ).queue()
}