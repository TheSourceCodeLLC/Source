package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardWarningResponse

class KickIncident(
    override val id: Long,
    private val sender: Member,
    val kicked: Member,
    override val reason: String,
) : OneshotIncident() {
    override val source = sender.id
    override val target = kicked.id
    override val type = Incident.Type.KICK
    private val kick = StandardWarningResponse(
        "Kick - Case #$id",
        """
            **Kicked By:** ${sender.formatted()} ($source)
            **Kicked User:** ${kicked.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = kicked.user.openPrivateChannel().complete()
            dm.sendMessage(kick.asMessage(kicked)).complete()
        }
        kicked.kick(reason).queue()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        kick.asMessage(sender)
    ).queue()
}