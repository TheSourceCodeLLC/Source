package net.sourcebot.module.experience.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.experience.Experience
import net.sourcebot.module.profiles.event.ProfileRenderEvent

class ExperienceListener : EventSubscriber<Experience> {
    override fun subscribe(
        module: Experience,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        sourceEvents.listen(module, this::onProfileRender)
    }

    private fun onProfileRender(event: ProfileRenderEvent) {
        val (embed, member) = event
        val experience = Experience[member]
        embed.addField("Experience", """
            ${"**Amount:** ${experience.amount}"}
            ${experience.booster?.let { "**Booster:** ${it.multiplier}x" } ?: ""}
        """.trimIndent(), false)
    }
}