package net.sourcebot.module.applications.data

import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.time.Instant

data class ApplicationModel
    (
    val name: String,
    val questions: List<String>,
    val creator: String,
    val cooldown: Long = 0L,
    val created: Long = Instant.now().toEpochMilli()
) {

    class Serial : MongoSerial<ApplicationModel> {
        override fun queryDocument(obj: ApplicationModel) = Document("name", obj.name)

        override fun deserialize(document: Document): ApplicationModel = document.let {
            val name = it["name"] as String
            val questions = it["questions"] as List<String>
            val creator = it["creator"] as String
            val cooldown = it["cooldown"] as Long
            val created = it["created"] as Long

            ApplicationModel(name, questions, creator, cooldown, created)
        }

        override fun serialize(obj: ApplicationModel) = queryDocument(obj).apply {
            append("questions", obj.questions)
            append("creator", obj.creator)
            append("cooldown", obj.cooldown)
            append("created", obj.created)
        }
    }
}

