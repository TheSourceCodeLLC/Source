package net.sourcebot.module.freegames

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.freegames.command.RefreshGamesCommand
import net.sourcebot.module.freegames.emitter.FreeGameEmitter
import net.sourcebot.module.freegames.listener.FreeGameListener

class FreeGames : SourceModule() {
    // TODO: ADD AUTO PUBLISH OPTION (This can not be done until the upgrade to JDA 5 is complete)
    override val configurationInfo = ConfigurationInfo("free-games") {
        section("services") {
            node("steam", "Determines if the bot will post free games from Steam.", true)
            node("epic-games", "Determines if the bot will post free games from the Epic Games Store.", true)
        }
        node("channel", "Channel ID for the free-games channel.")
        node(
            "role",
            "Role ID for the free-games role. If no ID is provided, the bot will not ping any role when posting a new free game listing."
        )
    }

    override fun enable() {
        registerCommands(
            RefreshGamesCommand()
        )
        subscribeEvents(FreeGameListener())
        gameEmitter.startEmitting()
    }

    companion object {
        val gameEmitter = FreeGameEmitter()
    }
}