package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*
import kotlin.collections.ArrayList

class SourceRole internal constructor(
    private val permissionData: PermissionData,
    val id: String,
    val guild: String,
    internal val permissions: MutableList<SourcePermission> = ArrayList(),
    internal val parents: SortedSet<SourceGroup> = TreeSet(Comparator.comparing(SourceGroup::weight))
) : SimplePermissible(permissions, parents) {
    override fun update(): UpdateResult = permissionData.updateRole(this)
    override fun delete(): DeleteResult = permissionData.deleteRole(this)

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceRole> {
        override fun queryDocument(obj: SourceRole) = Document("id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["id"] as String
            val guild = it["guild"] as String
            val permissionData = permissionHandler.getData(guild)
            val permissions = permissionData.getPermissions(it)
            val parents = permissionData.getParents(it)
            SourceRole(permissionData, id, guild, permissions, parents)
        }

        override fun serialize(obj: SourceRole) = queryDocument(obj).apply {
            append("guild", obj.guild)
            val permissions = obj.permissions.map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
            val parents = obj.parents.map(SourceGroup::name)
            append("parents", parents)
        }
    }
}