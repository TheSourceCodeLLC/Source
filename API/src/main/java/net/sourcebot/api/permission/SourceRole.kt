package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*
import kotlin.collections.ArrayList

class SourceRole internal constructor(
    private val permissionHandler: PermissionHandler,
    internal val id: String,
    internal val permissions: MutableList<SourcePermission> = ArrayList(),
    internal val parents: SortedSet<SourceGroup> = TreeSet(Comparator.comparing(SourceGroup::weight))
) : PermissionHolderImpl(permissions, parents) {
    override fun update(): UpdateResult = permissionHandler.updateRole(this)
    override fun delete(): DeleteResult = permissionHandler.deleteRole(this)

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceRole> {
        override fun queryDocument(obj: SourceRole) = Document("id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["id"] as String
            val permissions = permissionHandler.getPermissions(it)
            val parents = permissionHandler.getParents(it)
            SourceRole(permissionHandler, id, permissions, parents)
        }

        override fun serialize(obj: SourceRole) = queryDocument(obj).apply {
            val permissions = obj.permissions.map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
            val parents = obj.parents.map(SourceGroup::name)
            append("parents", parents)
        }
    }
}