package net.sourcebot.module.starboard

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.starboard.command.StarboardCommand
import net.sourcebot.module.starboard.misc.StarboardDataManager
import net.sourcebot.module.starboard.misc.StarboardListener

class Starboard : SourceModule() {
    override fun onEnable(source: Source) {
        val dataManager = StarboardDataManager(source.guildConfigurationManager)
        source.commandHandler.registerCommands(
            this,
            StarboardCommand(dataManager)
        )
        StarboardListener(
            source.jdaEventSystem,
            source.mongodb,
            dataManager
        ).listen(this)
    }
}