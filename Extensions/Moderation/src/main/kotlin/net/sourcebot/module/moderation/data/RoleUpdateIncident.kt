package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse

class RoleUpdateIncident(
    override val id: Long,
    private val sender: Member,
    private val member: Member,
    private val role: Role,
    override val reason: String,
    private val action: Action
) : OneshotIncident() {
    override val source = sender.id
    override val target = member.id
    override val type = Incident.Type.ROLE_UPDATE
    private val kind = action.name.lowercase().capitalize().let {
        it + if (it.endsWith("d")) "ed" else "d"
    }
    private val heading = "Role Update - #$id"
    private val description = """
        **Updated By:** ${sender.formatLong()} ($source)
        **Updated User:** ${member.formatLong()} ($target)
        **Role $kind**: ${role.name} (${role.id})
        **Reason:** $reason
    """.trimIndent()

    private val update = when (action) {
        Action.ADD -> StandardSuccessResponse(heading, description)
        Action.REMOVE -> StandardErrorResponse(heading, description)
    }

    override fun asDocument() = super.asDocument().also {
        it["action"] = action.name
        it["role"] = role.id
    }

    override fun execute() {
        val guild = sender.guild
        when (action) {
            Action.ADD -> {
                if (member.roles.contains(role)) throw RoleUpdateException(
                    "That member already belongs to the specified role!"
                )
                val exception = runCatching {
                    guild.addRoleToMember(member, role).complete()
                }.exceptionOrNull() ?: return
                when (exception) {
                    is InsufficientPermissionException -> throw RoleUpdateException(
                        "I do not have permission to manage roles!"
                    )
                    is HierarchyException -> throw RoleUpdateException(
                        "I do not have permission to assign that role!"
                    )
                }
            }
            Action.REMOVE -> {
                if (!member.roles.contains(role)) throw RoleUpdateException(
                    "That member does not belong to the specified role!"
                )
                val exception = runCatching {
                    guild.removeRoleFromMember(member, role).complete()
                }.exceptionOrNull() ?: return
                when (exception) {
                    is InsufficientPermissionException -> throw RoleUpdateException(
                        "I do not have permission to manage roles!"
                    )
                    is HierarchyException -> throw RoleUpdateException(
                        "I do not have permission to assign that role!"
                    )
                }
            }
        }
    }

    override fun sendLog(logChannel: TextChannel): Message =
        logChannel.sendMessageEmbeds(update.asEmbed(sender.user)).complete()

    enum class Action { ADD, REMOVE }

    class RoleUpdateException(message: String) : RuntimeException(message)
}
