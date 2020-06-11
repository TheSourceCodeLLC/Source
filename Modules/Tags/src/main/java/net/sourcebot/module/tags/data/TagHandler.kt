package net.sourcebot.module.tags.data

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.SourceColor
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.AbstractMessageHandler
import org.bson.Document

class TagHandler(
    private val mongodb: MongoDB,
    prefix: String
) : AbstractMessageHandler(prefix) {
    private val tags = HashMap<String, TagCache>()

    override fun cascade(message: Message, label: String, args: Array<String>) {
        val tagCache = getCache(message.guild)
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

    fun getCache(guild: Guild) = tags.computeIfAbsent(guild.id) {
        TagCache(mongodb.getCollection(it, "tags"))
    }
}