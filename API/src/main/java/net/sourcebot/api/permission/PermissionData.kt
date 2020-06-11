package net.sourcebot.api.permission

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.DeleteResult
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*

class PermissionData(
    mongodb: MongoDatabase,
    private val guild: String
) {
    private val users = mongodb.getCollection("user-permissions")
    private val roles = mongodb.getCollection("role-permissions")
    private val groups = mongodb.getCollection("group-permissions")

    private val userCache = HashMap<String, SourceUser>()
    private val roleCache = HashMap<String, SourceRole>()
    private val groupCache = HashMap<String, SourceGroup>()
    private val defaultGroup = {
        groupCache.computeIfAbsent("default") {
            getGroup(it) ?: SourceGroup(this, it, 0, guild).apply { insert(this, groups) }
        }
    }

    fun getUser(member: Member): SourceUser = userCache.computeIfAbsent(member.id) {
        users.find(Document("id", member.id)).first()?.let {
            MongoSerial.fromDocument<SourceUser>(it)
        } ?: SourceUser(this, member.id, guild).also {
            it.addParent(defaultGroup())
            insert(it, users)
        }
    }

    internal fun updateUser(sourceUser: SourceUser) = update(sourceUser, users)
    internal fun deleteUser(sourceUser: SourceUser) =
        delete(sourceUser, users, userCache)

    fun getRole(role: Role): SourceRole = roleCache.computeIfAbsent(role.id) {
        roles.find(Document("id", role.id)).first()?.let {
            MongoSerial.fromDocument<SourceRole>(it)
        } ?: SourceRole(this, role.id, guild).also {
            it.addParent(defaultGroup())
            insert(it, roles)
        }
    }

    internal fun updateRole(sourceRole: SourceRole) = update(sourceRole, roles)
    internal fun deleteRole(sourceRole: SourceRole) =
        delete(sourceRole, roles, roleCache)

    fun getGroup(name: String): SourceGroup? {
        val cached = groupCache[name]
        return if (cached != null) cached else {
            val found = groups.find(Document("name", name)).first()
            if (found != null) MongoSerial.fromDocument<SourceGroup>(found) else null
        }
    }

    fun getGroups(): Collection<SourceGroup> = groupCache.values

    fun createGroup(name: String, weight: Int): Boolean {
        val cached = groupCache[name]
        return if (cached != null) false
        else {
            val created = SourceGroup(this, name, weight, guild)
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
        return delete(sourceGroup, groups, groupCache)
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
        cache: HashMap<String, T>
    ): DeleteResult {
        cache.values.remove(obj)
        return collection.deleteOne(MongoSerial.getQueryDocument(obj, type))
    }

    private inline fun <reified T> delete(
        obj: T,
        collection: MongoCollection<Document>,
        cache: HashMap<String, T>
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