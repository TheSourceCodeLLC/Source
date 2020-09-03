package net.sourcebot.api.configuration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
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

        @JvmStatic fun <T> registerSerial(type: Class<T>, serial: JsonSerial<T>) {
            val module = SimpleModule()
            module.addSerializer(type, serial.serializer)
            module.addDeserializer(type, serial.deserializer)
            mapper.registerModule(module)
        }

        @JvmStatic inline fun <reified T> registerSerial(
            serial: JsonSerial<T>
        ) = registerSerial(T::class.java, serial)

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

    }
}