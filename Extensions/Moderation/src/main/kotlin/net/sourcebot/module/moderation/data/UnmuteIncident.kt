package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardSuccessResponse

class UnmuteIncident(
    override val id: Long,
    private val muteRole: Role,
    private val sender: Member,
    val member: Member,
    override val reason: String,
) : OneshotIncident() {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.UNMUTE
    private val unmute = StandardSuccessResponse(
        "Unmute - Case #$id",
        """
            **Unmuted By:** ${sender.formatted()} ($source)
            **Unmuted User:** ${member.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(unmute.asMessage(member)).complete()
        }
        sender.guild.removeRoleFromMember(member, muteRole).complete()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        unmute.asMessage(sender)
    ).queue()
}