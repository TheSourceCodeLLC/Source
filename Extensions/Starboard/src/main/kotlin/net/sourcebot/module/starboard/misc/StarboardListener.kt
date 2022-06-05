package net.sourcebot.module.starboard.misc

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.sourcebot.Source
import net.sourcebot.api.configuration.config
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.module.starboard.Starboard
import org.bson.Document

class StarboardListener : EventSubscriber<Starboard> {
    private val mongo = Source.MONGODB

    companion object {
        const val UNICODE_STAR = "\u2B50"
    }

    override fun subscribe(
        module: Starboard,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onReactionAdd)
        jdaEvents.listen(module, this::onReactionRemove)
        jdaEvents.listen(module, this::onMessageDelete)
        jdaEvents.listen(module, this::onMessageEdit)
    }

    private fun onReactionAdd(event: GuildMessageReactionAddEvent) {
        listenReaction(event) { message ->
            if (event.user == message.author)
                return@listenReaction event.reaction.removeReaction(message.author).queue()
            val data = Starboard::class.config(message.guild)
            val threshold = data.required<Long>("threshold")
            val count = message.reactions.find { it.reactionEmote.name == UNICODE_STAR }?.count ?: 0
            if (count < threshold) return@listenReaction
            val channel = data.optional<String>(getChannelKey(event.channel))?.let {
                event.guild.getTextChannelById(it)
            } ?: return@listenReaction
            val linkObject = getLinkObject(event.guild, message.id)
            if (linkObject != null) {
                val starredId = linkObject["starred"] as String
                channel.retrieveMessageById(starredId).queue {
                    it.editMessage(StarboardResponse.fromMessage(message, count)).queue()
                }
            } else {
                channel.sendMessage(StarboardResponse.fromMessage(message, count)).queue {
                    getCollection(event.guild).insertOne(
                        Document(
                            mapOf(
                                "original" to message.id,
                                "starred" to it.id
                            )
                        )
                    )
                }
            }
        }
    }

    private fun onReactionRemove(event: GuildMessageReactionRemoveEvent) {
        listenReaction(event) { message ->
            val data = Starboard::class.config(message.guild)
            val linkObject = getLinkObject(event.guild, message.id)
            val starredId = linkObject?.get("starred") as String? ?: return@listenReaction
            val count = message.reactions.find { it.reactionEmote.name == UNICODE_STAR }?.count ?: 0
            val channel = data.optional<String>(getChannelKey(event.channel))?.let {
                event.guild.getTextChannelById(it)
            } ?: return@listenReaction
            if (count < data.required<Long>("threshold")) {
                channel.deleteMessageById(starredId).queue({}, {})
                getCollection(event.guild).deleteOne(linkObject!!)
            } else {
                channel.retrieveMessageById(starredId).queue {
                    it.editMessage(StarboardResponse.fromMessage(message, count)).queue()
                }
            }
        }
    }

    private fun listenReaction(
        event: GenericGuildMessageReactionEvent,
        consumer: (Message) -> Unit
    ) {
        val data = Starboard::class.config(event.guild)
        if (data.optional<List<String>>("excluded-channels")?.contains(event.channel.id) == true) return
        if (event.reactionEmote.name != UNICODE_STAR) return
        event.retrieveMessage().queue { message ->
            if (message.author.isBot) return@queue
            consumer(message)
        }
    }

    private fun onMessageDelete(event: GuildMessageDeleteEvent) {
        val collection = getCollection(event.guild)
        val data = Starboard::class.config(event.guild)
        if (event.channel.id == data.optional<String>(getChannelKey(event.channel))) {
            collection.findOneAndDelete(Document("starred", event.messageId))
        } else deleteStarredMessage(event.guild, event.messageId, getChannelKey(event.channel))
    }

    private fun onMessageEdit(event: GuildMessageUpdateEvent) {
        getLinkObject(event.guild, event.messageId) ?: return
        event.message.clearReactions(UNICODE_STAR).queue()
        deleteStarredMessage(event.guild, event.messageId, getChannelKey(event.channel))
    }

    private fun deleteStarredMessage(guild: Guild, original: String, channelKey: String) {
        val collection = getCollection(guild)
        val deleted = collection.findOneAndDelete(Document("original", original))
        val starred = deleted?.get("starred") as String? ?: return
        val data = Starboard::class.config(guild)
        val channel = data.optional<String>(channelKey)?.let {
            guild.getTextChannelById(it)
        } ?: return
        channel.deleteMessageById(starred).queue()
    }

    private fun getLinkObject(
        guild: Guild,
        original: String
    ) = getCollection(guild).find(Document("original", original)).first()

    private fun getChannelKey(channel: TextChannel): String {
        val data = Starboard::class.config(channel.guild)
        return if (channel.isNSFW && !data.optional<String>("nsfw-channel")
                .isNullOrBlank()
        ) "nsfw-channel" else "channel"
    }

    private fun getCollection(guild: Guild) = mongo.getCollection(guild.id, "starboard")

    private class StarboardResponse(
        original: Message
    ) : StandardWarningResponse(
        "%#s".format(original.author),
        "${
            if (original.contentRaw.isBlank()) "${original.attachments[0].fileName}:"
            else "\"${original.contentRaw}\""
        } [[Jump](${original.jumpUrl})]"
    ) {
        companion object {
            @JvmStatic
            fun fromMessage(original: Message, count: Int): Message {
                val builder = MessageBuilder()
                builder.append("$UNICODE_STAR $count: ${(original.channel as TextChannel).asMention}")
                builder.setEmbeds(StarboardResponse(original).asEmbed(original.author))
                return builder.build()
            }
        }

        init {
            val attachment = original.attachments.firstOrNull()
            if (attachment != null) {
                if (attachment.isImage) setImage(attachment.proxyUrl)
                else addField("Attached File:", "[${attachment.fileName}](${attachment.proxyUrl})", false)
            }
        }
    }
}
