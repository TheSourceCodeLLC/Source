package net.sourcebot.module.economy.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.round
import net.sourcebot.module.boosters.Boosters
import net.sourcebot.module.economy.Economy
import net.sourcebot.module.economy.events.CoinChangeEvent

class BoosterListener : EventSubscriber<Economy> {
    override fun subscribe(
        module: Economy,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        sourceEvents.listen(module, this::applyCoinBooster)
    }

    private fun applyCoinBooster(event: CoinChangeEvent) {
        val (member, delta) = event
        if (delta < 0) return
        val (multiplier) = Boosters[member, "economy"]
        if (multiplier <= 1.0) return
        val additional = (delta * multiplier) - delta
        event.changelog +=
            (additional.toLong() to "${multiplier.round()}x Booster")
    }
}