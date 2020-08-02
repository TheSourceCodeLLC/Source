package net.sourcebot.module.tags.data

import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.time.Instant

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