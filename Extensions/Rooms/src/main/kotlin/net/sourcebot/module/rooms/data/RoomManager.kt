package net.sourcebot.module.rooms.data

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.configuration.GuildConfigurationManager
import net.sourcebot.api.database.MongoDB

class RoomManager(
    private val configurationManager: GuildConfigurationManager,
    private val mongodb: MongoDB
) {
    operator fun get(
        guild: Guild
    ): RoomsConfiguration {
        val configuration = configurationManager[guild]
        return configuration.required("rooms") {
            val computed = configuration.set("rooms", RoomsConfiguration())
            configurationManager.saveData(guild, configuration)
            computed
        }
    }

    fun createRoom() {

    }
}