package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*
import kotlin.collections.ArrayList

class SourceGroup internal constructor(
    private val permissionHandler: PermissionHandler,
    val name: String,
    val weight: Int,
    internal val permissions: MutableList<SourcePermission> = ArrayList(),
    internal val parents: SortedSet<SourceGroup> = TreeSet(Comparator.comparing(SourceGroup::weight))
) : PermissionHolderImpl(permissions, parents) {
    override fun update(): UpdateResult = permissionHandler.updateGroup(this)
    override fun delete(): DeleteResult = permissionHandler.deleteGroup(this)

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceGroup> {
        override fun queryDocument(obj: SourceGroup) = Document("name", obj.name)
        override fun deserialize(document: Document) = document.let {
            val name = it["name"] as String
            val weight = it["weight"] as Int
            val permissions = permissionHandler.getPermissions(it)
            val parents = permissionHandler.getParents(it)
            SourceGroup(permissionHandler, name, weight, permissions, parents)
        }

        override fun serialize(obj: SourceGroup) = queryDocument(obj).apply {
            this.append("weight", obj.weight)
            val permissions = obj.permissions.map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
            val parents = obj.parents.map(SourceGroup::name)
            append("parents", parents)
        }
    }
}