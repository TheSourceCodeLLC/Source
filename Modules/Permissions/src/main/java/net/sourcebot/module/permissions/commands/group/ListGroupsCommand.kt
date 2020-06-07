package net.sourcebot.module.permissions.commands.group

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class ListGroupsCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "listgroups"
    override val description = "List available permission groups."
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val groups = permissionHandler.getGroups()
        val groupList = groups.joinToString(",") {
            "`${it.name}`"
        }
        return InfoAlert("Group Listing", "Available groups: $groupList")
    }
}