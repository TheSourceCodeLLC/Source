package net.sourcebot.module.permissions.commands.permission

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class UnsetPermissionCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "unsetpermission"
    override val description = "Unset a permission for a specific target."
    override val argumentInfo = ArgumentInfo(
        Argument("user|group|role", "The target's type."),
        Argument("target", "The target to unset the permission from."),
        Argument("node", "The node to unset from the target."),
        OptionalArgument("context", "The context this node should be unset from.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val type = args.next("You did not specify a target type!")
        val target = args.next("You did not specify a valid target!")
        args.backtrack()
        val targetHolder = getPermissionHolder(type, args, message.guild)
        val node = args.next("You did not specify a node to set!")
        val context = args.next()
        val desc = if (context != null) {
            targetHolder.unsetPermission(node, context)
            "Updated $type `$target`: unset `$node` in context `$context`"
        } else {
            targetHolder.unsetPermission(node)
            "Updated $type `$target`: unset `$node`"
        }
        targetHolder.update()
        return SuccessAlert("Permission Unset!", desc)
    }
}