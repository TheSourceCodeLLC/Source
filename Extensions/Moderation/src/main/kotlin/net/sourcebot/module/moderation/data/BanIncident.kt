package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardErrorResponse

class BanIncident(
    override val id: Long,
    private val sender: Member,
    val member: Member,
    private val delDays: Int,
    override val reason: String
) : OneshotPunishment(Level.FOUR) {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.BAN
    private val ban = StandardErrorResponse(
        "Ban - Case #$id",
        """
            **Banned By:** ${sender.formatted()} ($source)
            **Banned User:** ${member.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failure
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(ban.asMessage(member)).complete()
        }
        member.ban(delDays, reason).complete()
    }

    override fun sendLog(logChannel: TextChannel): Message =
        logChannel.sendMessage(ban.asMessage(sender)).complete()
}