package net.sourcebot.impl

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.impl.command.help.HelpCommand
import net.sourcebot.impl.command.information.OnlineCommand
import net.sourcebot.impl.command.information.TimingsCommand
import net.sourcebot.impl.command.permissions.PermissionsCommand

class BaseModule : SourceModule() {
    override fun onEnable(source: Source) {
        val moduleHandler = source.moduleHandler
        val commandHandler = source.commandHandler
        val permissionHandler = source.permissionHandler
        registerCommands(
            HelpCommand(moduleHandler, commandHandler),
            OnlineCommand(),
            TimingsCommand(),
            PermissionsCommand(permissionHandler)
        )
    }
}