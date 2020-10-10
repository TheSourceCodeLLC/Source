package net.sourcebot.api.database

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import net.sourcebot.api.urlEncoded
import org.bson.Document
import java.io.Closeable

class MongoDB(uri: String) : Closeable {
    private val encodingPattern = "://(.+):(.+)@".toRegex()
    private val client = MongoClient(MongoClientURI(
        encodingPattern.replace(uri) {
            val (username, password) = it.destructured
            "://${username.urlEncoded()}:${password.urlEncoded()}@"
        }
    ))

    override fun close() = client.close()

    fun getDatabase(it: String): MongoDatabase = client.getDatabase(it)
    fun getCollection(db: String, name: String): MongoCollection<Document> =
        getDatabase(db).getCollection(name)

    fun getGlobalCollection(collection: String) = getCollection("global", collection)
}