package net.sourcebot.api.properties

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer

open class Properties(private val json: ObjectNode) {
    fun <T> optional(path: String, type: Class<T>): T? {
        val levels = path.split(".").iterator()
        var lastElem: JsonNode = json[levels.next()] ?: return null
        while (levels.hasNext()) {
            lastElem = lastElem[levels.next()]
        }
        return JsonSerial.fromJson(lastElem, type)
    }

    inline fun <reified T> optional(path: String) = optional(path, T::class.java)

    fun <T> required(path: String, type: Class<T>) =
        optional(path, type) ?: throw IllegalArgumentException("Could not load value at '$path'!")

    inline fun <reified T> required(path: String) = required(path, T::class.java)

    class Serial : JsonSerial<Properties> {
        override val serializer = object : StdSerializer<Properties>(
            Properties::class.java
        ) {
            override fun serialize(
                value: Properties,
                gen: JsonGenerator,
                provider: SerializerProvider
            ) = gen.writeTree(value.json)
        }
        override val deserializer = object : StdDeserializer<Properties>(
            Properties::class.java
        ) {
            override fun deserialize(
                p: JsonParser,
                ctxt: DeserializationContext
            ) = Properties(p.readValueAsTree())
        }
    }
}