package net.sourcebot.api.configuration

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.sourcebot.api.typeRefOf
import java.io.File
import java.net.URL

interface JsonSerial<T> {
    val serializer: JsonSerializer<T>
    val deserializer: JsonDeserializer<T>

    companion object {
        @JvmStatic
        val mapper: ObjectMapper = jacksonObjectMapper().enable(
            SerializationFeature.INDENT_OUTPUT
        ).enable(
            MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS
        )

        @JvmStatic
        fun <T> register(type: Class<T>, serial: JsonSerial<T>) {
            mapper.registerModule(SimpleModule().apply {
                addSerializer(type, serial.serializer)
                addDeserializer(type, serial.deserializer)
            })
        }

        @JvmStatic
        inline fun <reified T> register(
            serial: JsonSerial<T>
        ) = register(T::class.java, serial)

        @JvmStatic
        fun <T> toJson(obj: T): JsonNode = mapper.valueToTree(obj)

        @JvmStatic
        fun <T> fromJson(
            element: JsonNode,
            type: TypeReference<T>
        ): T = mapper.convertValue(element, type)

        @JvmStatic
        inline fun <reified T> fromJson(
            element: JsonNode
        ): T = fromJson(element, typeRefOf())

        @JvmStatic
        fun newObject(): ObjectNode = mapper.createObjectNode()

        @JvmStatic
        fun newArray(): ArrayNode = mapper.createArrayNode()

        @JvmStatic
        fun <T> fromFile(
            file: File,
            type: TypeReference<T>
        ): T = mapper.readValue(file, type)

        @JvmStatic
        inline fun <reified T> fromFile(
            file: File
        ): T = fromFile(file, typeRefOf())

        @JvmStatic
        fun <T> toFile(
            file: File,
            obj: T
        ) = mapper.writeValue(file, obj)

        @JvmStatic
        fun <T> fromUrl(
            url: String, type: TypeReference<T>
        ): T = mapper.readValue(URL(url), type)

        @JvmStatic
        inline fun <reified T> fromUrl(
            url: String
        ): T = fromUrl(url, typeRefOf())

        @JvmStatic inline fun <reified T> createSerializer(
            noinline serializer: (T, JsonGenerator, SerializerProvider) -> Unit
        ): JsonSerializer<T> = object : StdSerializer<T>(T::class.java) {
            override fun serialize(
                value: T, gen: JsonGenerator, provider: SerializerProvider
            ) = serializer(value, gen, provider)
        }

        @JvmStatic inline fun <reified T> createDeserializer(
            noinline deserializer: (JsonParser, DeserializationContext) -> T
        ): JsonDeserializer<T> = object : StdDeserializer<T>(T::class.java) {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = deserializer(p, ctxt)
        }
    }
}