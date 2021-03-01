package net.sourcebot.module.boosters.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.boosters.Boosters

class BoosterListener : EventSubscriber<Boosters> {
    override fun subscribe(
        module: Boosters,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {

    }
}