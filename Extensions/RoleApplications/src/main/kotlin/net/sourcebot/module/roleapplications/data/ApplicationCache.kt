package net.sourcebot.module.roleapplications.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mongodb.client.MongoCollection
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.concurrent.TimeUnit

class ApplicationCache(
    private val applications: MongoCollection<Document>
) {

    private val appCache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, ApplicationModel>() {
            override fun load(
                name: String
            ): ApplicationModel =
                applications.find(Document("name", name)).first()!!.let { MongoSerial.fromDocument(it) }
        })

    fun getApplication(name: String): ApplicationModel? {
        return try {
            appCache[name]
        } catch (ex: Exception) {
            null
        }
    }

    fun getApplications(): List<ApplicationModel> {
        return applications.find()
            .map { MongoSerial.fromDocument<ApplicationModel>(it) }
            .filterNotNull()
    }

    fun createApplication(name: String, questions: List<String>, creator: String) {
        val appModel = ApplicationModel(name, questions, creator)
        appCache.put(name, appModel)
        applications.insertOne(MongoSerial.toDocument(appModel))
    }

    fun deleteApplication(name: String) {
        appCache.invalidate(name)
        applications.deleteOne(Document("name", name))
    }

    fun saveApplication(appModel: ApplicationModel) {
        applications.updateOne(
            MongoSerial.getQueryDocument(appModel),
            Document("\$set", MongoSerial.toDocument(appModel))
        )
    }


}