package net.sourcebot.api.configuration

class ConfigurationInfo(
    namespace: String,
    init: ConfigurationNode.() -> Unit
) : ConfigurationNode() {
    override val fullName = namespace

    init {
        this.init()
    }
}

abstract class ConfigurationNode internal constructor() {
    private val children = ArrayList<ConfigurationNode>()
    val resolved: List<Pair<String, String>> by lazy {
        children.filterIsInstance<ChildNode>().map { it.fullName to it.description }
    }
    abstract val fullName: String

    fun section(
        name: String,
        init: ConfigurationNode.() -> Unit
    ) = registerNode(SectionNode(name).also(init))

    fun node(
        name: String, description: String
    ) = registerNode(ChildNode(name, description))

    private fun registerNode(node: ConfigurationNode) {
        children += node
    }

    inner class SectionNode internal constructor(
        namespace: String
    ) : ConfigurationNode() {
        override val fullName = "${this@ConfigurationNode.fullName}.$namespace"
    }

    inner class ChildNode internal constructor(
        namespace: String,
        val description: String
    ) : ConfigurationNode() {
        override val fullName = "${this@ConfigurationNode.fullName}.$namespace"
    }
}