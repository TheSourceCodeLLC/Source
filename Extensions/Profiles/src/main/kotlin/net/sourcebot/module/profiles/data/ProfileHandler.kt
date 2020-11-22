package net.sourcebot.module.profiles.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mongodb.client.MongoCollection
import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.concurrent.TimeUnit

internal class ProfileHandler(private val collection: MongoCollection<Document>) {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES).weakKeys()
        .removalListener<Member, JsonConfiguration> { (member, profile) -> save(member, profile) }
        .build(object : CacheLoader<Member, JsonConfiguration>() {
            override fun load(
                key: Member
            ): JsonConfiguration {
                val query = Document("_id", key.id)
                val found = collection.find(query).first()?.get("data") as Document?
                var insert = false
                val data = if (found != null) found else {
                    insert = true
                    HashMap()
                }
                val profile: JsonConfiguration = object : JsonConfiguration(data) {
                    override fun onChange() {
                        collection.updateOne(query, Document("\$set", Document().also {
                            it["data"] = MongoSerial.toDocument<JsonConfiguration>(this)
                        }))
                    }
                }
                if (insert) collection.insertOne(
                    Document("_id", key.id).append("data", MongoSerial.toDocument(profile))
                )
                return profile
            }
        })

    operator fun get(member: Member): JsonConfiguration = cache[member]

    private fun save(member: Member, profile: JsonConfiguration) {
        val toStore = Document("data", MongoSerial.toDocument(profile))
        collection.updateOne(Document("_id", member.id), Document("\$set", toStore))
    }

    fun saveAll() = cache.invalidateAll()
}