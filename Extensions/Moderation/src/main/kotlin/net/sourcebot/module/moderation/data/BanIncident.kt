package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.StandardErrorResponse

class BanIncident(
    override val id: Long,
    private val sender: Member,
    val user: User,
    private val delDays: Int,
    override val reason: String
) : OneshotPunishment(Level.FOUR) {
    override val source = sender.id
    override val target = user.id
    override val type = Incident.Type.BAN
    private val ban = StandardErrorResponse(
        "Ban - Case #$id",
        """
            **Banned By:** ${sender.formatLong()} ($source)
            **Banned User:** ${user.formatLong()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failure
        kotlin.runCatching {
            val dm = user.openPrivateChannel().complete()
            dm.sendMessage(ban.asMessage(user)).complete()
        }
        sender.guild.ban(user, delDays, reason).complete()
    }

    override fun sendLog(logChannel: TextChannel): Message =
        logChannel.sendMessage(ban.asMessage(sender)).complete()
}