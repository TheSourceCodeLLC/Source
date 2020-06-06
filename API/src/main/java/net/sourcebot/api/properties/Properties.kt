package net.sourcebot.api.properties

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import java.lang.reflect.Type

open class Properties(private val json: JsonObject) {
    fun <T> optional(path: String, type: Class<T>): T? {
        val levels = path.split(".").iterator()
        var lastElem: JsonElement = json[levels.next()] ?: return null
        while (levels.hasNext()) {
            lastElem = lastElem.asJsonObject[levels.next()]
        }
        return JsonSerial.gson.fromJson(lastElem, type)
    }

    inline fun <reified T> optional(path: String) = optional(path, T::class.java)

    fun <T> required(path: String, type: Class<T>) =
        optional(path, type) ?: throw IllegalArgumentException("Could not load value at '$path'!")

    inline fun <reified T> required(path: String) = required(path, T::class.java)

    class Serial : JsonSerial<Properties> {
        override fun serialize(
            obj: Properties,
            type: Type,
            context: JsonSerializationContext
        ) = obj.json

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ) = Properties(element as JsonObject)
    }
}