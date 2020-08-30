package net.sourcebot.api.configuration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.ObjectNode
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
        .removalListener<Guild, GuildConfiguration> { saveData(it.key, it.value) }
        .build(object : CacheLoader<Guild, GuildConfiguration>() {
            override fun load(
                key: Guild
            ) = GuildConfiguration.fromFile(File(dataFolder, "${key.id}.json"))
        })

    operator fun get(guild: Guild): GuildConfiguration = dataCache[guild]

    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
    }

    fun saveData(
        guild: Guild,
        guildConfiguration: GuildConfiguration
    ) = JsonSerial.toFile(File(dataFolder, "${guild.id}.json"), guildConfiguration)

    fun saveData(
        guild: Guild
    ) = saveData(guild, this[guild])

    fun saveAll() = dataCache.invalidateAll()
}

@JsonSerialize(`as` = Properties::class)
class GuildConfiguration @JsonCreator constructor(
    json: ObjectNode = JsonSerial.newObject()
) : JsonConfiguration(json) {
    companion object {
        @JvmStatic
        fun fromFile(
            file: File
        ): GuildConfiguration = file.apply {
            if (!exists()) JsonSerial.toFile(this, JsonSerial.newObject())
        }.let { JsonSerial.fromFile(it) }
    }
}