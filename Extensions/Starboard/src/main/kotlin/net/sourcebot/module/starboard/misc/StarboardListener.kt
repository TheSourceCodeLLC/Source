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
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.module.starboard.Starboard
import org.bson.Document

class StarboardListener(
    private val dataManager: StarboardDataManager
) : EventSubscriber<Starboard> {
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
        val message = listenReaction(event) ?: return
        if (event.user == message.author) return event.reaction.removeReaction(message.author).queue()
        val data = dataManager[event.guild]
        val threshold = data.required<Long>("threshold")
        val count = message.reactions.find { it.reactionEmote.name == UNICODE_STAR }?.count ?: 0
        if (count < threshold) return
        val channel = data.optional<String>("channel")?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        val linkObject = getLinkObject(event.guild, message.id)
        if (linkObject != null) {
            val starredId = linkObject["starred"] as String
            val starred = channel.retrieveMessageById(starredId).complete()
            starred.editMessage(StarboardResponse.fromMessage(message, count)).queue()
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

    private fun onReactionRemove(event: GuildMessageReactionRemoveEvent) {
        val message = listenReaction(event) ?: return
        val data = dataManager[event.guild]
        val linkObject = getLinkObject(event.guild, message.id)
        val starredId = linkObject?.get("starred") as String? ?: return
        val count = message.reactions.find { it.reactionEmote.name == UNICODE_STAR }?.count ?: 0
        val channel = data.optional<String>("channel")?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (count < data.required<Long>("threshold")) {
            channel.deleteMessageById(starredId).queue({}, {})
            getCollection(event.guild).deleteOne(linkObject!!)
        } else {
            val starred = channel.retrieveMessageById(starredId).complete()
            starred.editMessage(StarboardResponse.fromMessage(message, count)).queue()
        }
    }

    private fun listenReaction(event: GenericGuildMessageReactionEvent): Message? {
        if (event.reactionEmote.name != UNICODE_STAR) return null
        val message = event.retrieveMessage().complete()
        if (message.author.isBot) return null
        return message
    }

    private fun onMessageDelete(event: GuildMessageDeleteEvent) {
        val collection = getCollection(event.guild)
        val data = dataManager[event.guild]
        if (event.channel.id == data.optional<String>("channel")) {
            collection.findOneAndDelete(Document("starred", event.messageId))
        } else deleteStarredMessage(event.guild, event.messageId)
    }

    private fun onMessageEdit(event: GuildMessageUpdateEvent) {
        getLinkObject(event.guild, event.messageId) ?: return
        event.message.clearReactions(UNICODE_STAR).queue()
        deleteStarredMessage(event.guild, event.messageId)
    }

    private fun deleteStarredMessage(guild: Guild, original: String) {
        val collection = getCollection(guild)
        val deleted = collection.findOneAndDelete(Document("original", original))
        val starred = deleted?.get("starred") as String? ?: return
        val data = dataManager[guild]
        val channel = data.optional<String>("channel")?.let {
            guild.getTextChannelById(it)
        } ?: return
        channel.deleteMessageById(starred).queue()
    }

    private fun getLinkObject(
        guild: Guild,
        original: String
    ) = getCollection(guild).find(Document("original", original)).first()

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
                builder.setEmbed(StarboardResponse(original).asEmbed(original.author))
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
