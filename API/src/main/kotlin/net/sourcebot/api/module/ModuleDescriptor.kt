package net.sourcebot.api.module

import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.api.configuration.JsonConfiguration

class ModuleDescriptor(json: ObjectNode) : JsonConfiguration(json) {
    val main by delegateRequired<String>()
    val name by delegateRequired<String>()
    val version by delegateRequired<String>()
    val description by delegateRequired<String>()
    val author by delegateRequired<String>()

    val hardDepends: List<String> = optional("hard-depend") ?: emptyList()
    val softDepends: List<String> = optional("soft-depend") ?: emptyList()

    operator fun component1() = name
    operator fun component2() = version
    operator fun component3() = description
    operator fun component4() = author
}