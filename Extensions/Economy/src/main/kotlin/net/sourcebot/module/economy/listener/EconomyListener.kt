package net.sourcebot.module.economy.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.economy.Economy
import net.sourcebot.module.economy.data.EconomyData
import net.sourcebot.module.profiles.event.ProfileRenderEvent

class EconomyListener : EventSubscriber<Economy> {
    override fun subscribe(
        module: Economy,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) = sourceEvents.listen(module, this::onProfileRender)

    private fun onProfileRender(event: ProfileRenderEvent) {
        val (embed, member) = event
        val economy = EconomyData[member]
        embed.addField(
            "Economy:", """
            **Balance:** ${economy.balance} coins
        """.trimIndent(), false
        )
    }
}