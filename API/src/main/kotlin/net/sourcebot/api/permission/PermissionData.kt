package net.sourcebot.api.permission

import com.google.common.cache.Cache
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.DeleteResult
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.weakCache
import org.bson.Document

class PermissionData(mongodb: MongoDatabase) {
    private val users = mongodb.getCollection("user-permissions")
    private val roles = mongodb.getCollection("role-permissions")

    private val roleCache = weakCache<Role, SourceRole> { role ->
        roles.find(Document("_id", role.id)).first()?.let {
            MongoSerial.fromDocument<SourceRole>(it)
        } ?: SourceRole(role.id).also { insert(it, roles) }
    }
    private val userCache = weakCache<Member, SourceUser> { member ->
        val roles = member.roles.toMutableList().also {
            it.add(member.guild.publicRole)
        }.map(::getRole)
        users.find(Document("_id", member.id)).first()?.let {
            MongoSerial.fromDocument<SourceUser>(it)
        }?.also { it.roles = roles } ?: SourceUser(member.id, roles).also { insert(it, users) }
    }

    fun getUser(member: Member): SourceUser = userCache[member]
    fun getRole(role: Role): SourceRole = roleCache[role]

    internal fun updateUser(sourceUser: SourceUser) = update(sourceUser, users)
    internal fun deleteUser(sourceUser: SourceUser) = delete(sourceUser, users, userCache)
    internal fun updateRole(sourceRole: SourceRole) = update(sourceRole, roles)
    internal fun deleteRole(sourceRole: SourceRole) = delete(sourceRole, roles, roleCache)

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

    private fun <K, V> delete(
        obj: V,
        type: Class<V>,
        collection: MongoCollection<Document>,
        cache: Cache<K, V>
    ): DeleteResult {
        cache.asMap().values.remove(obj)
        return collection.deleteOne(MongoSerial.getQueryDocument(obj, type))
    }

    private inline fun <K, reified V> delete(
        obj: V,
        collection: MongoCollection<Document>,
        cache: Cache<K, V>
    ) = delete(obj, V::class.java, collection, cache)

    fun dropContexts(context: String) {
        dropContexts(users, context).also { userCache.invalidateAll() }
        dropContexts(roles, context).also { roleCache.invalidateAll() }
    }

    private fun dropContexts(collection: MongoCollection<Document>, context: String) {
        collection.updateMany(Document(), Document("\$pull", Document(
            "permissions", Document("context", context)
        )))
    }
}