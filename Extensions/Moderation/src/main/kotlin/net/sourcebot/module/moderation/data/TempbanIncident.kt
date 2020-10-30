package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardErrorResponse
import org.bson.Document
import java.time.Duration

class TempbanIncident(
    override val id: Long,
    private val sender: Member,
    val tempbanned: Member,
    private val delDays: Int,
    duration: Duration,
    override val reason: String,
    private val points: Double = 65.0
) : SimpleIncident(duration) {
    override val source = sender.id
    override val target = tempbanned.id
    override val type = Incident.Type.TEMPBAN
    private val tempban = StandardErrorResponse(
        "Tempban - Case #$id",
        """
            **Tempbanned By:** ${sender.formatted()} ($source)
            **Tempbanned User:** ${tempbanned.formatted()} ($target)
            **Duration:** ${DurationUtils.formatDuration(duration)}
            **Reason:** $reason
        """.trimIndent()
    )

    override fun asDocument() = super.asDocument().also {
        it["points"] = Document().also {
            it["value"] = points
            it["decay"] = (86400L * points).toLong()
        }
    }

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