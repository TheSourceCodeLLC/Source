package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.StandardSuccessResponse

class UnblacklistIncident(
    override val id: Long,
    private val blacklistRole: Role,
    private val sender: Member,
    val member: Member,
    override val reason: String,
) : OneshotIncident() {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.UNBLACKLIST
    private val unblacklist = StandardSuccessResponse(
        "Unblacklist - Case #$id",
        """
            **Unblacklisted By:** ${sender.formatLong()} ($source)
            **Unblacklisted User:** ${member.formatLong()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(unblacklist.asMessage(member)).complete()
        }
        sender.guild.removeRoleFromMember(member, blacklistRole).complete()
    }

    override fun sendLog(logChannel: TextChannel): Message = logChannel.sendMessage(
        unblacklist.asMessage(sender)
    ).complete()
}