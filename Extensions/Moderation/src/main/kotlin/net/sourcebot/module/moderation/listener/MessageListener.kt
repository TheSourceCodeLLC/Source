package net.sourcebot.module.moderation.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.sourcebot.api.DurationUtils.parseDuration
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.moderation.Moderation
import net.sourcebot.module.moderation.PunishmentHandler

class MessageListener(
    private val configManager: ConfigurationManager,
    private val punishmentHandler: PunishmentHandler,
    private val isCommand: (String) -> Boolean,
    private val mongo: MongoDB
) : EventSubscriber<Moderation> {
    override fun subscribe(
        module: Moderation,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMessageReceive)
        jdaEvents.listen(module, this::onMessageEdit)
        jdaEvents.listen(module, this::onMessageDelete)
    }

    private fun onMessageReceive(event: GuildMessageReceivedEvent) {
        if (isCommand(event.message.contentRaw)) return
        val config = configManager[event.guild]
        val mentionThreshold = config.required("moderation.mention-threshold") { 4 }
        if (event.message.mentionedMembers.size >= mentionThreshold) return handleMentionLimit(event, mentionThreshold)

    }

    private fun handleMentionLimit(event: GuildMessageReceivedEvent, threshold: Int) {
        val incident = punishmentHandler.muteIncident(
            event.guild.selfMember,
            event.member!!,
            parseDuration("10m"),
            "Mention Spam Threshold Reached ($threshold Members)"
        )
        event.channel.sendMessage(incident.asMessage(event.jda.selfUser)).queue()
    }

    private fun onMessageEdit(event: GuildMessageUpdateEvent) {

    }

    private fun onMessageDelete(event: GuildMessageDeleteEvent) {

    }
}