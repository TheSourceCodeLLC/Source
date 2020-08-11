package net.sourcebot.module.counting.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.properties.JsonSerial
import java.io.File

class CountingDataController(
    private val dataFile: File
) {
    private val typeRef = object : TypeReference<HashMap<String, CountingData>>() {}
    private val data = HashMap<String, CountingData>()

    init {
        val read = JsonSerial.mapper.readValue(dataFile, typeRef)
        data.putAll(read)
    }

    class CountingData @JsonCreator constructor(
        @JsonProperty("channel") var channel: String?,
        @JsonProperty("record") var record: Long
    ) {
        class Serial : JsonSerial<CountingData> {
            override val serializer = object : StdSerializer<CountingData>(
                CountingData::class.java
            ) {
                override fun serialize(
                    obj: CountingData,
                    gen: JsonGenerator,
                    provider: SerializerProvider
                ) {
                    gen.writeStartObject()
                    gen.writeFieldName("channel")
                    gen.writeString(obj.channel)
                    gen.writeFieldName("record")
                    gen.writeNumber(obj.record)
                    gen.writeEndObject()
                }

            }
            override val deserializer = object : StdDeserializer<CountingData>(
                CountingData::class.java
            ) {
                override fun deserialize(
                    parser: JsonParser,
                    ctxt: DeserializationContext
                ): CountingData {
                    val node = parser.readValueAsTree<JsonNode>()
                    val channel = node["channel"]?.asText()
                    val record = node["record"].asLong()
                    return CountingData(channel, record)
                }
            }
        }

        companion object {
            init {
                JsonSerial.registerSerial(Serial())
            }
        }
    }

    fun save() {
        JsonSerial.mapper.writeValue(dataFile, data)
    }

    fun getData(guild: Guild): CountingData = data.computeIfAbsent(guild.id) {
        CountingData(null, 1)
    }
}