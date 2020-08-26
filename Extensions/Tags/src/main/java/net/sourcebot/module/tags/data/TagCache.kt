package net.sourcebot.module.tags.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mongodb.client.MongoCollection
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.concurrent.TimeUnit

class TagCache(
    internal val tags: MongoCollection<Document>
) {
    private val tagCache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Tag>() {
            override fun load(
                name: String
            ): Tag = tags.find(Document("name", name)).first()!!.let { MongoSerial.fromDocument(it) }
        })

    fun getTag(
        name: String
    ): Tag? = try {
        tagCache[name]
    } catch (ex: Exception) {
        null
    }

    fun getTags(): Collection<Tag> = tagCache.asMap().values

    fun createTag(name: String, content: String, creator: String) {
        val tag = Tag(name, content, creator)
        tagCache.put(name, tag)
        tags.insertOne(MongoSerial.toDocument(tag))
    }

    fun deleteTag(name: String) {
        tagCache.invalidate(name)
        tags.deleteOne(Document("name", name))
    }

    fun saveTag(tag: Tag) {
        tags.updateOne(MongoSerial.getQueryDocument(tag), MongoSerial.toDocument(tag))
    }
}