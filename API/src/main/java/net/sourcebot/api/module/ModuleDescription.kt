package net.sourcebot.api.module

import com.google.gson.JsonObject
import net.sourcebot.api.properties.Properties

class ModuleDescription(json: JsonObject) : Properties(json) {
    val main: String? = optional("main")

    val name: String = required("name")
    val version: String = required("version")
    val description: String = required("description")

    val author: String = required("author")

    val hardDepends: List<String> = optional("hard-depend") ?: emptyList()
    val softDepends: List<String> = optional("soft-depend") ?: emptyList()

    operator fun component1() = name
    operator fun component2() = version
    operator fun component3() = description
    operator fun component4() = author
}