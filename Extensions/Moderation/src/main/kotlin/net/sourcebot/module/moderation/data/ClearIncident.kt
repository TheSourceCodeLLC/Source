package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageHistory
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.StandardInfoResponse

class ClearIncident(
    override val id: Long,
    private val sender: Member,
    private val channel: TextChannel,
    private val amount: Int,
    override val reason: String,
) : OneshotIncident() {
    override val source: String = sender.id
    override val target: String = channel.id
    override val type = Incident.Type.CLEAR

    override fun execute() {
        val history = MessageHistory(channel)
        val messages = ArrayList<List<Message>>()
        var toRetrieve = amount + 1
        while (toRetrieve >= 100) {
            messages += history.retrievePast(100).complete()
            toRetrieve -= 100
        }
        if (toRetrieve != 0) messages += history.retrievePast(toRetrieve).complete()
        messages.forEach { channel.deleteMessages(it).queue() }
    }

    override fun sendLog(logChannel: TextChannel): Message = logChannel.sendMessage(
        StandardInfoResponse(
            "Clear - Case #$id",
            """
                **Cleared By:** ${sender.formatLong()} ($source)
                **Cleared Channel:** ${channel.name} ($target)
                **Clear Amount:** $amount
                **Reason:** $reason
            """.trimIndent()
        ).asMessage(sender.user)
    ).complete()

    override fun asDocument() = super.asDocument().also {
        it["amount"] = amount
    }
}