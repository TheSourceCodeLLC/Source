package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatted
import net.sourcebot.api.response.StandardErrorResponse
import org.bson.Document
import java.time.Instant

class Report(
    val id: Long,
    val sender: String,
    val target: String,
    val reason: String,
    val time: Long = Instant.now().toEpochMilli()
) {
    constructor(document: Document) : this(
        document["_id"] as Long,
        document["sender"] as String,
        document["target"] as String,
        document["reason"] as String,
        document["time"] as Long
    )

    fun asDocument(): Document = Document().also {
        it["_id"] = id
        it["sender"] = sender
        it["target"] = target
        it["reason"] = reason
        it["time"] = time
    }

    fun send(channel: TextChannel) {
        val guild = channel.guild
        val senderMember = guild.getMemberById(sender)!!
        val targetMember = guild.getMemberById(target)!!
        val header = """
            ${channel.guild.publicRole.asMention}
            A report has been made against **${targetMember.formatted()}** by **${senderMember.formatted()}**
        """.trimIndent()
        val embed = StandardErrorResponse(
            "Report #$id", """
                **Reported By:** ${senderMember.formatted()} (${sender})
                **Reported User:** ${targetMember.formatted()} (${target})
                **Reason:** $reason
            """.trimIndent()
        )
        channel.sendMessage(header).embed(embed.asEmbed(targetMember.user)).queue()
    }
}