package net.sourcebot.module.tags.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.SourceColor
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.AbstractMessageHandler
import org.bson.Document
import java.util.concurrent.TimeUnit

class TagHandler(
    private val mongodb: MongoDB,
    prefix: String
) : AbstractMessageHandler(prefix) {
    private val tags = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<Guild, TagCache>() {
            override fun load(guild: Guild) = TagCache(mongodb.getCollection(guild.id, "tags"))
        })

    override fun cascade(message: Message, label: String, args: Array<String>) {
        if (!message.isFromGuild) return
        val tagCache = get(message.guild)
        val tag = tagCache.getTag(label.toLowerCase()) ?: return
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
        tagCache.tags.updateOne(MongoSerial.getQueryDocument(tag), Document("\$set", MongoSerial.toDocument(tag)))
    }

    operator fun get(guild: Guild): TagCache = tags[guild]
}