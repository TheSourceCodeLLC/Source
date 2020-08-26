package net.sourcebot.module.counting.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.sourcebot.api.properties.JsonSerial

class CountingData @JsonCreator constructor(
    @JsonProperty("channel") var channel: String?,
    @JsonProperty("record") var record: Long = 0
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