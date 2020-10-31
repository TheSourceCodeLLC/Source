package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardWarningResponse
import org.bson.Document

class WarnIncident @JvmOverloads constructor(
    override val id: Long,
    private val sender: Member,
    val warned: Member,
    override val reason: String,
    private val points: Double = 3.7
) : OneshotIncident() {
    override val source: String = sender.id
    override val target: String = warned.id
    override val type = Incident.Type.WARN
    private val warning = StandardWarningResponse(
        "Warning - Case #$id",
        """
            **Warned By:** ${sender.formatted()} ($source)
            **Warned User:** ${warned.formatted()} ($target)
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
            val dm = warned.user.openPrivateChannel().complete()
            dm.sendMessage(warning.asMessage(warned.user)).complete()
        }
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        warning.asMessage(sender.user)
    ).queue()
}