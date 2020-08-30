package net.sourcebot.api.module

import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.api.configuration.Properties

class ModuleDescriptor(json: ObjectNode) : Properties(json) {
    val main: String = required("main")

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