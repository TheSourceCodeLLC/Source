package net.sourcebot.api.permission

import java.util.*
import kotlin.collections.ArrayList

abstract class PermissionHolderImpl(
    private val permissions: MutableList<SourcePermission> = ArrayList(),
    private val parents: SortedSet<SourceGroup> = TreeSet(Comparator.comparing(SourceGroup::weight))
) : PermissionHolder {
    override fun addParent(sourceGroup: SourceGroup) = parents.add(sourceGroup)
    override fun removeParent(sourceGroup: SourceGroup) = parents.remove(sourceGroup)
    override fun getParents(): Set<SourceGroup> = parents
    override fun clearParents() {
        parents.clear()
    }

    override fun hasPermission(node: String): Boolean {
        val inherited = parents.any { it.hasPermission(node) }
        if (inherited) return true
        return permissions.find { it.node == node && it.context == null }?.flag ?: false
    }

    override fun hasPermission(node: String, context: String): Boolean {
        val inherited = parents.any { it.hasPermission(node, context) }
        if (inherited) return true
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

    override fun getContexts(node: String): Set<String> {
        val contexts = mutableSetOf<String>()
        parents.forEach { parent ->
            parent.permissions.forEach {
                if (it.node == node && it.context != null) contexts.add(it.context)
            }
        }
        permissions.forEach {
            if (it.node == node && it.context != null) contexts.add(it.context)
        }
        return contexts
    }
}