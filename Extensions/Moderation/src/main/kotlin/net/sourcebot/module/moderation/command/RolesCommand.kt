package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.sourcebot.Source
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.getHighestRole
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.moderation.Moderation

class RolesCommand : ModerationRootCommand(
    "roles", "Manage member roles."
) {
    override val aliases = arrayOf("role")
    private inner class RolesAddCommand : ModerationCommand(
        "add", "Add a role to a member."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("target", "The member to update."),
            Argument("role", "The role to add."),
            Argument("reason", "Why you are adding this role.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val guild = message.guild
            val sender = message.member!!
            val target = args.next(Adapter.member(guild), "You did not specify a valid member to update!")
            val role = args.next(Adapter.role(guild), "You did not specify a valid role to add!")
            val reason = args.slurp(" ", "You did not specify a reason for adding this role!")
            if (!canModify(sender, role)) return StandardErrorResponse(
                "Role Add Failure!", "You do not have permission to modify that role!"
            )
            if (role.isPublicRole) return StandardErrorResponse(
                "Role Add Failure!", "You may not add members to the default role!"
            )
            if (role.isManaged) return StandardErrorResponse(
                "Role Add Failure!", "You may not add members to managed roles!"
            )
            val senderHighest = sender.getHighestRole()
            if (senderHighest.position < role.position) return StandardErrorResponse(
                "Role Add Failure!", "You do not have permission to add members to that role!"
            )
            val targetHighest = target.getHighestRole()
            if (senderHighest.position < targetHighest.position) return StandardErrorResponse(
                "Role Add Failure!", "You do not have permission to manage that person's roles!"
            )
            return Moderation.getPunishmentHandler(guild) { submitRoleAdd(sender, target, role, reason) }
        }
    }

    private inner class RolesRemoveCommand : ModerationCommand(
        "remove", "Remove a a role from a member."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("target", "The member to update."),
            Argument("role", "The role to remove."),
            Argument("reason", "Why you are removing this role.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val guild = message.guild
            val sender = message.member!!
            val target = args.next(Adapter.member(guild), "You did not specify a valid member to update!")
            val role = args.next(Adapter.role(guild), "You did not specify a valid role to remove!")
            val reason = args.slurp(" ", "You did not specify a reason for removing this role!")
            if (!canModify(sender, role)) return StandardErrorResponse(
                "Role Remove Failure!", "You do not have permission to modify that role!"
            )
            if (role.isPublicRole) return StandardErrorResponse(
                "Role Remove Failure!", "You may not remove members from the default role!"
            )
            if (role.isManaged) return StandardErrorResponse(
                "Role Remove Failure!", "You may not remove members from managed roles!"
            )
            val senderHighest = sender.getHighestRole()
            if (senderHighest.position < role.position) return StandardErrorResponse(
                "Role Remove Failure!", "You do not have permission to remove members from that role!"
            )
            val targetHighest = target.getHighestRole()
            if (senderHighest.position < targetHighest.position) return StandardErrorResponse(
                "Role Remove Failure!", "You do not have permission to manage that person's roles!"
            )
            return Moderation.getPunishmentHandler(guild) { submitRoleRemove(sender, target, role, reason) }
        }
    }

    private inner class RolesListCommand : ModerationCommand(
        "list", "Show the available roles."
    ) {
        override val cleanupResponse = false
        override fun execute(message: Message, args: Arguments): Response {
            val guild = message.guild
            val member = message.member!!
            val roles = guild.roles
                .filter { !it.isPublicRole && !it.isManaged }
                .filter { member.getHighestRole().position > it.position }
                .filter { canModify(member, it) }
            val listing =
                if (roles.isEmpty()) "You do not have permission to modify any roles."
                else """
                    You have access to modify each of the following roles:
                    
                    ${roles.joinToString("\n") { "**${it.name}**" }}
                """.trimIndent()
            return StandardInfoResponse("Roles Listing", listing)
        }
    }

    init {
        addChildren(
            RolesAddCommand(),
            RolesRemoveCommand(),
            RolesListCommand()
        )
    }

    private fun canModify(member: Member, role: Role) = Source.PERMISSION_HANDLER.memberHasPermission(
        member, "moderation.roles.modify.${role.id}", null
    )
}