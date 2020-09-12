package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.response.SuccessResponse

class UnbanIncident(
    private val sender: Member,
    private val target: String,
    private val reason: String
) : SourceIncident(Type.UNBAN) {
    lateinit var unbanned: User
        internal set

    override fun execute(): Throwable? =
        try {
            val guild = sender.guild
            unbanned = guild.retrieveBanById(target).complete().user
            guild.unban(target).complete()
            null
        } catch (ex: Throwable) {
            ex
        }

    override fun sendLog(channel: TextChannel): Long {
        val case = computeId()
        val embed = SuccessResponse(
            "Unban - Case #$case",
            """
                **Unbanned By:** ${"%#s".format(sender.user)} (${sender.id}) 
                **Unbanned User:** ${"%#s".format(unbanned)} (${unbanned.id})
                **Reason:**: $reason
            """.trimIndent()
        ).asEmbed(unbanned)
        channel.sendMessage(embed).queue()
        return case
    }
}