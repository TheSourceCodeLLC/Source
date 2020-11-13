package net.sourcebot.module.profiles.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mongodb.client.MongoCollection
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.concurrent.TimeUnit

internal class ProfileHandler(private val collection: MongoCollection<Document>) {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .removalListener<String, JsonConfiguration> { (id, profile) -> save(id, profile) }
        .build(object : CacheLoader<String, JsonConfiguration>() {
            override fun load(
                key: String
            ): JsonConfiguration {
                val query = Document("_id", key)
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
                    Document("_id", key).append("data", MongoSerial.toDocument(profile))
                )
                return profile
            }
        })

    operator fun get(id: String): JsonConfiguration = cache[id]

    fun save(id: String, profile: JsonConfiguration) {
        val toStore = Document("data", MongoSerial.toDocument(profile))
        collection.updateOne(Document("_id", id), Document("\$set", toStore))
    }

    fun saveAll() = cache.invalidateAll()
}