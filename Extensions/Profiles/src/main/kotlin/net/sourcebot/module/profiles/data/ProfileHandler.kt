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
            ) = collection.find(Document("_id", key)).first()?.let(::JsonConfiguration) ?: JsonConfiguration().also {
                collection.insertOne(Document(mapOf("_id" to key, "data" to it)))
            }
        })

    operator fun get(id: String): JsonConfiguration = cache[id]

    fun save(id: String, profile: JsonConfiguration) {
        val toStore = Document("data", MongoSerial.toDocument(profile))
        collection.updateOne(Document("_id", id), Document("\$set", toStore))
    }

    fun saveAll() = cache.invalidateAll()
}