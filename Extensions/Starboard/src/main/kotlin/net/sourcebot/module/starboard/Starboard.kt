package net.sourcebot.module.starboard

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.starboard.misc.StarboardDataManager
import net.sourcebot.module.starboard.misc.StarboardListener

class Starboard : SourceModule() {
    override val configurationInfo = ConfigurationInfo("starboard") {
        node("channel", "Channel ID for the starboard channel.")
        node(
            "nsfw-channel",
            "Channel ID for nsfw-starboard channel. If defined, all starred posts in NSFW channels will be posted here, assuming they are not excluded."
        )
        node("threshold", "Star threshold for starboard messages.")
        node("excluded-channels", "Channel IDs blacklisted from starboard.")
    }

    override fun enable() {
        subscribeEvents(StarboardListener(StarboardDataManager()))
    }
}