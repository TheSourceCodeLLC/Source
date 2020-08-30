package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class SourceUser internal constructor(
    val id: String,
    val permissions: MutableList<SourcePermission> = ArrayList()
) : SimplePermissible(permissions) {
    internal var roles: Set<SourceRole> = emptySet()
    override fun update(data: PermissionData): UpdateResult = data.updateUser(this)
    override fun delete(data: PermissionData): DeleteResult = data.deleteUser(this)
    override fun asMention() = "<@$id>"

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceUser> {
        override fun queryDocument(obj: SourceUser) = Document("id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["id"] as String
            val permissions = permissionHandler.getPermissions(it)
            SourceUser(id, permissions)
        }

        override fun serialize(obj: SourceUser) = queryDocument(obj).apply {
            val permissions = obj.permissions.map { MongoSerial.toDocument(it) }
            append("permissions", permissions)
        }
    }

    override fun hasPermission(node: String): Boolean? {
        when (super.hasPermission(node)) {
            true -> return true
            false -> return false
        }
        for (it in roles) return it.hasPermission(node) ?: continue
        return null
    }

    override fun hasPermission(node: String, context: String): Boolean? {
        when (super.hasPermission(node, context)) {
            true -> return true
            false -> return false
        }
        for (it in roles) return it.hasPermission(node, context) ?: continue
        return null
    }

    override fun getContexts(node: String): Set<String> {
        val forRoles = roles.flatMap { it.getContexts(node) }
        val forSelf = super.getContexts(node)
        val set = HashSet<String>()
        return set.apply {
            addAll(forRoles)
            addAll(forSelf)
        }
    }
}