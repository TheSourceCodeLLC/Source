package net.sourcebot.api.permission

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.error.InvalidChannelResponse
import net.sourcebot.api.response.error.NoPermissionDMAllowedResponse
import net.sourcebot.api.response.error.NoPermissionResponse
import org.bson.Document

class PermissionHandler(
    private val mongodb: MongoDB,
    private val globalAdmins: Set<String>
) {
    private val dataCache = HashMap<String, PermissionData>()

    private fun computeContext(channel: MessageChannel): Set<String> {
        val context = mutableSetOf<String>()
        return if (channel !is TextChannel) context
        else context.apply {
            add(channel.id)
            if (channel.parent != null) add(channel.parent!!.id)
        }
    }

    private fun hasPermission(
        permissible: Permissible,
        node: String,
        context: Set<String> = emptySet()
    ): Boolean {
        if (permissible is SourceUser && hasGlobalAccess(permissible.id)) return true
        getEffectiveNodes(node).forEach { eff ->
            when (permissible.hasPermission(eff)) {
                true -> return true
                false -> return false
            }
            for (ctx in context) return permissible.hasPermission(eff, ctx) ?: continue
        }
        return false
    }

    private fun hasGlobalAccess(
        id: String
    ): Boolean = id in globalAdmins

    fun hasGlobalAccess(
        user: User
    ): Boolean = hasGlobalAccess(user.id)

    fun hasPermission(
        permissible: Permissible,
        node: String,
        channel: MessageChannel
    ): Boolean = hasPermission(permissible, node, computeContext(channel))

    fun checkPermission(
        permissible: Permissible,
        node: String,
        channel: MessageChannel,
        ifPresent: () -> Response
    ): Response =
        if (hasPermission(permissible, node, channel)) ifPresent()
        else getPermissionAlert(true, channel.jda, permissible, node)

    private fun getEffectiveNodes(permission: String): Set<String> =
        mutableSetOf(permission).apply {
            addAll(permission.mapIndexed { idx, c ->
                if (c == '.') permission.substring(0..idx) + "*" else null
            }.filterNotNull().toMutableSet()).apply { add("*") }
        }

    private fun getAllowedContexts(
        permissible: Permissible,
        node: String
    ): Set<String> {
        val allowed = mutableSetOf<String>()
        getEffectiveNodes(node).forEach {
            if (permissible.hasPermission(it) == false) return allowed
            allowed.addAll(permissible.getContexts(it))
        }
        return allowed
    }

    fun getPermissionAlert(
        guildOnly: Boolean, jda: JDA,
        permissible: Permissible, node: String
    ): Response {
        val availableIn = getAllowedContexts(permissible, node)
        return if (availableIn.isEmpty()) {
            if (guildOnly) NoPermissionResponse()
            else NoPermissionDMAllowedResponse()
        } else InvalidChannelResponse(jda, availableIn)
    }


    private fun getData(guild: String): PermissionData = dataCache.computeIfAbsent(guild) {
        PermissionData(mongodb.getDatabase(it))
    }

    fun getData(guild: Guild) = getData(guild.id)

    internal fun getPermissions(
        document: Document
    ): MutableList<SourcePermission> = document.getList("permissions", Document::class.java)
        .map { MongoSerial.fromDocument<SourcePermission>(it) }
        .toMutableList()
}