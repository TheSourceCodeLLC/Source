package net.sourcebot.module.counting

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.counting.command.CountingCommand
import net.sourcebot.module.counting.data.CountingListener

class Counting : SourceModule() {
    override fun onEnable(source: Source) {
        CountingListener(source, source.guildConfigurationManager).listen(this)
        source.commandHandler.registerCommands(this, CountingCommand(source.guildConfigurationManager))
    }
}