package net.sourcebot.module.tags

import com.mongodb.client.MongoCollection
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.SourceColor
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.AbstractMessageHandler
import org.bson.Document
import java.time.Instant

class TagHandler(
    private val tags: MongoCollection<Document>,
    prefix: String
) : AbstractMessageHandler(prefix) {
    private val tagCache = HashMap<String, Tag>()

    init {
        tags.find().forEach {
            val tag = MongoSerial.fromDocument<Tag>(it)
            tagCache[tag.name] = tag
        }
    }

    override fun cascade(message: Message, label: String, args: Array<String>) {
        val tag = getTag(label.toLowerCase()) ?: return
        val content = tag.processArguments(args)
        when (tag.type) {
            Tag.Type.TEXT -> message.channel.sendMessage(content).queue()
            Tag.Type.EMBED -> {
                val embed = EmbedBuilder()
                    .setDescription(content)
                    .setColor(SourceColor.INFO.color)
                    .build()
                message.channel.sendMessage(embed).queue()
            }
        }
        tag.uses++
        tags.updateOne(MongoSerial.getQueryDocument(tag), Document("\$set", MongoSerial.toDocument(tag)))
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

    data class Tag(
        val name: String,
        var content: String,
        val creator: String,
        val created: Long = Instant.now().toEpochMilli(),
        var category: String = "Miscellaneous",
        var type: Type = Type.TEXT,
        var uses: Int = 0
    ) {
        //TODO: Argument expansions
        fun processArguments(args: Array<String>): String = content

        class Serial : MongoSerial<Tag> {
            override fun queryDocument(obj: Tag) = Document("name", obj.name)

            override fun deserialize(document: Document): Tag = document.let {
                val name = it["name"] as String
                val content = it["content"] as String
                val creator = it["creator"] as String
                val created = it["created"] as Long
                val category = it["category"] as String
                val type = when (it["type"]) {
                    "embed" -> Type.EMBED
                    else -> Type.TEXT
                }
                val uses = it["uses"] as Int
                Tag(name, content, creator, created, category, type, uses)
            }

            override fun serialize(obj: Tag) = queryDocument(obj).apply {
                append("content", obj.content)
                append("creator", obj.creator)
                append("created", obj.created)
                append("category", obj.category)
                append("type", obj.type.name.toLowerCase())
                append("uses", obj.uses)
            }
        }

        enum class Type { EMBED, TEXT }
    }
}