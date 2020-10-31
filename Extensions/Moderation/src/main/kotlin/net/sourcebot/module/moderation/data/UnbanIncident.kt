package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardSuccessResponse

class UnbanIncident(
    override val id: Long,
    private val sender: Member,
    val user: User,
    override val reason: String,
) : OneshotIncident() {
    override val source = sender.id
    override val target = user.id
    override val type = Incident.Type.UNBAN

    private val unban = StandardSuccessResponse(
        "Unban - Case #$id",
        """
            **Unbanned By:** ${sender.formatted()} ($source)
            **Unbanned User:** ${user.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = user.openPrivateChannel().complete()
            dm.sendMessage(unban.asMessage(user)).complete()
        }
        sender.guild.unban(user).complete()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        unban.asMessage(sender)
    ).queue()
}