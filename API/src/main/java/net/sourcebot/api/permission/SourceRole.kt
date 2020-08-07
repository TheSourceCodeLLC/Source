package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class SourceRole internal constructor(
    private val permissionData: PermissionData,
    val id: String,
    val guild: String,
    internal val permissions: MutableList<SourcePermission> = ArrayList()
) : SimplePermissible(permissions) {
    override fun update(): UpdateResult = permissionData.updateRole(this)
    override fun delete(): DeleteResult = permissionData.deleteRole(this)
    override fun asMention() = "<@&$id>"

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceRole> {
        override fun queryDocument(obj: SourceRole) = Document("id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["id"] as String
            val guild = it["guild"] as String
            val permissionData = permissionHandler.getData(guild)
            val permissions = permissionData.getPermissions(it)
            SourceRole(permissionData, id, guild, permissions)
        }

        override fun serialize(obj: SourceRole) = queryDocument(obj).apply {
            append("guild", obj.guild)
            val permissions = obj.permissions.map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
        }
    }
}