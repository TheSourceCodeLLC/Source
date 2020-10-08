package net.sourcebot.api.configuration

class ConfigurationInfo(
    namespace: String,
    init: ParentNode.() -> Unit
) : ParentNode() {
    override val fullName = namespace

    init {
        this.init()
    }
}

abstract class ParentNode internal constructor() : ConfigurationNode() {
    private val children = ArrayList<ConfigurationNode>()
    val resolved: Map<String, String> by lazy {
        val map = LinkedHashMap<String, String>()
        children.forEach {
            when (it) {
                is ParentNode -> map += it.resolved
                is ChildNode -> map[it.fullName] = it.description
            }
        }
        map
    }

    fun section(
        name: String,
        init: ParentNode.() -> Unit
    ) = registerNode(SectionNode(name).also(init))

    fun node(
        name: String, description: String
    ) = registerNode(ChildNode(name, description))

    private fun registerNode(node: ConfigurationNode) {
        children += node
    }

    inner class SectionNode internal constructor(
        namespace: String
    ) : ParentNode() {
        override val fullName = "${this@ParentNode.fullName}.$namespace"
    }

    inner class ChildNode internal constructor(
        namespace: String,
        val description: String
    ) : ConfigurationNode() {
        override val fullName = "${this@ParentNode.fullName}.$namespace"
    }
}

abstract class ConfigurationNode internal constructor() {
    abstract val fullName: String
}