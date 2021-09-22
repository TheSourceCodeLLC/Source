package net.sourcebot.module.tags.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.sourcebot.Source
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.AbstractMessageHandler
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.SourceColor
import net.sourcebot.module.tags.Tags
import org.bson.Document
import java.util.concurrent.TimeUnit

class TagHandler(private val defaultPrefix: String) : AbstractMessageHandler(), EventSubscriber<Tags> {
    private val configManager = Source.CONFIG_MANAGER
    private val mongodb = Source.MONGODB

    private val tags = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<Guild, TagCache>() {
            override fun load(guild: Guild) = TagCache(mongodb.getCollection(guild.id, "tags"))
        })

    override fun cascade(message: Message, label: String, arguments: Arguments) {
        if (!message.isFromGuild) return
        val tagCache = get(message.guild)
        val tag = tagCache.getTag(label.toLowerCase()) ?: return
        val content = tag.processArguments(arguments.slice().raw)
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

    override fun getPrefix(
        event: MessageReceivedEvent
    ) = if (event.isFromGuild) getPrefix(event.guild) else defaultPrefix

    private fun getPrefix(
        guild: Guild
    ) = configManager[guild].required("tags.prefix") { defaultPrefix }

    operator fun get(guild: Guild): TagCache = tags[guild]
    override fun subscribe(
        module: Tags,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMessageReceived)
    }
}