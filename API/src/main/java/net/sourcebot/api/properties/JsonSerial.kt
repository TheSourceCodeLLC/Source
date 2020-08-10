package net.sourcebot.api.properties

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule

interface JsonSerial<T> {
    val serializer: JsonSerializer<T>
    val deserializer: JsonDeserializer<T>

    companion object {
        @JvmStatic val mapper: ObjectMapper = ObjectMapper().enable(
            SerializationFeature.INDENT_OUTPUT
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

        @JvmStatic fun <T> toJson(
            obj: T
        ): JsonNode = mapper.valueToTree(obj)

        @JvmStatic fun <T> fromJson(
            element: JsonNode,
            type: Class<T>
        ): T = mapper.convertValue(element, type)

        @JvmStatic inline fun <reified T> fromJson(
            element: JsonNode
        ): T = fromJson(element, T::class.java)
    }
}