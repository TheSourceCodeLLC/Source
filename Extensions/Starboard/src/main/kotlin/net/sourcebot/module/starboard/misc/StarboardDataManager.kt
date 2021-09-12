package net.sourcebot.module.starboard.misc

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonConfiguration

class StarboardDataManager {
    private val configurationManager = Source.CONFIG_MANAGER
    operator fun get(
        guild: Guild
    ): JsonConfiguration = configurationManager[guild].run {
        required("starboard") {
            JsonConfiguration(
                mapOf(
                    "channel" to null,
                    "nsfw-channel" to null,
                    "threshold" to 5,
                    "excluded-channels" to emptyList<String>()
                )
            )
        }
    }

    fun save(guild: Guild, data: JsonConfiguration) {
        val guildData = configurationManager[guild]
        guildData["starboard"] = data
        configurationManager.saveData(guild, guildData)
    }
}