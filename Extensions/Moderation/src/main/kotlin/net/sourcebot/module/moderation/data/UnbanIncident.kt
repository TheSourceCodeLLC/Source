package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.SuccessResponse

class UnbanIncident(
    override val id: Long,
    private val sender: Member,
    val unbanned: User,
    override val reason: String,
) : OneshotIncident() {
    override val source = sender.id
    override val target = unbanned.id
    override val type = Incident.Type.UNBAN

    private val unban = SuccessResponse(
        "Unban - Case #$id",
        """
            **Unbanned By:** ${sender.formatted()} ($source)
            **Unbanned User:** ${unbanned.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = unbanned.openPrivateChannel().complete()
            dm.sendMessage(unban.asMessage(unbanned)).complete()
        }
        sender.guild.unban(unbanned).complete()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        unban.asMessage(sender)
    ).queue()
}