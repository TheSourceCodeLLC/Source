package net.sourcebot.api.database

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import net.sourcebot.api.properties.Properties
import net.sourcebot.api.urlEncoded
import java.io.Closeable

class MongoDB(config: Properties) : Closeable {
    private val host: String = config.required("host")
    private val port: Long = config.required("port")
    private val databaseName: String = config.required("database")
    private val username = config.required<String>("username").urlEncoded()
    private val password = config.required<String>("password").urlEncoded()
    private val uri = "mongodb://$username:$password@$host:$port"
    private val client = MongoClient(MongoClientURI(uri))
    val database: MongoDatabase = client.getDatabase(databaseName)

    override fun close() = client.close()

    fun <T> getCollection(name: String, type: Class<T>): MongoCollection<T> = database.getCollection(name, type)
    inline fun <reified T> getCollection(name: String): MongoCollection<T> = getCollection(name, T::class.java)

}