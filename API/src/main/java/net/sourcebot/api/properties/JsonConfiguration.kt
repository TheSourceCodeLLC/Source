package net.sourcebot.api.properties

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

open class JsonConfiguration(
    private val json: ObjectNode
) : Properties(json) {
    fun <T> set(path: String, obj: T): T {
        if (path.isBlank()) throw IllegalArgumentException(
            "Argument 'path' may not be empty!"
        )
        val parts = path.split(".")
        if (parts.size == 1) {
            json.set<JsonNode>(parts[0], JsonSerial.toJson(obj))
        } else {
            val toStore = JsonSerial.newObject()
            var lastElem = toStore
            parts.subList(1, parts.size - 2).forEachIndexed { index, elem ->
                lastElem = if (index == parts.size - 1) {
                    lastElem.set(elem, JsonSerial.toJson(obj))
                } else {
                    lastElem.set(elem, JsonSerial.newObject())
                }
            }
            json.set<JsonNode>(parts[0], toStore)
        }
        return obj
    }

    fun save(dest: File) = JsonSerial.toFile(dest, json)
}