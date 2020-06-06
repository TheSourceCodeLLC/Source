package net.sourcebot.module.permissions.commands.parent

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class AddParentCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "addparent"
    override val description = "Add a parent to a specified target."
    override val argumentInfo = ArgumentInfo(
        Argument("user|group|role", "The target's type."),
        Argument("target", "The target to add a parent for."),
        Argument("parent", "The parent to add.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val type = args.next("You did not specify a target type!")
        val target = args.next("You did not specify a valid target!")
        args.backtrack()
        val targetHolder = getPermissionHolder(type, args, message.guild)
        val parent = args.next("You did not specify a parent to add!")
        val group = permissionHandler.getGroup(parent) ?: return ErrorAlert(
            "Invalid Group!",
            "The group `$parent` does not exist!"
        )
        targetHolder.addParent(group)
        targetHolder.update()
        return SuccessAlert("Parent Added!", "Added parent `$parent` to $type `$target`.")
    }
}