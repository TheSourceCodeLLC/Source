package net.sourcebot.api.permission

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.error.InvalidChannelAlert
import net.sourcebot.api.alert.error.NoPermissionAlert
import net.sourcebot.api.alert.error.NoPermissionDMAllowedAlert
import net.sourcebot.api.database.MongoDB

class PermissionHandler(private val mongodb: MongoDB) {
    private val dataCache = HashMap<String, PermissionData>()

    private fun computeContext(channel: MessageChannel): Set<String> {
        val context = mutableSetOf<String>()
        return if (channel !is TextChannel) context
        else context.apply {
            add(channel.id)
            if (channel.parent != null) add(channel.parent!!.id)
        }
    }

    private fun hasPermission(permissible: Permissible, node: String, context: Set<String>): Boolean =
        if (context.any { permissible.hasPermission(node, it) }) true
        else permissible.hasPermission(node)

    fun hasPermission(permissible: Permissible, node: String, channel: MessageChannel): Boolean {
        val effective = getEffectiveNodes(node)
        val contexts = computeContext(channel)
        return effective.any { hasPermission(permissible, it, contexts) }
    }

    private fun getEffectiveNodes(permission: String): Set<String> =
        mutableSetOf(permission).apply {
            addAll(permission.mapIndexed { idx, c ->
                if (c == '.') permission.substring(0..idx) + "*" else null
            }.filterNotNull().toMutableSet()).apply { add("*") }
        }

    fun getAllowedContexts(permissible: Permissible, node: String): Set<String> =
        getEffectiveNodes(node).flatMap { permissible.getContexts(it) }.toSet()

    fun getPermissionAlert(
        guildOnly: Boolean, jda: JDA,
        permissible: Permissible, node: String
    ): Alert {
        val availableIn = getAllowedContexts(permissible, node)
        return if (availableIn.isEmpty()) {
            if (guildOnly) NoPermissionAlert()
            else NoPermissionDMAllowedAlert()
        } else InvalidChannelAlert(jda, availableIn)
    }


    fun getData(guild: String): PermissionData = dataCache.computeIfAbsent(guild) {
        val database = mongodb.getDatabase(it)
        PermissionData(database, it)
    }

    fun getData(guild: Guild) = getData(guild.id)
}