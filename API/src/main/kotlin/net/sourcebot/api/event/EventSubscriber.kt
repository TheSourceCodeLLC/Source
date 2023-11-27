package net.sourcebot.api.event

import net.dv8tion.jda.api.events.GenericEvent
import net.sourcebot.api.module.SourceModule

fun interface EventSubscriber<M : SourceModule> {
    fun subscribe(
        module: M,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    )
}