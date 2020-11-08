package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardWarningResponse
import java.time.Duration

class MuteIncident(
    override val id: Long,
    private val muteRole: Role,
    private val sender: Member,
    val member: Member,
    val duration: Duration,
    override val reason: String
) : ExpiringPunishment(duration, Level.TWO) {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.MUTE
    private val mute = StandardWarningResponse(
        "Mute - Case #$id",
        """
            **Muted By:** ${sender.formatted()} ($source)
            **Muted User:** ${member.formatted()} ($target)
            **Duration:** ${DurationUtils.formatDuration(duration)}
            **Reason:** $reason
        """.trimIndent()
    )

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(mute.asMessage(member)).complete()
        }
        sender.guild.addRoleToMember(member, muteRole).complete()
    }

    override fun sendLog(logChannel: TextChannel): Message =
        logChannel.sendMessage(mute.asMessage(sender)).complete()
}