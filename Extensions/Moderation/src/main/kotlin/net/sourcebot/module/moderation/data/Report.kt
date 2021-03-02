package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.EmbedResponse
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import org.bson.Document
import java.time.Instant

class Report(
    val id: Long,
    val sender: String,
    val target: String,
    val reason: String,
    val channel: String,
    val time: Long = Instant.now().toEpochMilli(),
    val handling: Document? = null,
    val deleted: Boolean = false
) {
    constructor(document: Document) : this(
        document["_id"] as Long,
        document["sender"] as String,
        document["target"] as String,
        document["reason"] as String,
        document["channel"] as String,
        document["time"] as Long,
        document["handling"] as Document?,
        document["deleted"] as Boolean? ?: false
    )

    fun asDocument(): Document = Document().also {
        it["_id"] = id
        it["sender"] = sender
        it["target"] = target
        it["reason"] = reason
        it["channel"] = channel
        it["time"] = time
    }

    fun send(logChannel: TextChannel) {
        val guild = logChannel.guild
        val senderMember = guild.getMemberById(sender)!!
        val targetMember = guild.getMemberById(target)!!
        val header = """
            ${guild.publicRole.asMention}
            A report has been made against **${targetMember.formatLong()}** by **${senderMember.formatLong()}**
        """.trimIndent()
        val embed = render(guild)
        logChannel.sendMessage(header).embed(embed.asEmbed(targetMember.user)).queue {
            it.addReaction("✅").queue()
            it.addReaction("❌").queue()
        }
    }

    fun render(guild: Guild): EmbedResponse {
        val by = runCatching { "${guild.getMemberById(sender)!!.formatLong()} ($sender)" }.getOrDefault(sender)
        val who = runCatching { "${guild.getMemberById(target)!!.formatLong()} ($target)" }.getOrDefault(target)
        val from = runCatching { "${guild.getTextChannelById(channel)!!.name} ($channel)" }.getOrDefault(channel)
        return when {
            deleted -> {
                StandardSuccessResponse(
                    "Report #$id - Deleted", """
                            **Deleted By**: $by
                            **Reason:** $reason
                        """.trimIndent()
                )
            }
            handling != null -> {
                val valid = handling["valid"] as Boolean
                val status = if (valid) "Handled" else "Marked as Invalid"
                val handler = handling["handler"] as String
                val staff =
                    runCatching { "${guild.getMemberById(handler)!!.formatLong()} ($handler)" }.getOrDefault(handler)
                StandardSuccessResponse(
                    "Report #$id - Handled", """
                        **Reported By:** $by
                        **Reported User:** $who
                        **Channel:** $from
                        **Reason:** $reason
                        
                        **$status By:** $staff
                    """.trimIndent()
                )
            }
            else -> StandardErrorResponse(
                "Report #$id", """
                    **Reported By:** $by
                    **Reported User:** $who
                    **Channel:** $from
                    **Reason:** $reason
                """.trimIndent()
            )
        }
    }
}