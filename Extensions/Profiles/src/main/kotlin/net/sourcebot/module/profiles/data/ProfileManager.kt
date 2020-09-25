package net.sourcebot.module.profiles.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.concurrent.TimeUnit

class ProfileManager(
    private val mongo: MongoDB
) {
    private val dataCache = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .removalListener<Member, Profile> {
            val collection = getCollection(it.key.guild)
            val query = MongoSerial.getQueryDocument(it.value)
            val toStore = MongoSerial.toDocument(it.value)
            collection.updateOne(query, Document("\$set", toStore))
        }.build(object : CacheLoader<Member, Profile>() {
            override fun load(
                member: Member
            ): Profile {
                val collection = getCollection(member.guild)
                return collection.find(Document("id", member.id)).first()?.let {
                    MongoSerial.fromDocument<Profile>(it)
                } ?: Profile(member.id).also { collection.insertOne(MongoSerial.toDocument(it)) }
            }
        })

    operator fun get(
        member: Member
    ): Profile = dataCache[member]

    private fun getCollection(guild: Guild) = mongo.getCollection(guild.id, "profiles")
}