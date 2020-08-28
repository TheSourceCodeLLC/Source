package net.sourcebot.module.starboard.misc

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.data.GuildDataManager

class StarboardDataManager(
    private val dataManager: GuildDataManager
) {
    operator fun get(
        guild: Guild
    ): StarboardData = dataManager[guild].run {
        required("starboard") { set("starboard", StarboardData()) }
    }

    fun save(guild: Guild, data: StarboardData) {
        val guildData = dataManager[guild]
        guildData["starboard"] = data
        dataManager.saveData(guild, guildData)
    }
}