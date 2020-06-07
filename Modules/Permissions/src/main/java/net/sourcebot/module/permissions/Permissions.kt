package net.sourcebot.module.permissions

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.permissions.commands.group.CreateGroupCommand
import net.sourcebot.module.permissions.commands.group.DeleteGroupCommand
import net.sourcebot.module.permissions.commands.group.ListGroupsCommand
import net.sourcebot.module.permissions.commands.parent.AddParentCommand
import net.sourcebot.module.permissions.commands.parent.ClearParentsCommand
import net.sourcebot.module.permissions.commands.parent.ParentInfoCommand
import net.sourcebot.module.permissions.commands.parent.RemoveParentCommand
import net.sourcebot.module.permissions.commands.permission.CheckPermissionCommand
import net.sourcebot.module.permissions.commands.permission.ClearPermissionsCommand
import net.sourcebot.module.permissions.commands.permission.SetPermissionCommand
import net.sourcebot.module.permissions.commands.permission.UnsetPermissionCommand

class Permissions : SourceModule() {
    override fun onEnable(source: Source) {
        val permissionHandler = source.permissionHandler
        registerCommands(
            SetPermissionCommand(permissionHandler),
            UnsetPermissionCommand(permissionHandler),
            ClearPermissionsCommand(permissionHandler),
            CheckPermissionCommand(permissionHandler),

            ParentInfoCommand(permissionHandler),
            AddParentCommand(permissionHandler),
            RemoveParentCommand(permissionHandler),
            ClearParentsCommand(permissionHandler),

            CreateGroupCommand(permissionHandler),
            DeleteGroupCommand(permissionHandler),
            ListGroupsCommand(permissionHandler)
        )
    }
}