package net.sourcebot.module.counting

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.counting.command.CountingCommand
import net.sourcebot.module.counting.data.CountingListener

class Counting : SourceModule() {
    override val configurationInfo = ConfigurationInfo("counting") {
        node("channel", "Channel ID for the counting channel.")
    }

    override fun onEnable() {
        val configManager = source.guildConfigurationManager
        registerCommands(CountingCommand(configManager))
        subscribeEvents(CountingListener(this, source.commandHandler, configManager))
    }
}