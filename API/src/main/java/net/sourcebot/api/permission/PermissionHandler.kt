@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")

package net.sourcebot.api.permission

import com.mongodb.client.MongoCollection
import com.mongodb.client.result.DeleteResult
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*
import java.util.Map

class PermissionHandler(mongodb: MongoDB) {
    private val users = mongodb.getCollection<Document>("user-permissions")
    private val roles = mongodb.getCollection<Document>("role-permissions")
    private val groups = mongodb.getCollection<Document>("group-permissions")

    private val userCache = IdentityHashMap<Member, SourceUser>()
    private val roleCache = IdentityHashMap<Role, SourceRole>()
    private val groupCache = HashMap<String, SourceGroup>()
    private val defaultGroup = {
        groupCache.computeIfAbsent("default") {
            getGroup(it) ?: SourceGroup(this, it, 0).apply { insert(this, groups) }
        }
    }

    fun getUser(member: Member): SourceUser {
        val cached = userCache[member]
        return if (cached != null) {
            cached.roles = member.roles.map(this::getRole).toSet()
            cached
        } else userCache.computeIfAbsent(member) {
            users.find(Document("id", member.id)).first()?.let {
                MongoSerial.fromDocument<SourceUser>(it)
            } ?: SourceUser(this, member.id).also {
                it.addParent(defaultGroup())
                insert(it, users)
            }
        }
    }

    internal fun updateUser(sourceUser: SourceUser) = update(sourceUser, users)
    internal fun deleteUser(sourceUser: SourceUser) =
        delete(sourceUser, users, userCache as Map<*, SourceUser>)

    fun getRole(role: Role): SourceRole = roleCache.computeIfAbsent(role) {
        roles.find(Document("id", role.id)).first()?.let {
            MongoSerial.fromDocument<SourceRole>(it)
        } ?: SourceRole(this, role.id).also {
            it.addParent(defaultGroup())
            insert(it, roles)
        }
    }

    fun createRole(role: Role): Boolean {
        val cached = roleCache[role]
        return if (cached != null) false else {
            val created = SourceRole(this, role.id)
            roleCache[role] = created
            insert(role, roles)
            true
        }
    }

    internal fun updateRole(sourceRole: SourceRole) = update(sourceRole, roles)
    internal fun deleteRole(sourceRole: SourceRole) =
        delete(sourceRole, roles, roleCache as Map<*, SourceRole>)

    fun getGroup(name: String): SourceGroup? {
        val cached = groupCache[name]
        return if (cached != null) cached else {
            val found = groups.find(Document("name", name)).first()
            if (found != null) MongoSerial.fromDocument<SourceGroup>(found) else null
        }
    }

    fun createGroup(name: String, weight: Int): Boolean {
        val cached = groupCache[name]
        return if (cached != null) false
        else {
            val created = SourceGroup(this, name, weight)
            groupCache[name] = created
            insert(created, groups)
            true
        }
    }

    internal fun updateGroup(sourceGroup: SourceGroup) = update(sourceGroup, groups)
    internal fun deleteGroup(sourceGroup: SourceGroup): DeleteResult {
        userCache.values.forEach {
            if (it.removeParent(sourceGroup)) it.update()
        }
        roleCache.values.forEach {
            if (it.removeParent(sourceGroup)) it.update()
        }
        groupCache.values.forEach {
            if (it.removeParent(sourceGroup)) it.update()
        }
        return delete(sourceGroup, groups, groupCache as Map<*, SourceGroup>)
    }

    private fun <T> insert(
        obj: T,
        type: Class<T>,
        collection: MongoCollection<Document>
    ) = collection.insertOne(MongoSerial.toDocument(obj, type))

    private inline fun <reified T> insert(obj: T, collection: MongoCollection<Document>) =
        insert(obj, T::class.java, collection)

    private fun <T> update(obj: T, type: Class<T>, collection: MongoCollection<Document>) = collection.updateOne(
        MongoSerial.getQueryDocument(obj, type),
        Document("\$set", MongoSerial.toDocument(obj, type))
    )

    private inline fun <reified T> update(
        obj: T,
        collection: MongoCollection<Document>
    ) = update(obj, T::class.java, collection)

    private fun <T> delete(
        obj: T,
        type: Class<T>,
        collection: MongoCollection<Document>,
        cache: Map<*, T>
    ): DeleteResult {
        cache.values().remove(obj)
        return collection.deleteOne(MongoSerial.getQueryDocument(obj, type))
    }

    private inline fun <reified T> delete(
        obj: T,
        collection: MongoCollection<Document>,
        cache: Map<*, T>
    ) = delete(obj, T::class.java, collection, cache)

    internal fun getParents(
        document: Document
    ): SortedSet<SourceGroup> = document.getList("parents", String::class.java)
        .mapNotNull(::getGroup)
        .toSortedSet(Comparator.comparing(SourceGroup::weight))

    internal fun getPermissions(
        document: Document
    ): MutableList<SourcePermission> = document.getList("permissions", Document::class.java)
        .map { MongoSerial.fromDocument<SourcePermission>(it) }
        .toMutableList()
}