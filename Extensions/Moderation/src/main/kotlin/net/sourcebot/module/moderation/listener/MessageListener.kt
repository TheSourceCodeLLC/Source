package net.sourcebot.module.moderation.listener

import com.google.common.cache.CacheBuilder
import com.mongodb.client.model.UpdateOptions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.Source
import net.sourcebot.api.*
import net.sourcebot.api.DurationUtils.parseDuration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.SourceColor
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.module.moderation.Moderation
import net.sourcebot.module.moderation.data.RoleUpdateIncident
import net.sourcebot.module.moderation.event.MessageDeleteEvent
import net.sourcebot.module.moderation.event.MessageEditEvent
import org.bson.Document
import java.time.Instant
import java.util.concurrent.TimeUnit

class MessageListener : EventSubscriber<Moderation> {
    private val permissionHandler = Source.PERMISSION_HANDLER
    private val configManager = Source.CONFIG_MANAGER
    private val mongo = Source.MONGODB

    private val recentBans = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build<String, String>()

    override fun subscribe(
        module: Moderation,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        listOf(
            this::onMemberBanned,
            this::onMemberKicked,
            this::onMemberUnbanned,
            this::onMemberRoleAdded,
            this::onMemberRoleRemoved,
            this::onMessageReceive,
            this::submitMessageEdit,
            this::submitMessageDelete,
            this::handleReportReaction
        ).forEach { jdaEvents.listen(module, it) }
        sourceEvents.listen(module, this::onMessageDelete)
        sourceEvents.listen(module, this::onMessageEdit)
    }

    private fun onMemberBanned(event: GuildBanEvent) {
        val id = event.user.id
        recentBans.put(id, id)
        event.guild.retrieveAuditLogs().type(ActionType.BAN).queue {
            val entry = it.find { entry -> entry.targetId == id } ?: return@queue
            val sender = event.guild.getMember(event.user)!!
            val target = event.user
            val reason = entry.reason
            Moderation.getPunishmentHandler(event.guild) {
                val incident = reason.ifPresentOrElse({ reason ->
                    manualBanIncident(sender, target, 0, reason)
                }, {
                    manualBanIncident(sender, target, 0)
                })
                incidentChannel()?.let { logIncident(it, incident) }
            }
        }
    }

    private fun onMemberKicked(event: GuildMemberRemoveEvent) {
        val (sender, target) = extractAuditInfo(event.guild, ActionType.KICK, event.user) ?: return
        Moderation.getPunishmentHandler(event.guild) {
            val incident = manualKickIncident(sender, target)
            incidentChannel()?.let { logIncident(it, incident) }
        }
    }

    private fun onMemberUnbanned(event: GuildUnbanEvent) {
        val (sender, target) = extractAuditInfo(event.guild, ActionType.UNBAN, event.user) ?: return
        Moderation.getPunishmentHandler(event.guild) {
            val incident = manualUnbanIncident(sender, target)
            incidentChannel()?.let { logIncident(it, incident) }
        }
    }

    private fun onMemberRoleAdded(event: GuildMemberRoleAddEvent) =
        onMemberRoleChange(event.guild, event.user, event.roles.first(), RoleUpdateIncident.Action.REMOVE)

    private fun onMemberRoleRemoved(event: GuildMemberRoleRemoveEvent) =
        onMemberRoleChange(event.guild, event.user, event.roles.first(), RoleUpdateIncident.Action.ADD)

    private fun onMemberRoleChange(guild: Guild, user: User, role: Role, action: RoleUpdateIncident.Action) {
        val (sender, target) = extractAuditInfo(guild, ActionType.MEMBER_ROLE_UPDATE, user) ?: return
        Moderation.getPunishmentHandler(guild) {
            val incident = manualRoleUpdate(sender, guild.getMember(target)!!, role, action)
            incidentChannel()?.let { logIncident(it, incident) }
        }
    }

