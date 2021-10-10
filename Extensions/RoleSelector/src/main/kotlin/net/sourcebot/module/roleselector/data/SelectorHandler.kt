package net.sourcebot.module.roleselector.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent
import net.sourcebot.Source
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.roleselector.RoleSelector
import java.util.concurrent.TimeUnit

class SelectorHandler : EventSubscriber<RoleSelector> {
    private val mongodb = Source.MONGODB

    private val menus = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<Guild, SelectorCache>() {
            override fun load(guild: Guild) = SelectorCache(mongodb.getCollection(guild.id, "role-selectors"))
        })

    operator fun get(guild: Guild): SelectorCache = menus[guild]

    override fun subscribe(
        module: RoleSelector,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onSelectionMenu)
    }

    private fun onSelectionMenu(event: SelectionMenuEvent) {
        val guild = event.guild ?: return
        val menu = event.selectionMenu ?: return
        val name = menu.id ?: return

        if (!name.contains("roleselector")) return
        val values = event.values
        if (values.isEmpty()) return

        val cache = this[guild]
        val selectorName = name.substringAfter(":")
        val selector = cache.getSelector(selectorName) ?: return


        event.reply("It worked oh my god").setEphemeral(true).queue()
    }

}