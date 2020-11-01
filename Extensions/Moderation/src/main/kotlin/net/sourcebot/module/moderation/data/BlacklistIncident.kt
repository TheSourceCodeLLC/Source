package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardWarningResponse
import org.bson.Document
import java.time.Duration

class BlacklistIncident(
    override val id: Long,
    private val blacklistRole: Role,
    private val sender: Member,
    val member: Member,
    val duration: Duration,
    override val reason: String,
    private val points: Double
) : SimpleIncident(duration) {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.MUTE
    private val blacklist = StandardWarningResponse(
        "Blacklist - Case #$id",
        """
            **Blacklisted By:** ${sender.formatted()} ($source)
            **Blacklisted User:** ${member.formatted()} ($target)
            **Duration:** ${DurationUtils.formatDuration(duration)}
            **Reason:** $reason
        """.trimIndent()
    )

    override fun asDocument() = super.asDocument().also {
        it["points"] = Document().also {
            it["value"] = points
            it["decay"] = (86400L * points).toLong()
        }
    }

    override fun execute() {
        //Ignore DM failures
        kotlin.runCatching {
            val dm = member.user.openPrivateChannel().complete()
            dm.sendMessage(blacklist.asMessage(member)).complete()
        }
        sender.guild.addRoleToMember(member, blacklistRole).complete()
    }

    override fun sendLog(logChannel: TextChannel) =
        logChannel.sendMessage(blacklist.asMessage(sender)).queue()
}