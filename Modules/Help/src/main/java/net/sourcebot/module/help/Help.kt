package net.sourcebot.module.help

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.help.command.HelpCommand

class Help : SourceModule() {
    override fun onEnable(source: Source) {
        val moduleHandler = source.moduleHandler
        val commandHandler = source.commandHandler
        registerCommands(HelpCommand(moduleHandler, commandHandler))
    }
}