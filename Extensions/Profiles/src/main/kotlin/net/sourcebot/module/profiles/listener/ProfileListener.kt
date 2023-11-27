package net.sourcebot.module.profiles.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.profiles.Profiles
import java.time.Instant
import java.time.temporal.ChronoUnit

class ProfileListener : EventSubscriber<Profiles> {
    override fun subscribe(
        module: Profiles,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMemberQuit)
        jdaEvents.listen(module, this::onMemberJoin)
    }

    private fun onMemberQuit(event: GuildMemberRemoveEvent) {
        val profile = Profiles[event.guild, event.user.id]
        profile["expiry"] = Instant.now().plus(7, ChronoUnit.DAYS)
    }

    private fun onMemberJoin(event: GuildMemberJoinEvent) {
        val profile = Profiles[event.member]
        profile["expiry"] = null
    }
}