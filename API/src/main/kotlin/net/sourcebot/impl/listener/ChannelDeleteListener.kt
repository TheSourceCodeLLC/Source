package net.sourcebot.impl.listener

import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.channel.category.CategoryDeleteEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.sourcebot.Source
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.impl.BaseModule

class ChannelDeleteListener : EventSubscriber<BaseModule> {
    override fun subscribe(
        module: BaseModule,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen<TextChannelDeleteEvent>(module) { clearContexts(it.channel) }
        jdaEvents.listen<CategoryDeleteEvent>(module) { clearContexts(it.category) }
    }

    private fun clearContexts(channel: GuildChannel) {
        Source.PERMISSION_HANDLER.getData(channel.guild).dropContexts(channel.id)
    }
}