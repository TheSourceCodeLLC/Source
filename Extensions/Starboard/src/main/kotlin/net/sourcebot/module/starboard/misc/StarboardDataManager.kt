package net.sourcebot.module.starboard.misc

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.configuration.GuildConfigurationManager

class StarboardDataManager(
    private val configurationManager: GuildConfigurationManager
) {
    operator fun get(
        guild: Guild
    ): StarboardData = configurationManager[guild].run {
        required("starboard") { set("starboard", StarboardData()) }
    }

    fun save(guild: Guild, data: StarboardData) {
        val guildData = configurationManager[guild]
        guildData["starboard"] = data
        configurationManager.saveData(guild, guildData)
    }
}