package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardWarningResponse
import org.bson.Document

class WarnIncident(
    override val id: Long,
    private val sender: Member,
    val member: Member,
    override val reason: String,
    private val points: Double
) : OneshotIncident() {
    override val source: String = sender.id
    override val target: String = member.id
    override val type = Incident.Type.WARN
    private val warning = StandardWarningResponse(
        "Warning - Case #$id",
        """
            **Warned By:** ${sender.formatted()} ($source)
            **Warned User:** ${member.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun asDocument() = super.asDocument().also { outer ->
        outer["points"] = Document().also {
            it["value"] = points
            it["decay"] = (86400L * points).toLong()
        }
    }

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(warning.asMessage(member.user)).complete()
        }
    }

    override fun sendLog(logChannel: TextChannel): Message = logChannel.sendMessage(
        warning.asMessage(sender.user)
    ).complete()
}