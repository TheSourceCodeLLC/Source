package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardWarningResponse
import org.bson.Document

class KickIncident(
    override val id: Long,
    private val sender: Member,
    val member: Member,
    override val reason: String,
    private val points: Double
) : OneshotIncident() {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.KICK
    private val kick = StandardWarningResponse(
        "Kick - Case #$id",
        """
            **Kicked By:** ${sender.formatted()} ($source)
            **Kicked User:** ${member.formatted()} ($target)
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
            dm.sendMessage(kick.asMessage(member)).complete()
        }
        member.kick(reason).queue()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        kick.asMessage(sender)
    ).queue()
}