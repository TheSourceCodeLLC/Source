package net.sourcebot.module.freegames

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.freegames.command.RefreshGamesCommand
import java.util.concurrent.TimeUnit

class FreeGames : SourceModule() {
    override val configurationInfo = ConfigurationInfo("free-games") {
        section("services") {
            node("steam", "Determines if the bot will post free games from Steam.")
            node("epic-games", "Determines if the bot will post free games from the Epic Games Store.")
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
    }

    companion object {
        private val freeGameHandlers = CacheBuilder.newBuilder()
            .weakKeys().expireAfterWrite(10, TimeUnit.MINUTES)
            .build(object : CacheLoader<Guild, FreeGameHandler>() {
                override fun load(guild: Guild) = FreeGameHandler(guild)
            })

        fun getFreeGameHandler(guild: Guild): FreeGameHandler = freeGameHandlers[guild]
    }
}