package net.sourcebot.api.configuration

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class ConfigurationManager(private val dataFolder: File) {
    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
    }

    private val dataCache = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .removalListener<Guild, JsonConfiguration> { saveData(it.key!!, it.value!!) }
        .build(object : CacheLoader<Guild, JsonConfiguration>() {
            override fun load(key: Guild) = File(dataFolder, "${key.id}.json").let(JsonConfiguration::fromFile)
        })

    operator fun get(guild: Guild): JsonConfiguration = dataCache[guild]

    fun saveData(
        guild: Guild,
        configuration: JsonConfiguration
    ) = JsonSerial.toFile(File(dataFolder, "${guild.id}.json"), configuration)

    fun saveAll() = dataCache.invalidateAll()

    fun <M : SourceModule> moduleConfig(type: Class<M>, guild: Guild): JsonConfiguration {
        val module = Source.MODULE_HANDLER.getExtension(type)!!
        val configInfo = module.configurationInfo
        val guildConf = Source.CONFIG_MANAGER[guild]
        return guildConf.proxyObj(configInfo.fullName).also(configInfo::applyDefaults)
    }

    inline fun <reified M : SourceModule> moduleConfig(guild: Guild) = moduleConfig(M::class.java, guild)
}

fun <T : SourceModule> KClass<T>.config(guild: Guild): JsonConfiguration {
    return Source.CONFIG_MANAGER.moduleConfig(this.java, guild)
}