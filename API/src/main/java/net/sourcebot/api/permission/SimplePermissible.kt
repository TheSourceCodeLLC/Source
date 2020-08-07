package net.sourcebot.api.permission

abstract class SimplePermissible(
    private val permissions: MutableList<SourcePermission> = ArrayList()
) : Permissible {
    override fun hasPermission(node: String): Boolean {
        return permissions.find { it.node == node && it.context == null }?.flag ?: false
    }

    override fun hasPermission(node: String, context: String): Boolean {
        return permissions.find { it.node == node && it.context == context }?.flag ?: false
    }

    override fun setPermission(node: String, flag: Boolean) {
        unsetPermission(node)
        permissions.add(SourcePermission(node, flag))
    }

    override fun setPermission(node: String, flag: Boolean, context: String) {
        unsetPermission(node, context)
        permissions.add(SourcePermission(node, flag, context))
    }

    override fun unsetPermission(node: String) {
        permissions.removeIf { it.node == node }
    }

    override fun unsetPermission(node: String, context: String) {
        permissions.removeIf { it.node == node && it.context == context }
    }

    override fun clearPermissions() {
        permissions.clear()
    }

    override fun clearPermissions(context: String) {
        permissions.removeIf { it.context == context }
    }

    override fun getPermissions(): Collection<SourcePermission> = permissions

    override fun getContexts(node: String): Set<String> {
        val contexts = mutableSetOf<String>()
        permissions.forEach {
            if (it.node == node && it.context != null) contexts.add(it.context)
        }
        return contexts
    }
}