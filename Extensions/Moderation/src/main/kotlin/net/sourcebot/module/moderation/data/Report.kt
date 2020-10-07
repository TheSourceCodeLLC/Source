package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatted
import net.sourcebot.api.response.ErrorResponse
import org.bson.Document

class Report(
    val id: Long,
    val sender: Member,
    val target: Member,
    val reason: String
) {
    fun asDocument(): Document = Document().also {
        it["_id"] = id
        it["sender"] = sender.id
        it["target"] = target.id
        it["reason"] = reason
    }

    fun send(channel: TextChannel) {
        val header = """
            ${channel.guild.publicRole.asMention}
            A report has been made against **${target.formatted()}** by **${sender.formatted()}**
        """.trimIndent()
        val embed = ErrorResponse(
            "Report #$id", """
                **Reported By:** ${sender.formatted()} (${sender.id})
                **Reported User:** ${target.formatted()} (${target.id})
                **Reason:** $reason
            """.trimIndent()
        )
        channel.sendMessage(header).embed(embed.asEmbed(target.user)).queue()
    }
}