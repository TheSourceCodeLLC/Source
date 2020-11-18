package net.sourcebot.api.permission

import java.util.*

abstract class SimplePermissible(stored: Set<SourcePermission>) : Permissible {
    private val permissions: MutableSet<SourcePermission> = TreeSet(
        compareByDescending(SourcePermission::node).thenBy {
            it.node.count { it == '.' }
        }
    ).apply { addAll(stored) }

    override fun hasPermission(
        node: String
    ) = getPermissions().find { it.node == node && it.context == null }?.flag

    override fun hasPermission(
        node: String,
        context: String
    ) = getPermissions().find { it.node == node && it.context == context }?.flag

    override fun setPermission(node: String, flag: Boolean) {
        unsetPermission(node)
        permissions.add(SourcePermission(node, flag))
    }

    override fun setPermission(node: String, flag: Boolean, context: String) {
        unsetPermission(node, context)
        permissions.add(SourcePermission(node, flag, context))
    }

    override fun unsetPermission(node: String) {
        permissions.removeIf { it.node == node && it.context == null }
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

    override fun getPermissions(): Set<SourcePermission> = permissions

    override fun getContexts(node: String) = permissions.filter {
        it.node == node && it.context != null && it.flag
    }.mapNotNull(SourcePermission::context).toSet()
}