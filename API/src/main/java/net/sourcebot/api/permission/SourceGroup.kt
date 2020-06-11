package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*
import kotlin.collections.ArrayList

class SourceGroup internal constructor(
    private val permissionData: PermissionData,
    val name: String,
    val weight: Int,
    val guild: String,
    internal val permissions: MutableList<SourcePermission> = ArrayList(),
    internal val parents: SortedSet<SourceGroup> = TreeSet(Comparator.comparing(SourceGroup::weight))
) : SimplePermissible(permissions, parents) {
    override fun update(): UpdateResult = permissionData.updateGroup(this)
    override fun delete(): DeleteResult = permissionData.deleteGroup(this)

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceGroup> {
        override fun queryDocument(obj: SourceGroup) = Document("name", obj.name)
        override fun deserialize(document: Document) = document.let {
            val name = it["name"] as String
            val weight = it["weight"] as Int
            val guild = it["guild"] as String
            val permissionData = permissionHandler.getData(guild)
            val permissions = permissionData.getPermissions(it)
            val parents = permissionData.getParents(it)
            SourceGroup(permissionData, name, weight, guild, permissions, parents)
        }

        override fun serialize(obj: SourceGroup) = queryDocument(obj).apply {
            append("guild", obj.guild)
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