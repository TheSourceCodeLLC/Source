package net.sourcebot.module.boosters.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.FormatStrategy
import net.sourcebot.api.capital
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.boosters.Boosters
import net.sourcebot.module.profiles.event.ProfileRenderEvent
import java.time.Instant

class BoosterListener : EventSubscriber<Boosters> {
    override fun subscribe(
        module: Boosters,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        sourceEvents.listen(module, this::onProfileRender)
    }

    private fun onProfileRender(event: ProfileRenderEvent) {
        val (embed, member) = event
        val boosters = Boosters.getAll(member).filter {
            it.value.expiry != null
        }
        if (boosters.isEmpty()) return
        val listing = boosters.entries.joinToString("\n") { (key, booster) ->
            val (multi, expiry) = booster
            val duration = DurationUtils.formatMillis(
                expiry!!.toEpochMilli() - Instant.now().toEpochMilli(),
                FormatStrategy.SHORT
            )
            "**${key.capital()}**: ${multi}x ($duration)"
        }
        embed.addField("Boosters:", listing, false)
    }
}