    private fun extractAuditInfo(guild: Guild, type: ActionType, user: User): Pair<Member, User>? {
        val logs = guild.retrieveAuditLogs().type(type).complete()
        val entry = logs.find { entry -> entry.targetId == user.id } ?: return null
        if (entry.timeCreated.isBefore(entry.timeCreated.minusSeconds(30))) return null
        val sender = guild.getMember(user)!!
        return sender to user
    }

    private fun onMessageDelete(event: MessageDeleteEvent) {
        if (isIgnored(event.channel)) return
        val (guild, authorId, content, channelId, sent) = event
        if (recentBans.asMap().containsKey(authorId)) return
        val author = event.author
        val channel = event.channel
        messageLogChannel(guild)?.let { log ->
            if (author != null && author.user.isBot) return@let
            val embed = StandardErrorResponse(
                "Message Deleted", """
                **Author:** ${author?.let { "${it.formatLong()} (${it.id})" } ?: authorId}
                **Channel:** ${channel?.let { "${it.name} (${it.id})" } ?: channelId}
                **Date Sent:** ${Source.DATE_TIME_FORMAT.format(sent)}
            """.trimIndent()
            )
            embed.addField("Message:", content.truncate(1024), false)
            log.sendMessage(embed.asMessage(author ?: event.guild.selfMember)).queue()
        }
        @Suppress("NestedLambdaShadowedImplicitParameter")
        channel?.let {
            val mentionedUsers = Message.MentionType.USER.listMatches(content, guild::getMemberById).filterNot {
                it.user.isBot
            }
            val mentionedRoles = Message.MentionType.ROLE.listMatches(content, guild::getRoleById)
            if (mentionedUsers.isEmpty() && mentionedRoles.isEmpty()) return
            it.sendMessage(
                StandardErrorResponse(
                    "Ghost Ping!", """
                        **User:** ${author?.let { "${it.formatLong()} (${it.id})" } ?: authorId}
                    """.trimIndent()
                ).also {
                    it.addField("Message:", content.truncate(1024), false)
                }.asMessage(author ?: event.guild.selfMember)
            ).queue()
        }
    }

    private fun onMessageEdit(event: MessageEditEvent) {
        if (isIgnored(event.channel)) return
        val (guild, author, channel, newContent, oldContent) = event
        if (recentBans.asMap().containsKey(author.id)) return
        val parent = channel.parent
        val blacklist = messageLogBlacklist(event.guild)
        if (channel !in blacklist && !(parent != null && parent in blacklist)) {
            messageLogChannel(guild)?.let { log ->
                val embed = StandardWarningResponse(
                    "Message Edited", """
                        **Author:** ${author.formatLong()} (${author.id})
                        **Channel:** ${channel.name} (${channel.id})
                        **Edited At:** ${Source.DATE_TIME_FORMAT.format(Instant.now())}
                        **Jump Link:** [${MarkdownUtil.maskedLink("Click", event.message.jumpUrl)}]
                    """.trimIndent()
                )
                if (oldContent != null)
                    embed.addField("Old Content:", oldContent.truncate(1024), false)
                embed.addField("New Content:", newContent.truncate(1024), false)
                saveMessage(event.message)
                log.sendMessage(embed.asMessage(event.author)).queue()
            }
        }
        @Suppress("NAME_SHADOWING")
        oldContent?.let { oldContent: String ->
            val oldUsers = Message.MentionType.USER.listMatches(oldContent, guild::getMemberById).filterNot {
                it.user.isBot
            }
            val oldRoles = Message.MentionType.ROLE.listMatches(oldContent, guild::getRoleById)
            val newUsers = Message.MentionType.USER.listMatches(newContent, guild::getMemberById).filterNot {
                it.user.isBot
            }
            val newRoles = Message.MentionType.ROLE.listMatches(newContent, guild::getRoleById)
            if (newUsers.containsAll(oldUsers) && newRoles.containsAll(oldRoles)) return@let
            channel.sendMessage(
                StandardErrorResponse(
                    "Ghost Ping!", """
                            **User:** ${author.let { "${it.formatLong()} (${it.id})" }}
                        """.trimIndent()
                ).also {
                    it.addField("Message:", oldContent.truncate(1024), false)
                }.asMessage(author)
            ).queue()
        }
    }

