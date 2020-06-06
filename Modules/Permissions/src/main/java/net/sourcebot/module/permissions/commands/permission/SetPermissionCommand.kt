package net.sourcebot.module.permissions.commands.permission

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class SetPermissionCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "setpermission"
    override val description = "Set a permission for a specific target."
    override val argumentInfo = ArgumentInfo(
        Argument("user|group|role", "The target's type."),
        Argument("target", "The target to set the permission for."),
        Argument("node", "The node to set for the target."),
        Argument("flag", "The flag to set for the node."),
        OptionalArgument("context", "The context this node should be set in.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val type = args.next("You did not specify a target type!")
        val target = args.next("You did not specify a valid target!")
        args.backtrack()
        val targetHolder = getPermissionHolder(type, args, message.guild)
        val node = args.next("You did not specify a node to set!")
        val flag = args.next(Adapter.BOOLEAN, "You did not specify a flag for the node!")
        val context = args.next()
        val desc = if (context != null) {
            targetHolder.setPermission(node, flag, context)
            "Updated $type `$target`: set `$node` = `$flag` in context `$context`"
        } else {
            targetHolder.setPermission(node, flag)
            "Updated $type `$target`: set `$node` = `$flag`"
        }
        targetHolder.update()
        return SuccessAlert("Permission Set!", desc)
    }
}