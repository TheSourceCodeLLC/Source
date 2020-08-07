package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class SourceUser internal constructor(
    private val permissionData: PermissionData,
    val id: String,
    val guild: String,
    val permissions: MutableList<SourcePermission> = ArrayList()
) : SimplePermissible(permissions) {
    internal var roles: Set<SourceRole> = emptySet()
    override fun update(): UpdateResult = permissionData.updateUser(this)
    override fun delete(): DeleteResult = permissionData.deleteUser(this)
    override fun asMention() = "<@$id>"

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceUser> {
        override fun queryDocument(obj: SourceUser) = Document("id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["id"] as String
            val guild = it["guild"] as String
            val permissionData = permissionHandler.getData(guild)
            val permissions = permissionData.getPermissions(it)
            SourceUser(permissionData, id, guild, permissions)
        }

        override fun serialize(obj: SourceUser) = queryDocument(obj).apply {
            append("guild", obj.guild)
            val permissions = obj.permissions.map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
        }
    }

    override fun hasPermission(node: String): Boolean {
        if (roles.any { it.hasPermission(node) }) return true
        return super.hasPermission(node)
    }

    override fun hasPermission(node: String, context: String): Boolean {
        if (roles.any { it.hasPermission(node, context) }) return true
        return super.hasPermission(node, context)
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