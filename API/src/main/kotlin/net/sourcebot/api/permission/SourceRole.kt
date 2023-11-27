package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class SourceRole internal constructor(
    val id: String, stored: Set<SourcePermission> = emptySet()
) : SimplePermissible(stored) {
    override fun update(data: PermissionData): UpdateResult = data.updateRole(this)
    override fun delete(data: PermissionData): DeleteResult = data.deleteRole(this)
    override fun asMention() = "<@&$id>"

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceRole> {
        override fun queryDocument(obj: SourceRole) = Document("_id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["_id"] as String
            val permissions = permissionHandler.getPermissions(it)
            SourceRole(id, permissions)
        }

        override fun serialize(obj: SourceRole) = queryDocument(obj).apply {
            val permissions = obj.getPermissions().map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
        }
    }
}