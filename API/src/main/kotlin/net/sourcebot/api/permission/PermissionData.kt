package net.sourcebot.api.permission

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.DeleteResult
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*

class PermissionData(mongodb: MongoDatabase) {
    private val users = mongodb.getCollection("user-permissions")
    private val roles = mongodb.getCollection("role-permissions")

    private val userCache = HashMap<String, SourceUser>()
    private val roleCache = HashMap<String, SourceRole>()

    fun getUser(member: Member): SourceUser = userCache.computeIfAbsent(member.id) {
        users.find(Document("_id", member.id)).first()?.let {
            MongoSerial.fromDocument<SourceUser>(it)
        } ?: SourceUser(member.id).also { insert(it, users) }
    }

    internal fun updateUser(sourceUser: SourceUser) = update(sourceUser, users)
    internal fun deleteUser(sourceUser: SourceUser) = delete(sourceUser, users, userCache)

    fun getRole(role: Role): SourceRole = roleCache.computeIfAbsent(role.id) {
        roles.find(Document("_id", role.id)).first()?.let {
            MongoSerial.fromDocument<SourceRole>(it)
        } ?: SourceRole(role.id).also { insert(it, roles) }
    }

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
}