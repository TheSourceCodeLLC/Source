package net.sourcebot.module.tags.data

import com.mongodb.client.MongoCollection
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class TagCache(
    internal val tags: MongoCollection<Document>
) {
    private val tagCache = HashMap<String, Tag>()

    init {
        tags.find().forEach {
            val tag = MongoSerial.fromDocument<Tag>(it)
            tagCache[tag.name] = tag
        }
    }

    fun getTag(name: String): Tag? = tagCache.compute(name) { _, cached ->
        cached ?: tags.find(Document("name", name)).first()?.let {
            MongoSerial.fromDocument<Tag>(it)
        }
    }

    fun getTags(): Collection<Tag> = tagCache.values

    fun createTag(name: String, content: String, creator: String) {
        val tag = Tag(name, content, creator)
        tagCache[name] = tag
        tags.insertOne(MongoSerial.toDocument(tag))
    }

    fun deleteTag(name: String) {
        val removed = tagCache.remove(name) ?: return
        tags.deleteOne(MongoSerial.getQueryDocument(removed))
    }
}