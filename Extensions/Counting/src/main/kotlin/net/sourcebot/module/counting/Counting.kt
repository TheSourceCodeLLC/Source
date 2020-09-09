package net.sourcebot.module.counting

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.counting.command.CountingCommand
import net.sourcebot.module.counting.data.CountingListener

class Counting : SourceModule() {
    override fun onEnable() {
        CountingListener(source, source.guildConfigurationManager).listen(this)
        registerCommands(CountingCommand(source.guildConfigurationManager))
    }
}