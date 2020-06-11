package net.sourcebot.api.permission

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class SourceUser internal constructor(
    private val permissionData: PermissionData,
    val id: String,
    val guild: String,
    val permissions: MutableList<SourcePermission> = ArrayList(),
    val parents: SortedSet<SourceGroup> = TreeSet(Comparator.comparing(SourceGroup::weight))
) : SimplePermissible(permissions, parents) {
    internal var roles: Set<SourceRole> = emptySet()
    override fun update(): UpdateResult = permissionData.updateUser(this)
    override fun delete(): DeleteResult = permissionData.deleteUser(this)

    class Serial(private val permissionHandler: PermissionHandler) : MongoSerial<SourceUser> {
        override fun queryDocument(obj: SourceUser) = Document("id", obj.id)
        override fun deserialize(document: Document) = document.let {
            val id = it["id"] as String
            val guild = it["guild"] as String
            val permissionData = permissionHandler.getData(guild)
            val permissions = permissionData.getPermissions(it)
            val parents = permissionData.getParents(it)
            SourceUser(permissionData, id, guild, permissions, parents)
        }

        override fun serialize(obj: SourceUser) = queryDocument(obj).apply {
            append("guild", obj.guild)
            val permissions = obj.permissions.map {
                MongoSerial.toDocument(it)
            }
            append("permissions", permissions)
            val parents = obj.parents.map(SourceGroup::name)
            append("parents", parents)
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