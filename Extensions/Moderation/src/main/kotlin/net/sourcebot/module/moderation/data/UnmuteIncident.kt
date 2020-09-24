package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.SuccessResponse

class UnmuteIncident(
    override val id: Long,
    private val muteRole: Role,
    private val sender: Member,
    val unmuted: Member,
    override val reason: String,
) : OneshotIncident() {
    override val source = sender.id
    override val target = unmuted.id
    override val type = Incident.Type.UNMUTE
    private val unmute = SuccessResponse(
        "Unmute - Case #$id",
        """
            **Unmuted By:** ${sender.formatted()} ($source)
            **Unmuted User:** ${unmuted.formatted()} ($target)
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = unmuted.user.openPrivateChannel().complete()
            dm.sendMessage(unmute.asMessage(unmuted)).complete()
        }
        sender.guild.removeRoleFromMember(unmuted, muteRole).complete()
    }

    override fun sendLog(logChannel: TextChannel) = logChannel.sendMessage(
        unmute.asMessage(sender)
    ).queue()
}