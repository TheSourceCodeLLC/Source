package net.sourcebot.module.roleselector.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
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

    private val selectionCache = UserSelectionCache()

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
        val roleIdsToAdd = event.values

        if (selector.hasPermission()) {
            val member = guild.getMember(user) ?: return
            val hasPermission = Source.PERMISSION_HANDLER.memberHasPermission(member, selector.permission, null)
            if (!hasPermission) {
                event.reply("You do not have permission to use this selector menu!").setEphemeral(true).queue()
                return
            }
        }

        cache.verifyRoles(guild, selector)
        val addIsEmpty = roleIdsToAdd.isEmpty()

        val selectorData = selectionCache[selector] ?: mutableListOf()
        val userSelectionData = selectorData.find { it.id == user.id }

        if (userSelectionData == null && addIsEmpty) {
            event.reply("Please try again!").setEphemeral(true).queue()
            return
        }

        val rolesIdsToRemove = userSelectionData?.selections?.toMutableList() ?: mutableListOf()
        rolesIdsToRemove.removeIf { roleIdsToAdd.contains(it) }

        if (addIsEmpty) selectionCache.removeSelectionData(selector, userSelectionData!!)
        else selectionCache.addSelectionData(selector, SelectionData(user.id, roleIdsToAdd))

        val removeRoles = optionIdsToRole(guild, rolesIdsToRemove)
        val addRoles = optionIdsToRole(guild, roleIdsToAdd)

        handleRoles(guild, user, addRoles, removeRoles)

        event.reply("Successfully updated your roles!").setEphemeral(true).queue()
    }

    private fun optionIdsToRole(guild: Guild, roles: MutableList<String>) =
        roles.map { it.substringAfter(":") }.mapNotNull { guild.getRoleById(it) }

    private fun handleRoles(guild: Guild, user: User, rolesToAdd: List<Role>, rolesToRemove: List<Role>) {
        val member = guild.getMember(user) ?: return
        val memberRoles = member.roles.toMutableList()
        memberRoles.removeIf { rolesToRemove.contains(it) }
        memberRoles.addAll(rolesToAdd)

        guild.modifyMemberRoles(member, memberRoles.distinct()).queue()
    }

    private class UserSelectionCache {

        private val cache = CacheBuilder.newBuilder().softValues()
            .build(object : CacheLoader<String, MutableList<SelectionData>>() {
                override fun load(key: String): MutableList<SelectionData> = mutableListOf()
            })

        operator fun get(selector: Selector): MutableList<SelectionData>? = try {
            cache[selector.name]
        } catch (ex: Exception) {
            null
        }

        fun addSelectionData(selector: Selector, selectionData: SelectionData) {
            val currentData = get(selector) ?: mutableListOf()

            currentData.removeIf { it.id == selectionData.id }
            currentData.add(selectionData)

            updateSelectionData(selector, currentData)
        }

        fun removeSelectionData(selector: Selector, selectionData: SelectionData) {
            val currentData = get(selector) ?: mutableListOf()
            currentData.removeIf { it.id == selectionData.id }

            updateSelectionData(selector, currentData)
        }

        private fun updateSelectionData(selector: Selector, selectionData: MutableList<SelectionData>) {
            cache.invalidate(selector.name)
            cache.put(selector.name, selectionData)
        }

    }

    private data class SelectionData(
        val id: String,
        var selections: MutableList<String>
    )

}