package net.sourcebot.api.event

import net.dv8tion.jda.api.events.GenericEvent

fun interface EventSubscriber {
    fun subscribe(
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    )
}