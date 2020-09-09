package net.sourcebot.api.configuration

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import java.io.File
import java.util.concurrent.TimeUnit

class GuildConfigurationManager(
    private val dataFolder: File
) {
    private val dataCache = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .removalListener<Guild, JsonConfiguration> { saveData(it.key, it.value) }
        .build(object : CacheLoader<Guild, JsonConfiguration>() {
            override fun load(
                key: Guild
            ) = JsonConfiguration.fromFile(File(dataFolder, "${key.id}.json"))
        })

    operator fun get(guild: Guild): JsonConfiguration = dataCache[guild]

    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
    }

    fun saveData(
        guild: Guild,
        guildConfiguration: JsonConfiguration
    ) = JsonSerial.toFile(File(dataFolder, "${guild.id}.json"), guildConfiguration)

    fun saveData(
        guild: Guild
    ) = saveData(guild, this[guild])

    fun saveAll() = dataCache.invalidateAll()
}