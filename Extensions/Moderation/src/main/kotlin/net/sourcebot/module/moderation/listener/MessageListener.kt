package net.sourcebot.module.moderation.listener

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Invite
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.sourcebot.Source
import net.sourcebot.api.DurationUtils.parseDuration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.formatted
import net.sourcebot.api.response.SourceColor
import net.sourcebot.module.moderation.Moderation

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
        if (Source.COMMAND_HANDLER.isValidCommand(event.message.contentRaw) == true) return
        val config = configManager[event.guild]
        val mentionThreshold = config.required("moderation.mention-threshold") { 4 }
        if (event.message.mentionedMembers.size >= mentionThreshold) return handleMentionLimit(event, mentionThreshold)
        if (event.message.invites.isNotEmpty()) return handleChatAdvertising(event)
    }

    private fun handleMentionLimit(event: GuildMessageReceivedEvent, threshold: Int) {
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
    }

    private fun handleChatAdvertising(event: GuildMessageReceivedEvent) {
        val message = event.message
        val invites = message.invites.mapNotNull {
            runCatching { Invite.resolve(event.jda, it, true).complete() }.getOrNull()
        }
        if (invites.isEmpty()) return
        val permissible = permissionHandler.getData(event.guild).getUser(event.member!!)
        if (!permissionHandler.hasPermission(permissible, "moderation.ignore-advertising-check", event.channel)) {
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
            if (delete && punishmentHandler.logAdvertising(event.guild, message)) message.delete().queue()
        }
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
        val title = embed.author?.name ?: return
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
        }
        val render = EmbedBuilder(embed).appendDescription(
            """
               
                
                **$handledMessage By:** ${event.user.formatted()} (${event.userId})
            """.trimIndent()
        ).setColor(SourceColor.SUCCESS.color).build()
        return message.editMessage(render).override(true).queue {
            it.clearReactions().queue()
        }
    }

    private fun onMessageEdit(event: GuildMessageUpdateEvent) {

    }

    private fun onMessageDelete(event: GuildMessageDeleteEvent) {

    }
}