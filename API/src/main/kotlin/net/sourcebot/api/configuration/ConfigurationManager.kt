package net.sourcebot.api.configuration

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import java.io.File
import java.util.concurrent.TimeUnit

class ConfigurationManager(
    private val dataFolder: File
) {
    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
    }

    private val dataCache = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .removalListener<Guild, JsonConfiguration> { saveData(it.key, it.value) }
        .build(object : CacheLoader<Guild, JsonConfiguration>() {
            override fun load(
                key: Guild
            ) = File(dataFolder, "${key.id}.json").apply {
                if (!exists()) JsonSerial.toFile(this, JsonSerial.newObject())
            }.let<File, JsonConfiguration>(JsonSerial.Companion::fromFile)
        })

    operator fun get(guild: Guild): JsonConfiguration = dataCache[guild]

    fun saveData(
        guild: Guild,
        configuration: JsonConfiguration
    ) = JsonSerial.toFile(File(dataFolder, "${guild.id}.json"), configuration)

    fun saveAll() = dataCache.invalidateAll()
}