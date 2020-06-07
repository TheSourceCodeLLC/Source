package net.sourcebot.module.permissions.commands

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.nextMember
import net.sourcebot.api.nextRole
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.permission.PermissionHolder

abstract class PermissionCommand(
    internal val permissionHandler: PermissionHandler
) : RootCommand() {
    override val guildOnly = true

    fun getPermissionHolder(type: String, args: Arguments, guild: Guild): PermissionHolder {
        return when (type.toLowerCase()) {
            "user" -> {
                val member = args.nextMember(guild, "You did not specify a valid user!")
                permissionHandler.getUser(member)
            }
            "group" -> {
                permissionHandler.getGroup(
                    args.next("You did not specify a valid group!")
                ) ?: throw InvalidSyntaxException("You did not specify a valid group!")
            }
            "role" -> {
                val role = args.nextRole(guild, "You did not specify a valid role!")
                permissionHandler.getRole(role)
            }
            else -> throw InvalidSyntaxException("Invalid target type, `$type`!")
        }
    }
}