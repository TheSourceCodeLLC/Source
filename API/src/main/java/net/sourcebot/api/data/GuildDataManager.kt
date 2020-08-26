package net.sourcebot.api.data

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.properties.JsonConfiguration
import net.sourcebot.api.properties.JsonSerial
import java.io.File
import java.util.concurrent.TimeUnit

class GuildDataManager(
    private val dataFolder: File
) {
    private val dataCache = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .removalListener<Guild, GuildData> { saveData(it.key, it.value) }
        .build(object : CacheLoader<Guild, GuildData>() {
            override fun load(
                key: Guild
            ) = GuildData.fromFile(File(dataFolder, "${key.id}.json"))
        })

    operator fun get(guild: Guild): GuildData = dataCache[guild]

    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        JsonSerial.registerSerial(GuildData.Serial())
    }

    fun saveData(
        guild: Guild,
        guildData: GuildData
    ) = JsonSerial.toFile(File(dataFolder, "${guild.id}.json"), guildData)

    fun saveData(
        guild: Guild
    ) = saveData(guild, this[guild])
}

class GuildData(
    private val json: ObjectNode
) : JsonConfiguration(json) {
    companion object {
        @JvmStatic
        fun fromFile(
            file: File
        ): GuildData = file.apply {
            if (!exists()) JsonSerial.toFile(this, JsonSerial.newObject())
        }.let { JsonSerial.fromFile(it) }
    }

    class Serial : JsonSerial<GuildData> {
        override val serializer = object : StdSerializer<GuildData>(GuildData::class.java) {
            override fun serialize(
                value: GuildData,
                gen: JsonGenerator,
                provider: SerializerProvider
            ) = gen.writeTree(value.json)
        }
        override val deserializer = object : StdDeserializer<GuildData>(GuildData::class.java) {
            override fun deserialize(
                p: JsonParser,
                ctxt: DeserializationContext
            ): GuildData = GuildData(p.readValueAsTree())
        }
    }
}