package net.sourcebot.module.profiles.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.concurrent.TimeUnit

internal class ProfileHandler(guild: Guild) {
    private val collection = Source.MONGODB.getCollection(guild.id, "profiles")
    private val cache = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .removalListener<String, JsonConfiguration> { (member, profile) -> save(member!!, profile!!) }
        .build(object : CacheLoader<String, JsonConfiguration>() {
            override fun load(
                id: String
            ): JsonConfiguration {
                val data = collection.find(
                    Document("_id", id)
                ).first()?.get("data") as Document? ?: HashMap()
                return object : JsonConfiguration(data) {
                    override fun onChange() = save(id, this)
                }
            }
        })

    operator fun get(member: Member): JsonConfiguration = cache[member.id]
    operator fun get(id: String): JsonConfiguration = cache[id]

    private fun save(id: String, profile: JsonConfiguration) {
        val toStore = Document("data", MongoSerial.toDocument(profile))
        collection.updateOne(Document("_id", id), Document("\$set", toStore))
    }

    fun saveAll() = cache.invalidateAll()
}