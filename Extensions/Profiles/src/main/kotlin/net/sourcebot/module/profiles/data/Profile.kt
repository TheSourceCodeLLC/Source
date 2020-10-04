package net.sourcebot.module.profiles.data

import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class Profile : JsonConfiguration {
    private val id: String

    constructor(map: Map<String, Any?>) : super(map) {
        this.id = map["id"] as String
    }

    constructor(id: String) : this(mapOf("id" to id))

    class Serial : MongoSerial<Profile> {
        override fun deserialize(document: Document) = Profile(document)
        override fun serialize(obj: Profile) = Document(obj.asMap())
        override fun queryDocument(obj: Profile) = Document("id", obj.id)
    }

    companion object {
        init {
            MongoSerial.register(Serial())
        }
    }
}