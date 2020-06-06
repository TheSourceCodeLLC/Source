package net.sourcebot.module.permissions.commands.group

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class CreateGroupCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "creategroup"
    override val description = "Create a permission group."
    override val argumentInfo = ArgumentInfo(
        Argument("name", "The name of the group to create."),
        Argument("weight", "The weight of the new group.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val name = args.next("You did not specify a group to create!")
        val exists = permissionHandler.getGroup(name) != null
        return if (exists) {
            ErrorAlert("Duplicate Group!", "The group `$name` already exists!")
        } else {
            val weight = args.next(Adapter.INTEGER, "You did not specify a valid group weight!")
            permissionHandler.createGroup(name, weight)
            SuccessAlert("Group Created!", "Created group `$name` with a weight of `$weight`!")
        }
    }
}