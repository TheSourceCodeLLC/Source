package net.sourcebot.impl.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.formatted
import net.sourcebot.api.response.SimpleErrorResponse
import net.sourcebot.api.response.SimpleInfoResponse
import net.sourcebot.impl.BaseModule

class ConnectionListener(
    private val configurationManager: ConfigurationManager
) : EventSubscriber<BaseModule> {
    override fun subscribe(
        module: BaseModule,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMemberJoin)
        jdaEvents.listen(module, this::onMemberLeave)
    }

    private fun onMemberJoin(event: GuildMemberJoinEvent) {
        val connectionConfig = getConnectionConfig(event) ?: return
        val channel = connectionConfig.optional<String>("channel")?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        val joinMessages = connectionConfig.optional<List<String>>("joinMessages") ?: return
        val toSend = joinMessages.random()
        val joiner = event.user
        val message = SimpleInfoResponse().setDescription(
            toSend.format("**${joiner.formatted()}**")
        )
        channel.sendMessage(message.build()).queue()
    }

    private fun onMemberLeave(event: GuildMemberRemoveEvent) {
        val connectionConfig = getConnectionConfig(event) ?: return
        val channel = connectionConfig.optional<String>("channel")?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        val leaveMessages = connectionConfig.optional<List<String>>("leaveMessages") ?: return
        val toSend = leaveMessages.random()
        val leaver = event.user
        val message = SimpleErrorResponse().setDescription(
            toSend.format("**${leaver.formatted()}**")
        )
        channel.sendMessage(message.build()).queue()
    }

    private fun getConnectionConfig(
        event: GenericGuildEvent
    ): JsonConfiguration? = configurationManager[event.guild].optional("source.connections")
}