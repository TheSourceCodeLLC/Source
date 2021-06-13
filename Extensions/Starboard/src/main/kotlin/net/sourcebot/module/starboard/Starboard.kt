package net.sourcebot.module.starboard

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.starboard.misc.StarboardDataManager
import net.sourcebot.module.starboard.misc.StarboardListener

class Starboard : SourceModule() {
    override val configurationInfo = ConfigurationInfo("starboard") {
        node("channel", "Channel ID for the starboard channel.")
        node("threshold", "Star threshold for starboard messages.")
    }

    override fun enable() {
        subscribeEvents(StarboardListener(StarboardDataManager()))
    }
}