    private fun onMessageReceive(event: GuildMessageReceivedEvent) {
        if (isIgnored(event.channel)) return
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
        val channel = event.channel
        val parent = channel.parent
        if (delete) message.delete().queue() else {
            val blacklist = messageLogBlacklist(event.guild)
            if (channel in blacklist || (parent != null && parent in blacklist)) return
            saveMessage(message)
        }
    }

    private fun handleMentionLimit(event: GuildMessageReceivedEvent, threshold: Int): Boolean {
        val permData = permissionHandler.getData(event.guild)
        val user = permData.getUser(event.member!!)
        if (!permissionHandler.hasPermission(user, "moderation.ignore-mention-threshold", event.channel)) {
            val incident = Moderation.getPunishmentHandler(event.guild) {
                muteIncident(
                    event.guild.selfMember,
                    event.member!!,
                    parseDuration("10m"),
                    "Mention Spam Threshold Reached ($threshold Members)"
                )
            }
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
            delete && Moderation.getPunishmentHandler(event.guild) { logAdvertising(message) }
        } else false
    }

    private val reportTitle = "^Report #(\\d+)$".toRegex()
    private fun handleReportReaction(event: GuildMessageReactionAddEvent) {
        val punishmentHandler = Moderation.getPunishmentHandler(event.guild)
        if (event.channel != punishmentHandler.reportChannel()) return
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
                punishmentHandler.markReportHandled(id, valid, event.userId)
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
               
                
                **$handledMessage By:** ${event.user.formatLong()} (${event.userId})
            """.trimIndent()
            ).setColor(SourceColor.SUCCESS.color).build()
        return message.editMessageEmbeds(render).override(true).queue {
            it.clearReactions().queue()
        }
    }

    private fun submitMessageEdit(event: GuildMessageUpdateEvent) {
        if (event.author.isBot) return
        val collection = messageLogCollection(event.guild)
        val author = event.member!!
        val channel = event.channel
        val newContent = event.message.contentRaw
        val oldContent = collection.find(Document("_id", event.messageId)).first()?.get("content") as? String
        Source.SOURCE_EVENTS.fireEvent(
            MessageEditEvent(
                event.guild, author, channel, newContent, oldContent, event.message
            )
        )
    }

    private fun submitMessageDelete(event: GuildMessageDeleteEvent) {
        val found = messageLogCollection(event.guild).findOneAndDelete(
            Document("_id", event.messageId)
        ) ?: return
        Source.SOURCE_EVENTS.fireEvent(
            MessageDeleteEvent(
                event.guild,
                found["author"] as String,
                found["content"] as String,
                found["channel"] as String,
                (found["time"] as Long).let(Instant::ofEpochMilli)
            )
        )
    }

    private fun messageLogCollection(guild: Guild) =
        mongo.getCollection(guild.id, "message-log")

    private fun messageLogChannel(guild: Guild) =
        configManager[guild].optional<String>("moderation.message-log.channel")?.let(
            guild::getTextChannelById
        )

    private fun messageLogBlacklist(guild: Guild) =
        configManager[guild].optional<List<String>>("moderation.message-log.blacklist")?.mapNotNull {
            guild.getGuildChannelById(it) ?: return@mapNotNull null
        } ?: emptyList()

    private fun saveMessage(message: Message) = messageLogCollection(message.guild).let { collection ->
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

    private fun isIgnored(channel: TextChannel?): Boolean {
        if (channel == null) return false
        return configManager[channel.guild].optional<List<String>>(
            "moderation.excluded-channels"
        )?.contains(channel.id) ?: false
    }
}