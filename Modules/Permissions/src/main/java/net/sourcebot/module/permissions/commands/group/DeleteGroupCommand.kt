package net.sourcebot.module.permissions.commands.group

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class DeleteGroupCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "deletegroup"
    override val description = "Delete a permission group."
    override val argumentInfo = ArgumentInfo(
        Argument("group", "The group to delete.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val group = args.next("You did not specify a group to delete!")
        val exists = permissionHandler.getGroup(group)
        return if (exists != null) {
            exists.delete()
            SuccessAlert("Group Deleted!", "The group `$group` has been deleted!")
        } else {
            ErrorAlert("Unknown Group!", "The group `$group` does not exist!")
        }
    }
}