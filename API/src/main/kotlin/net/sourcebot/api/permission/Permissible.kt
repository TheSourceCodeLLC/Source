package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult

interface Permissible {
    fun hasPermission(node: String): Boolean?
    fun hasPermission(node: String, context: String): Boolean?

    fun setPermission(node: String, flag: Boolean)
    fun setPermission(node: String, flag: Boolean, context: String)

    fun unsetPermission(node: String)
    fun unsetPermission(node: String, context: String)

    fun clearPermissions()
    fun clearPermissions(context: String)

    fun getPermissions(): Collection<SourcePermission>
    fun getContexts(node: String): Set<String>

    fun update(data: PermissionData): UpdateResult
    fun delete(data: PermissionData): DeleteResult

    fun asMention(): String
}