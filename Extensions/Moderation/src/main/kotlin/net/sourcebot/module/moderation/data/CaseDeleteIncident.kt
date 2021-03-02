package net.sourcebot.module.moderation.data

import com.mongodb.client.MongoCollection
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.asMessage
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.StandardInfoResponse
import org.bson.Document

class CaseDeleteIncident(
    private val collection: MongoCollection<Document>,
    override val id: Long,
    val sender: Member,
    val member: Member,
    override val reason: String
) : OneshotIncident() {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.CASE_DELETE
    private val delete = StandardInfoResponse(
        "Case Deletion - #$id",
        """
            **Deleted By:** ${sender.formatLong()} ($source)
            **Reason:** $reason
        """.trimIndent()
    )

    var deleted: Document? = null
    override fun execute() {
        deleted = collection.findOneAndDelete(
            Document().also {
                it["_id"] = id
                it["type"] = Document("\$ne", "CASE_DELETE")
            }
        ) ?: throw NoSuchElementException(
            "Case ID '$id' does not exist or has already been deleted!"
        )
    }

    override fun sendLog(logChannel: TextChannel): Message = logChannel.sendMessage(delete.asMessage(sender)).complete()
}