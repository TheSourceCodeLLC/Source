package net.sourcebot.impl.command.permissions

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.nextMember
import net.sourcebot.api.nextRole
import net.sourcebot.api.permission.PermissionHandler

class PermissionsCommand(
    private val permissionHandler: PermissionHandler
) : RootCommand() {
    override val name = "permissions"
    override val description = "Modify Source permissions."
    override val guildOnly = true
    override val argumentInfo = ArgumentInfo(
        Argument("group|role|user", "The type of entity to modify."),
        Argument("target", "The target to modify."),
        Argument("parents|permissions", "The element you wish to modify."),
        Argument("<add|remove|list|clear>|<set|unset|check|clear>", "The operation you wish to perform."),
        Argument("data...", "The data supplied to the chosen operation.")
    )
    override val aliases = arrayOf("permission", "perms", "perm")

    override fun execute(message: Message, args: Arguments): Alert {
        val permissionData = permissionHandler.getData(message.guild)
        val type = args.next("You did not specify a type to modify!").toLowerCase()
        val target = args.next("You did not specify a target!")
        args.backtrack()
        val permissible = when (type) {
            "group" -> {
                val groupName = args.next("You did not specify a valid group name!")
                permissionData.getGroup(groupName)
                ?: throw InvalidSyntaxException("There is no group named `$groupName`!")
            }
            "role" -> {
                val role = args.nextRole(message.guild, "You did not specify a valid role!")
                permissionData.getRole(role)
            }
            "user" -> {
                val member = args.nextMember(message.guild, "You did not specify a valid member!")
                permissionData.getUser(member)
            }
            else -> throw InvalidSyntaxException("You did not specify a valid type!")
        }
        val toSend = when (args.next("You did not specify an element to modify!").toLowerCase()) {
            "parents" -> {
                when (args.next(
                    "You did not specify an operation!\n" +
                    "Valid operations: `add`,`remove`,`list`,`clear`"
                ).toLowerCase()) {
                    "add" -> {
                        val toAdd = args.next("You did not specify a group to add!")
                        val group = permissionData.getGroup(toAdd)
                                    ?: throw InvalidSyntaxException("There is no group named `$toAdd`!")
                        permissible.addParent(group)
                        SuccessAlert("Parent Added!", "Added parent `$toAdd` to $type `$target`!")
                    }
                    "remove" -> {
                        val toRemove = args.next("You did not specify a group to remove!")
                        val group = permissionData.getGroup(toRemove)
                                    ?: throw InvalidSyntaxException("There is no group named `$toRemove`!")
                        permissible.removeParent(group)
                        SuccessAlert("Parent Removed!", "Removed parent `$toRemove` from $type `$target`!")
                    }
                    "list" -> {
                        SuccessAlert(
                            "${type.capitalize()} `$target`'s Parents:",
                            permissible.getParents().joinToString(",") {
                                "`${it.name}"
                            })
                    }
                    "clear" -> {
                        permissible.clearParents()
                        SuccessAlert("Parents Cleared!", "${type.capitalize()} `$target`'s parents have been cleared!")
                    }
                    else -> throw InvalidSyntaxException(
                        "Invalid Operation!\n" +
                        "Valid operations: `add`,`remove`,`check`,`clear`"
                    )
                }
            }
            "permissions" -> {
                when (args.next(
                    "You did not specify an operation!\n" +
                    "Valid operations: `set`,`unset`,`check`,`clear`"
                ).toLowerCase()) {
                    "set" -> {
                        val node = args.next("You did not specify a node to set!")
                        val flag = args.next(Adapter.BOOLEAN, "You did not specify a flag for the node!")
                        val context = args.next()
                        val description = if (context != null) {
                            permissible.setPermission(node, flag, context)
                            "Set permission for $type `$target`: `$node` = `$flag` @ `$context`"
                        } else {
                            permissible.setPermission(node, flag)
                            "Set permission for $type `$target`: `$node` = `$flag`"
                        }
                        SuccessAlert("Permission Set!", description)
                    }
                    "unset" -> {
                        val node = args.next("You did not specify a node to unset!")
                        val context = args.next()
                        val description = if (context != null) {
                            permissible.unsetPermission(node, context)
                            "Unset `$node` for $type `$target` @ `$context`"
                        } else {
                            permissible.unsetPermission(node)
                            "Unset `$node` for $type `$target`"
                        }
                        SuccessAlert("Permission Unset!", description)
                    }
                    "check" -> {
                        val node = args.next("You did not specify a node to check!")
                        val context = args.next()
                        val description = if (context != null) {
                            val has = permissible.hasPermission(node, context)
                            "Permission check for $type `$target`; `$node` @ `$context`: `$has`"
                        } else {
                            val has = permissible.hasPermission(node)
                            "Permission check for $type `$target`; `$node`: `$has`"
                        }
                        InfoAlert("Permission Check", description)
                    }
                    "clear" -> {
                        val context = args.next()
                        val description = if (context != null) {
                            permissible.clearPermissions(context)
                            "Permissions cleared for $type `$target` @ `$context`"
                        } else {
                            permissible.clearPermissions()
                            "Permissions cleared for $type `$target`"
                        }
                        SuccessAlert("Permissions Cleared!", description)
                    }
                    else -> throw InvalidSyntaxException(
                        "Invalid Operation!\n" +
                        "Valid operations: `set`,`unset`,`check`,`clear`"
                    )
                }
            }
            else -> throw InvalidSyntaxException("You did not specify a valid element!")
        }
        permissible.update()
        return toSend
    }
}