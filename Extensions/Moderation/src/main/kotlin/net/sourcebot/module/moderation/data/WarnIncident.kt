package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatted
import net.sourcebot.api.response.WarningResponse

class WarnIncident(
    override val id: Long,
    private val sender: Member,
    val warned: Member,
    override val reason: String,
) : OneshotIncident() {
    override val source: String = sender.id
    override val target: String = warned.id
    override val type = Incident.Type.WARN

    private val warning = WarningResponse(
        "Warning - Case #$id",
        """
            **Warned By:** ${sender.formatted()} ($source)
            **Warned User:** ${warned.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

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