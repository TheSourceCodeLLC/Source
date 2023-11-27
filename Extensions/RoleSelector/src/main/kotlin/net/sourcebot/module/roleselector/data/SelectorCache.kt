package net.sourcebot.module.roleselector.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mongodb.client.MongoCollection
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.concurrent.TimeUnit

class SelectorCache(
    internal val selectors: MongoCollection<Document>
) {

    private val selectorCache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Selector>() {
            override fun load(
                name: String
            ): Selector = selectors.find(Document("name", name)).first()!!.let { MongoSerial.fromDocument(it) }
        })

    operator fun get(name: String): Selector? = try {
        selectorCache[name]
    } catch (ex: Exception) {
        null
    }

    fun verifyRoles(guild: Guild, selector: Selector) {
        selector.roleIds.removeIf {
            try {
                return@removeIf guild.getRoleById(it) == null
            } catch (ex: Exception) {
                return@removeIf true
            }
        }
        saveSelector(selector)
    }

    fun retrieveMessages(guild: Guild, selector: Selector): MutableList<Message> {
        val msgList: MutableList<Message> = mutableListOf()
        selector.messageIds.entries.removeIf { (channelId, messageIdList) ->
            val channel = guild.getTextChannelById(channelId) ?: return@removeIf true

            messageIdList.removeIf {
                try {
                    msgList.add(channel.retrieveMessageById(it).complete())
                    false
                } catch (ex: Exception) {
                    true
                }
            }

            return@removeIf false
        }

        saveSelector(selector)
        return msgList
    }

    fun getSelectors(): Collection<Selector> = selectors.find().map {
        MongoSerial.fromDocument<Selector>(it)
    }.filterNotNull()

    fun createSelector(name: String, roleIds: MutableList<String>) {
        val selector = Selector(name, roleIds)
        selectorCache.put(name, selector)
        selectors.insertOne(MongoSerial.toDocument(selector))
    }

    fun deleteSelector(name: String) {
        selectorCache.invalidate(name)
        selectors.deleteOne(Document("name", name))
    }

    fun saveSelector(tag: Selector) {
        selectors.updateOne(
            MongoSerial.getQueryDocument(tag),
            Document("\$set", MongoSerial.toDocument(tag))
        )
    }

}