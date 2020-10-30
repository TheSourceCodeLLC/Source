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

class MuteIncident @JvmOverloads constructor(
    override val id: Long,
    private val muteRole: Role,
    private val sender: Member,
    val muted: Member,
    duration: Duration,
    override val reason: String,
    private val points: Double = 10.0
) : SimpleIncident(duration) {
    override val source = sender.id
    override val target = muted.id
    override val type = Incident.Type.MUTE
    private val mute = StandardWarningResponse(
        "Mute - Case #$id",
        """
            **Muted By:** ${sender.formatted()} ($source)
            **Muted User:** ${muted.formatted()} ($target)
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
            val dm = muted.user.openPrivateChannel().complete()
            dm.sendMessage(mute.asMessage(muted)).complete()
        }
        sender.guild.addRoleToMember(muted, muteRole).complete()
    }

    override fun sendLog(logChannel: TextChannel) =
        logChannel.sendMessage(mute.asMessage(sender)).queue()
}