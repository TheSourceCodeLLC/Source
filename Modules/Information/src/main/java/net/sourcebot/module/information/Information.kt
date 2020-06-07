package net.sourcebot.module.information

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.information.commands.OnlineCommand
import net.sourcebot.module.information.commands.TimingsCommand

class Information : SourceModule() {
    override fun onEnable(source: Source) {
        registerCommands(
            OnlineCommand(),
            TimingsCommand()
        )
    }
}