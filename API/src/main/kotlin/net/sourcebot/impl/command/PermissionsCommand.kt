package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse

class PermissionsCommand(
    private val permissionHandler: PermissionHandler
) : RootCommand() {
    override val name = "permissions"
    override val description = "Modify Source permissions."
    override val guildOnly = true
    override val argumentInfo = ArgumentInfo(
        Argument("role|user", "The type of entity to modify."),
        Argument("target", "The target to modify."),
        Argument("set|unset|info|check|clear", "The operation you wish to perform."),
        Argument("data...", "The data supplied to the chosen operation.")
    )
    override val aliases = arrayOf("permission", "perms", "perm")
    override val permission = name

    override fun execute(message: Message, args: Arguments): Response {
        val permissionData = permissionHandler.getData(message.guild)
        val user = permissionData.getUser(message.member!!)
        val type = args.next("You did not specify a type to modify!").toLowerCase()
        val permissible = when (type) {
            "role" -> {
                val role = args.next(Adapter.role(message.guild), "You did not specify a valid role!")
                permissionData.getRole(role)
            }
            "user" -> {
                val member = args.next(Adapter.member(message.guild), "You did not specify a valid member!")
                permissionData.getUser(member)
            }
            else -> throw InvalidSyntaxException("You did not specify a valid type!")
        }
        val asMention = permissible.asMention()
        val toSend = when (args.next(
            "You did not specify an operation!\n" +
            "Valid operations: `set`,`unset`,`info`,`check`,`clear`"
        ).toLowerCase()) {
            "set" -> {
                permissionHandler.checkPermission(user, "permissions.$type.set", message.channel) {
                    val node = args.next("You did not specify a node to set!")
                    val flag = args.next(Adapter.boolean(), "You did not specify a flag for the node!")
                    val context = args.next()
                    val description = if (context != null) {
                        permissible.setPermission(node, flag, context)
                        "Set permission for $asMention: `$node` = `$flag` @ `$context`"
                    } else {
                        permissible.setPermission(node, flag)
                        "Set permission for $asMention: `$node` = `$flag`"
                    }
                    SuccessResponse("Permission Set!", description)
                }
            }
            "unset" -> {
                permissionHandler.checkPermission(user, "permissions.$type.unset", message.channel) {
                    val node = args.next("You did not specify a node to unset!")
                    val context = args.next()
                    val description = if (context != null) {
                        permissible.unsetPermission(node, context)
                        "Unset `$node` for $asMention @ `$context`"
                    } else {
                        permissible.unsetPermission(node)
                        "Unset `$node` for $asMention"
                    }
                    SuccessResponse("Permission Unset!", description)
                }
            }
            "info" -> {
                permissionHandler.checkPermission(user, "permissions.info", message.channel) {
                    val permissions = permissible.getPermissions()
                    val infoOutput = permissions.joinToString("\n") {
                        if (it.context != null) "`${it.node}`: `${it.flag}` @ `${it.context}`"
                        else "`${it.node}`: `${it.flag}`"
                    }.ifEmpty { "No permissions set." }
                    InfoResponse("Permission Information", "Permissions for $asMention:\n$infoOutput")
                }
            }
            "check" -> {
                permissionHandler.checkPermission(user, "permissions.$type.check", message.channel) {
                    val node = args.next("You did not specify a node to check!")
                    val context = args.next()
                    val description = if (context != null) {
                        val has = permissible.hasPermission(node, context)
                        "Permission check for $asMention; `$node` @ `$context`: `$has`"
                    } else {
                        val has = permissible.hasPermission(node)
                        "Permission check for $asMention; `$node`: `$has`"
                    }
                    InfoResponse("Permission Check", description)
                }
            }
            "clear" -> {
                permissionHandler.checkPermission(user, "permissions.$type.clear", message.channel) {
                    val context = args.next()
                    val description = if (context != null) {
                        permissible.clearPermissions(context)
                        "Permissions cleared for $asMention @ `$context`"
                    } else {
                        permissible.clearPermissions()
                        "Permissions cleared for $asMention"
                    }
                    SuccessResponse("Permissions Cleared!", description)
                }
            }
            else -> throw InvalidSyntaxException(
                "Invalid Operation!\n" +
                "Valid operations: `set`,`unset`,`info`,`check`,`clear`"
            )
        }
        permissible.update(permissionData)
        return toSend
    }
}