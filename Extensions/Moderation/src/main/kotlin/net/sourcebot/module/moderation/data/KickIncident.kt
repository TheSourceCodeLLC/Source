package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.StandardWarningResponse

class KickIncident(
    override val id: Long,
    private val sender: Member,
    val member: Member,
    override val reason: String
) : OneshotPunishment(Level.ONE) {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.KICK
    private val kick = StandardWarningResponse(
        "Kick - Case #$id",
        """
            **Kicked By:** ${sender.formatLong()} ($source)
            **Kicked User:** ${member.formatLong()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(kick.asMessage(member)).complete()
        }
        member.kick(reason).queue()
    }

    override fun sendLog(logChannel: TextChannel): Message = logChannel.sendMessage(
        kick.asMessage(sender)
    ).complete()
}