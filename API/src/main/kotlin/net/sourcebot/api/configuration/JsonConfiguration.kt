package net.sourcebot.api.configuration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

@JsonSerialize(`as` = Properties::class)
open class JsonConfiguration @JsonCreator constructor(
    private val json: ObjectNode = JsonSerial.newObject()
) : Properties(json) {
    operator fun <T> set(path: String, obj: T): T {
        if (path.isBlank()) throw IllegalArgumentException(
            "Argument 'path' may not be empty!"
        )
        val parts = path.split(".")
        if (parts.size == 1) json.set(parts[0], JsonSerial.toJson(obj))
        else {
            val config = required(parts[0], ::JsonConfiguration)
            config[parts.subList(1, parts.size).joinToString(".")] = obj
        }
        return obj
    }

    fun save(dest: File) = JsonSerial.toFile(dest, json)
}