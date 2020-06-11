package net.sourcebot.api.database

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import net.sourcebot.api.properties.Properties
import net.sourcebot.api.urlEncoded
import org.bson.Document
import java.io.Closeable

class MongoDB(config: Properties) : Closeable {
    private val host: String = config.required("host")
    private val port: Long = config.required("port")
    private val username = config.required<String>("username").urlEncoded()
    private val password = config.required<String>("password").urlEncoded()
    private val authSource: String = config.optional("auth-source") ?: "admin"
    private val uri = "mongodb://$username:$password@$host:$port/?authSource=$authSource"
    private val client = MongoClient(MongoClientURI(uri))

    override fun close() = client.close()

    fun getDatabase(it: String): MongoDatabase = client.getDatabase(it)
    fun getCollection(db: String, name: String): MongoCollection<Document> =
        getDatabase(db).getCollection(name)
}