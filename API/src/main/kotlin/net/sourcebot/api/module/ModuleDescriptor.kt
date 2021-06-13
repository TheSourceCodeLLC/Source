package net.sourcebot.api.module

import com.fasterxml.jackson.databind.node.ObjectNode
import me.hwiggy.extensible.contract.Descriptor
import net.sourcebot.api.configuration.JsonConfiguration

class ModuleDescriptor(json: ObjectNode) : JsonConfiguration(json), Descriptor {
    val main by delegateRequired<String>()
    override val name by delegateRequired<String>()
    val version by delegateRequired<String>()
    val description by delegateRequired<String>()
    val author by delegateRequired<String>()

    override val hardDependencies: List<String> = optional("hard-depend") ?: emptyList()
    override val softDependencies: List<String> = optional("soft-depend") ?: emptyList()

    operator fun component1() = name
    operator fun component2() = version
    operator fun component3() = description
    operator fun component4() = author
}