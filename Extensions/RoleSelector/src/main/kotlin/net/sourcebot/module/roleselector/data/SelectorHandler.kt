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

        val cache = this[guild]
        val selectorName = name.substringAfter(":")
        val selector = cache[selectorName] ?: return
        val user = event.user
        val roleOptionsToAdd = event.values

        if (selector.hasPermission()) {
            val member = guild.getMember(user) ?: return
            val hasPermission = Source.PERMISSION_HANDLER.memberHasPermission(member, selector.permission, null)
            if (!hasPermission) {
                event.reply("You do not have permission to use this selector menu!").setEphemeral(true).queue()
                return
            }
        }

        cache.verifyRoles(guild, selector)

        val roleOptionsToRemove = selector.roleIds.toMutableList()
        roleOptionsToRemove.removeIf { roleOptionsToAdd.contains(it) }

        val removeRoleIds = roleOptionsToRemove.map { it.substringAfter(":") }
        val addRoles = roleOptionsToAdd.mapNotNull { guild.getRoleById(it.substringAfter(":")) }

        val member = guild.getMember(user) ?: return
        val memberRoles = member.roles.toMutableList()

        memberRoles.removeIf { removeRoleIds.contains(it.id) }
        memberRoles.addAll(addRoles)

        guild.modifyMemberRoles(member, memberRoles.distinct()).queue()

        event.reply("Successfully updated your roles!").setEphemeral(true).queue()
    }

}