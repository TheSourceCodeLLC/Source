package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.ErrorResponse

class BanIncident(
    override val id: Long,
    private val sender: Member,
    val banned: Member,
    private val delDays: Int,
    override val reason: String,
) : OneshotIncident() {
    override val source = sender.id
    override val target = banned.id
    override val type = Incident.Type.BAN
    private val ban = ErrorResponse(
        "Ban - Case #$id",
        """
            **Banned By:** ${sender.formatted()} ($source)
            **Banned User:** ${banned.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failure
        kotlin.runCatching {
            val dm = banned.user.openPrivateChannel().complete()
            dm.sendMessage(ban.asMessage(banned)).complete()
        }
        banned.ban(delDays, reason).complete()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        ban.asMessage(sender)
    ).queue()
}