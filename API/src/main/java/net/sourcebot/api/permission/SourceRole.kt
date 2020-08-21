package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class SourceRole internal constructor(
    val id: String,
    internal val permissions: MutableList<SourcePermission> = ArrayList()
) : SimplePermissible(permissions) {
    override fun update(data: PermissionData): UpdateResult = data.updateRole(this)
    override fun delete(data: PermissionData): DeleteResult = data.deleteRole(this)
    override fun asMention() = "<@&$id>"

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceRole> {
        override fun queryDocument(obj: SourceRole) = Document("id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["id"] as String
            val permissions = permissionHandler.getPermissions(it)
            SourceRole(id, permissions)
        }

        override fun serialize(obj: SourceRole) = queryDocument(obj).apply {
            val permissions = obj.permissions.map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
        }
    }
}