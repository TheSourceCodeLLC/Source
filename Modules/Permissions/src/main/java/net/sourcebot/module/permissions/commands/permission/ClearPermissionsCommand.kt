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

class ClearPermissionsCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "clearpermissions"
    override val description = "Clear permissions for a specific target."
    override val argumentInfo = ArgumentInfo(
        Argument("user|group|role", "The target's type."),
        Argument("target", "The target to clear permissions for."),
        OptionalArgument("context", "The context to clear permissions in.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val type = args.next("You did not specify a target type!")
        val target = args.next("You did not specify a valid target!")
        args.backtrack()
        val targetHolder = getPermissionHolder(type, args, message.guild)
        val context = args.next()
        val desc = if (context != null) {
            targetHolder.clearPermissions(context)
            "Cleared permissions for $type `$target` in context `$context`"
        } else {
            targetHolder.clearPermissions()
            "Cleared permissions for $type `$target`"
        }
        targetHolder.update()
        return SuccessAlert("Permissions Cleared!", desc)
    }
}