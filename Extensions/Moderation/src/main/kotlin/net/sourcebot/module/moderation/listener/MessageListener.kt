package net.sourcebot.module.moderation.listener

import com.mongodb.client.model.UpdateOptions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Invite
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.Source
import net.sourcebot.api.DurationUtils.parseDuration
import net.sourcebot.api.asMessage
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.formatted
import net.sourcebot.api.response.SourceColor
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.api.truncate
import net.sourcebot.module.moderation.Moderation
import org.bson.Document
import java.time.Instant

class MessageListener : EventSubscriber<Moderation> {
    private val punishmentHandler = Moderation.PUNISHMENT_HANDLER
    private val permissionHandler = Source.PERMISSION_HANDLER
    private val configManager = Source.CONFIG_MANAGER
    private val mongo = Source.MONGODB

    override fun subscribe(
        module: Moderation,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMessageReceive)
        jdaEvents.listen(module, this::onMessageEdit)
        jdaEvents.listen(module, this::onMessageDelete)
        jdaEvents.listen(module, this::handleReportReaction)
    }

    private fun onMessageReceive(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        val message = event.message
        if (Source.COMMAND_HANDLER.isValidCommand(message.contentRaw) == true) return
        val config = configManager[event.guild]
        val mentionThreshold = config.required("moderation.mention-threshold") { 4 }
        val delete = when {
            message.mentionedMembers.size >= mentionThreshold -> handleMentionLimit(event, mentionThreshold)
            message.invites.isNotEmpty() -> handleChatAdvertising(event)
            else -> false
        }
        if (delete) return message.delete().queue() else saveMessage(message)
    }

    private fun handleMentionLimit(event: GuildMessageReceivedEvent, threshold: Int): Boolean {
        val permData = permissionHandler.getData(event.guild)
        val user = permData.getUser(event.member!!)
        if (!permissionHandler.hasPermission(user, "moderation.ignore-mention-threshold", event.channel)) {
            val incident = punishmentHandler.muteIncident(
                event.guild.selfMember,
                event.member!!,
                parseDuration("10m"),
                "Mention Spam Threshold Reached ($threshold Members)"
            )
            event.channel.sendMessage(incident.asMessage(event.jda.selfUser)).queue()
        }
        return false
    }

    private fun handleChatAdvertising(event: GuildMessageReceivedEvent): Boolean {
        val message = event.message
        val invites = message.invites.mapNotNull {
            runCatching { Invite.resolve(event.jda, it, true).complete() }.getOrNull()
        }
        if (invites.isEmpty()) return false
        val permissible = permissionHandler.getData(event.guild).getUser(event.member!!)
        return if (!permissionHandler.hasPermission(
                permissible,
                "moderation.ignore-advertising-check",
                event.channel
            )
        ) {
            val allowed = configManager[event.guild].optional<Array<String>>(
                "moderation.advertising.whitelist"
            ) ?: emptyArray()
            val memberLimit = configManager[event.guild].optional<Long>(
                "moderation.advertising.member-limit"
            ) ?: Long.MAX_VALUE
            val delete = invites.any {
                val guild = it.guild ?: return@any false
                if (guild.id == event.guild.id) return@any false
                if (guild.id in allowed) return@any false
                if (guild.memberCount >= memberLimit) return@any false
                true
            }
            delete && punishmentHandler.logAdvertising(event.guild, message)
        } else false
    }

    private val reportTitle = "^Report #(\\d+)$".toRegex()
    private fun handleReportReaction(event: GuildMessageReactionAddEvent) {
        if (event.channel != punishmentHandler.getReportChannel(event.guild)) return
        if (event.user.isBot) return
        if (event.reactionEmote.isEmote) return
        val valid = when (event.reactionEmote.name) {
            "✅" -> true
            "❌" -> false
            else -> return
        }
        val handledMessage = if (valid) "Handled" else "Marked as Invalid"
        val message = event.retrieveMessage().complete()
        val embed = message.embeds.getOrNull(0) ?: return
        val authorInfo = embed.author ?: return
        val title = authorInfo.name ?: return
        when {
            reportTitle.matches(title) -> {
                val id = reportTitle.matchEntire(title)!!.groupValues[1].toLong()
                punishmentHandler.markReportHandled(event.guild, id, valid, event.userId)
            }
            title == "Potential Advertising" -> {
                if (valid) {
                    val target = Regex("\\*\\*User:\\*\\* .+ \\((\\d+)\\)").find(
                        embed.description!!
                    )!!.groupValues[1].let(event.guild::getMemberById)
                    if (target != null) punishmentHandler.banIncident(
                        event.member, target, 7, "Advertising in Chat"
                    )
                }
            }
            else -> return
        }
        val render = EmbedBuilder(embed)
            .setAuthor("$title - Handled", authorInfo.url, authorInfo.iconUrl)
            .appendDescription(
                """
               
                
                **$handledMessage By:** ${event.user.formatted()} (${event.userId})
            """.trimIndent()
            ).setColor(SourceColor.SUCCESS.color).build()
        return message.editMessage(render).override(true).queue {
            it.clearReactions().queue()
        }
    }

    private fun onMessageEdit(event: GuildMessageUpdateEvent) {
        if (event.author.isBot) return
        val log = messageLogChannel(event.guild) ?: return
        val collection = messageLogCollection(event.guild)
        val author = event.author
        val channel = event.channel
        val embed = StandardWarningResponse(
            "Message Edited", """
                **Author:** ${author.formatted()} (${author.id})
                **Channel:** ${channel.name} (${channel.id})
                **Edited At:** ${Source.DATE_TIME_FORMAT.format(Instant.now())}
                **Jump Link:** [${MarkdownUtil.maskedLink("Click", event.message.jumpUrl)}]
            """.trimIndent()
        )
        collection.find(Document("_id", event.messageId)).first()?.let {
            embed.addField("Old Content:", (it["content"] as String).truncate(1024), false)
        }
        embed.addField("New Content:", event.message.contentRaw.truncate(1024), false)
        saveMessage(event.message)
        log.sendMessage(embed.asMessage(event.author)).queue()
    }

    private fun onMessageDelete(event: GuildMessageDeleteEvent) {
        val log = messageLogChannel(event.guild) ?: return
        val found = messageLogCollection(event.guild).findOneAndDelete(
            Document("_id", event.messageId)
        ) ?: return
        var foundAuthor: Member? = null
        val author = (found["author"] as String).let {
            foundAuthor = event.guild.getMemberById(it) ?: return@let it
            "${foundAuthor!!.formatted()} ($it)"
        }
        if (foundAuthor != null && foundAuthor!!.user.isBot) return
        val content = found["content"] as String
        val channel = (found["channel"] as String).let {
            val channel = event.guild.getTextChannelById(it) ?: return@let it
            "${channel.name} ($it)"
        }
        val sent = (found["time"] as Long)
            .let(Instant::ofEpochMilli)
            .let(Source.DATE_TIME_FORMAT::format)
        val embed = StandardErrorResponse(
            "Message Deleted", """
                **Author:** $author
                **Channel:** $channel
                **Date Sent:** $sent
            """.trimIndent()
        )
        embed.addField("Message:", content.truncate(1024), false)
        log.sendMessage(embed.asMessage(foundAuthor ?: event.guild.selfMember)).queue()
    }

    private fun messageLogCollection(guild: Guild) =
        mongo.getCollection(guild.id, "message-log")

    private fun messageLogChannel(guild: Guild) =
        configManager[guild].optional<String>("moderation.message-log")?.let(
            guild::getTextChannelById
        )

    private fun saveMessage(message: Message) {
        val collection = messageLogCollection(message.guild)
        collection.updateOne(
            Document("_id", message.id),
            Document("\$set", Document().also {
                it["author"] = message.author.id
                it["content"] = message.contentRaw
                it["channel"] = message.channel.id
                it["time"] = message.timeCreated.toInstant().toEpochMilli()
            }),
            UpdateOptions().upsert(true)
        )
    }
}