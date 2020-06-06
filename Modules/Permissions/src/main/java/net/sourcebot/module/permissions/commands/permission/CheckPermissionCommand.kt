package net.sourcebot.module.permissions.commands.permission

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class CheckPermissionCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "checkpermission"
    override val description = "Check permissions for a specific target."
    override val argumentInfo = ArgumentInfo(
        Argument("user|group|role", "The target's type."),
        Argument("target", "The target to check permissions for."),
        Argument("node", "The permission node to check for."),
        OptionalArgument("context", "The context to check permissions in.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val type = args.next("You did not specify a target type!")
        val target = args.next("You did not specify a valid target!")
        args.backtrack()
        val targetHolder = getPermissionHolder(type, args, message.guild)
        val node = args.next("You did not specify a node to check for!")
        val context = args.next()
        val desc = if (context != null) {
            "${type.capitalize()} `$target`: `$node` @ `$context` = `${targetHolder.hasPermission(node, context)}`"
        } else {
            "${type.capitalize()} `$target`: `$node` = `${targetHolder.hasPermission(node)}`"
        }
        return InfoAlert("Permissions Check:", desc)
    }
}